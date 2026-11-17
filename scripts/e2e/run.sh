#!/usr/bin/env sh
# Harness E2E genérico para o app Android, executado por adb no aparelho ou emulador.
#
# Seleção do alvo (nesta ordem):
#   1. ADB_DEVICE do .env (valor não é exibido);
#   2. o único dispositivo conectado por USB ou Wi-Fi;
#   3. emulador em execução (serial emulator-*). Emulador parado não é alvo.
# Sem alvo, sai com código 2 e marca tudo como NOT TESTED.
#
# Cada caso declara o requisito de arquitetura. Casos com `arm64` são
# NOT TESTED em alvo que não seja arm64 (sessão PRoot exige arm64).
# Casos pendentes (`pending <caso> <ticket>`) são entregues pelo ticket
# indicado: aparecem como NOT TESTED até esse ticket ativá-los.
#
# Uso: sh scripts/e2e/run.sh [caso ...]

set -u

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PKG="com.jarves.mh"
ACTIVITY="$PKG/.MainActivity"
ADB="${ADB:-adb}"

results=""
fail_count=0
pass_count=0
nt_count=0

record() {
  # record <PASS|FAIL|NOT TESTED> <caso> <evidência curta>
  results="${results}$1 $2 $3
"
  case "$1" in
    PASS) pass_count=$((pass_count + 1)) ;;
    FAIL) fail_count=$((fail_count + 1)) ;;
    *) nt_count=$((nt_count + 1)) ;;
  esac
}

resolve_target() {
  connected=$($ADB devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1}')
  if [ -f "$ROOT/.env" ]; then
    configured=$(sed -n 's/^ADB_DEVICE=\(.*\)$/\1/p' "$ROOT/.env" | tail -n 1)
    if [ -n "$configured" ]; then
      # Usa o ADB_DEVICE só se ele estiver conectado; senão cai no fallback.
      # O valor não é exibido.
      if printf '%s\n' "$connected" | grep -qxF "$configured"; then
        TARGET="$configured"; TARGET_LABEL="ADB_DEVICE do .env"
        return 0
      fi
      echo "E2E: ADB_DEVICE do .env não está conectado; usando fallback." >&2
    fi
  fi
  count=$(printf '%s\n' "$connected" | grep -c . || true)
  if [ "$count" = "1" ]; then
    TARGET="$connected"; TARGET_LABEL="dispositivo único conectado"
    return 0
  fi
  # Emulador só conta quando está em execução (serial emulator-*).
  emu=$(printf '%s\n' "$connected" | grep '^emulator-' | head -n 1 || true)
  if [ -n "$emu" ]; then
    TARGET="$emu"; TARGET_LABEL="emulador em execução"
    return 0
  fi
  if [ "$count" = "0" ] && command -v emulator >/dev/null 2>&1 \
     && [ -n "$(emulator -list-avds 2>/dev/null | head -n 1)" ]; then
    NO_TARGET_REASON="emulador configurado, mas não está em execução"
  fi
  return 1
}

if ! resolve_target; then
  echo "E2E: nenhum alvo (${NO_TARGET_REASON:-ADB_DEVICE ausente, nenhum aparelho ou emulador})."
  echo "NOT TESTED all: sem alvo"
  exit 2
fi

adb_t() { $ADB -s "$TARGET" "$@"; }

abi=$(adb_t shell getprop ro.product.cpu.abi 2>/dev/null | tr -d '\r')
is_arm64=0
case "$abi" in arm64-v8a) is_arm64=1 ;; esac

case_launch() {
  adb_t shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
  adb_t shell am start -n "$ACTIVITY" >/dev/null 2>&1
  sleep 2
  if adb_t shell dumpsys window | grep -q "mCurrentFocus=.*$PKG"; then
    record PASS launch "MainActivity em foco"
  else
    record FAIL launch "MainActivity não ficou em foco"
  fi
}

case_arm64_only() {
  if [ "$is_arm64" = "1" ]; then
    record PASS arm64-only "alvo arm64"
  else
    record "NOT TESTED" arm64-only "requer arm64 (alvo: $abi)"
  fi
}

pending() {
  record "NOT TESTED" "$1" "pendente: entregue pelo ticket ${2}"
}

# Pré-condição dos casos do #5: projeto "$E2E_PROJECT" criado pela UI com pasta
# externa "/storage/emulated/0/$E2E_DIR" (cenário 1 do épico). Os casos não criam
# o projeto; se ele não existir na lista, o caso sai como NOT TESTED.
E2E_PROJECT="mhe2e"
E2E_DIR="MH-E2E-Projeto"

# Lê a árvore de acessibilidade do aparelho (uiautomator) para a tela atual.
ui_dump() {
  adb_t shell uiautomator dump /sdcard/mh-e2e-ui.xml >/dev/null 2>&1
  adb_t shell cat /sdcard/mh-e2e-ui.xml 2>/dev/null
  adb_t shell rm /sdcard/mh-e2e-ui.xml >/dev/null 2>&1
}

ui_has() {
  ui_dump | grep -q "$1"
}


# Toca o centro do primeiro nó com o texto dado. Falha se não existir.
ui_tap_text() {
  xy=$(ui_dump | grep -o "<node[^>]*\(text\|content-desc\)=\"$1\"[^>]*>" | head -n 1 | grep -o 'bounds="[^"]*"' | grep -o '[0-9][0-9]*')
  set -- $xy
  [ -n "${1:-}" ] || return 1
  adb_t shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
}

restart_app() {
  adb_t shell am force-stop "$PKG" >/dev/null 2>&1
  adb_t shell am start -n "$ACTIVITY" >/dev/null 2>&1
  sleep 12
}

# Cenário 1 do épico: o app grava na pasta externa do projeto, e o arquivo
# aparece no caminho real visto pelo adb.
case_project_folder_session() {
  if [ "$is_arm64" != "1" ]; then
    record "NOT TESTED" project-folder-session "requer arm64 (alvo: $abi)"
    return
  fi
  if ! adb_t shell dumpsys package "$PKG" | grep -q "WRITE_EXTERNAL_STORAGE: granted=true"; then
    record "NOT TESTED" project-folder-session "permissão de armazenamento não concedida"
    return
  fi
  restart_app
  if ! ui_tap_text "$E2E_PROJECT"; then
    record "NOT TESTED" project-folder-session "projeto $E2E_PROJECT não encontrado"
    return
  fi
  sleep 2
  name="e2e-$(date +%H%M%S).txt"
  if ! ui_tap_text "Files" || ! ui_tap_text "New" || ! ui_tap_text "New file"; then
    record FAIL project-folder-session "menu de novo arquivo não encontrado"
    return
  fi
  sleep 1
  ui_tap_text "Name" >/dev/null 2>&1
  adb_t shell input text "$name"
  ui_tap_text "Create" >/dev/null 2>&1
  sleep 3
  if adb_t shell test -f "/storage/emulated/0/$E2E_DIR/$name"; then
    record PASS project-folder-session "arquivo do app visível em /storage/emulated/0/$E2E_DIR"
  else
    record FAIL project-folder-session "arquivo não apareceu na pasta externa"
  fi
}

# Cenário 5 do épico: permissão revogada, projeto externo abre com aviso.
# Restaura a permissão ao final, para não deixar o aparelho alterado.
case_project_folder_revoke() {
  adb_t shell pm revoke "$PKG" android.permission.READ_EXTERNAL_STORAGE >/dev/null 2>&1
  adb_t shell pm revoke "$PKG" android.permission.WRITE_EXTERNAL_STORAGE >/dev/null 2>&1
  restart_app
  status=FAIL; evidence="painel de pasta externa indisponível não apareceu"
  if ui_tap_text "$E2E_PROJECT"; then
    i=0
    while [ "$i" -lt 3 ]; do
      if ui_has "No access to device storage"; then
        status=PASS; evidence="painel de pasta externa indisponível exibido no projeto"; break
      fi
      i=$((i + 1)); sleep 1
    done
  else
    status="NOT TESTED"; evidence="projeto $E2E_PROJECT não encontrado"
  fi
  adb_t shell pm grant "$PKG" android.permission.READ_EXTERNAL_STORAGE >/dev/null 2>&1
  adb_t shell pm grant "$PKG" android.permission.WRITE_EXTERNAL_STORAGE >/dev/null 2>&1
  record "$status" project-folder-revoke "$evidence"
}

run_all() {
  case_project_folder_session
  case_project_folder_revoke
  pending git-external-worktree "#6"
  case_launch
  case_arm64_only
}

if [ "$#" -eq 0 ]; then
  run_all
else
  for c in "$@"; do
    case "$c" in
      launch) case_launch ;;
      arm64-only) case_arm64_only ;;
      project-folder-session) case_project_folder_session ;;
      project-folder-revoke) case_project_folder_revoke ;;
      *) record "NOT TESTED" "$c" "caso desconhecido" ;;
    esac
  done
fi

printf '%s' "$results"
echo "E2E: PASS=$pass_count FAIL=$fail_count NOT_TESTED=$nt_count (alvo: $TARGET_LABEL, abi: ${abi:-?})"

[ "$fail_count" -eq 0 ] || exit 1
exit 0
