#!/usr/bin/env bash
# =============================================================================
# fix-attachment-patch.sh — Reaplica o patch de fallback do DSH que torna o
# envio/persistencia de imagens possivel neste runtime.
#
# PROBLEMA: o commit de anexos do @deepseek-ai/dsh-attachment-local publica o
# objeto com link(2) (hard link). Neste ambiente (app Android, dominio
# SELinux u:r:untrusted_app_27, dentro de PRoot) link/linkat sao negados com
# EACCES em QUALQUER filesystem — inclusive tmpfs. Resultado:
#   "Unable to persist image attachment" (cause: EACCES no link)
#
# SOLUCAO: se link falhar com algo alem de EEXIST, publicar com
# copyFile(parcial) + rename() atomico. Como o storage e content-addressed
# (nome = sha256 do conteudo), bytes identicos convergem para o mesmo objeto
# — a semantica do protocolo e preservada.
#
# POR QUE RODAR DE NOVO: uma atualizacao do DSH sobrescreve
# node_modules/@deepseek-ai/dsh-attachment-local/lib/index.js e o patch some.
# Rode este script depois de qualquer update (ou use fix-all.sh).
#
# Uso:
#   ./fix-attachment-patch.sh          # aplica se preciso + teste de regressao
#   SKIP_TEST=1 ./fix-attachment-patch.sh
# Variaveis opcionais:
#   DSH_ATTACHMENT_INDEX     caminho do lib/index.js do pacote
#   DSH_REGRESSION_TEST      script .mjs de regressao (padrao: ./test_store.mjs)
# =============================================================================
set -euo pipefail

FILE="${DSH_ATTACHMENT_INDEX:-/usr/local/lib/dsh/node_modules/@deepseek-ai/dsh-attachment-local/lib/index.js}"
MARKER="Host LSM fallback"
# patcheado = modo exato (marcador) OU modo tolerante (wrap proprio):
PATCHED_PATTERN='Host LSM fallback|linkFallbackError'

echo "== fix-attachment-patch =="
echo "alvo: $FILE"

if [ ! -f "$FILE" ]; then
  echo "ERRO: pacote nao encontrado: $FILE" >&2
  echo "      (o DSH foi movido/atualizado? ajuste DSH_ATTACHMENT_INDEX)" >&2
  exit 1
fi

if grep -qE "$PATCHED_PATTERN" "$FILE"; then
  echo "OK: patch ja presente (fallback de copy+rename ativo)."
else
  # referencia pristine uma unica vez, para auditoria/diff:
  if [ ! -f "$FILE.orig-pristine" ]; then
    cp -p "$FILE" "$FILE.orig-pristine"
    echo "backup pristine salvo em: $FILE.orig-pristine"
  fi

  python3 - "$FILE" <<'PY'
import re, sys

path = sys.argv[1]
with open(path, encoding="utf-8") as f:
    src = f.read()

# ---------------------------------------------------------------------------
# Bloco EXATO: reproduz o patch validado byte a byte quando o trecho
# upstream ainda e o original (0.1.2-rc.1).
# ---------------------------------------------------------------------------
OLD = (
    "\t\t\tawait link(temporary, target);\n"
    "\t\t} catch (error) {\n"
    "\t\t\t/* v8 ignore next -- Private same-filesystem directories make EEXIST the only recoverable link race. */\n"
    "\t\t\tif (!(error instanceof Error && \"code\" in error && error.code === \"EEXIST\")) throw error;\n"
    "\t\t\tif (digest$1(new Uint8Array(await readFile(target))) !== sha256) throw new AttachmentError(\"Stored attachment failed integrity verification.\", \"ATTACHMENT_CORRUPT\");\n"
    "\t\t}\n"
)
NEW = (
    "\t\t\tawait link(temporary, target);\n"
    "\t\t} catch (error) {\n"
    "\t\t\t/* v8 ignore next -- Private same-filesystem directories make EEXIST the only recoverable link race. */\n"
    "\t\t\tif (error instanceof Error && \"code\" in error && error.code === \"EEXIST\") {\n"
    "\t\t\t\tif (digest$1(new Uint8Array(await readFile(target))) !== sha256) throw new AttachmentError(\"Stored attachment failed integrity verification.\", \"ATTACHMENT_CORRUPT\");\n"
    "\t\t\t} else {\n"
    "\t\t\t\t/* Host LSM fallback: link(2) is denied for this sandbox (Android untrusted_app\n"
    "\t\t\t\t* cannot create hard links on any filesystem, including tmpfs). Publish through\n"
    "\t\t\t\t* a private copy plus atomic rename instead; the content-addressed target means\n"
    "\t\t\t\t* identical bytes converge on the same object, and only the final directory\n"
    "\t\t\t\t* entry is swapped. */\n"
    "\t\t\t\tconst partial = join(staging, `${randomUUID()}.copy`);\n"
    "\t\t\t\ttry {\n"
    "\t\t\t\t\tawait copyFile(temporary, partial, constants.COPYFILE_EXCL);\n"
    "\t\t\t\t\tawait rename(partial, target);\n"
    "\t\t\t\t} finally {\n"
    "\t\t\t\t\tawait rm(partial, { force: true });\n"
    "\t\t\t\t}\n"
    "\t\t\t}\n"
    "\t\t}\n"
)

mode = None
if NEW in src:
    mode = "already"
    out = src
elif OLD in src:
    mode = "exact"
    out = src.replace(OLD, NEW, 1)
else:
    # -----------------------------------------------------------------------
    # Caminho TOLERANTE: o upstream mudou a volta do link. Envolvemos a linha
    # unica `await link(...)` com um try/catch proprio: EEXIST re-throw
    # (mantem a logica upstream de integridade) e qualquer outro erro cai no
    # copy+rename. Nao depende do texto ao redor.
    # -----------------------------------------------------------------------
    anchor = "await link(temporary, target);"
    if src.count(anchor) != 1:
        sys.stderr.write(
            "ERRO: nao encontrei o ponto de aplicacao.\n"
            "  - marcador de patch ausente;\n"
            "  - bloco original alterado pelo upstream;\n"
            "  - ocorrencias de %r: %d (esperado 1).\n"
            "  Inspecione %s e ajuste o script.\n" % (anchor, src.count(anchor), path)
        )
        sys.exit(3)

    lines = src.splitlines(keepends=True)
    i = next(k for k, l in enumerate(lines) if anchor in l)
    line = lines[i]
    indent = line[: len(line) - len(line.lstrip("\t "))]

    wrap = (
        indent + "try {\n"
        + indent + "\t" + line.lstrip("\t ")
        + indent + "} catch (linkFallbackError) {\n"
        + indent + "\t/* Host LSM fallback: link(2) is denied for this sandbox (Android untrusted_app\n"
        + indent + "\t* cannot create hard links on any filesystem, including tmpfs). Publish through\n"
        + indent + "\t* a private copy plus atomic rename instead; the content-addressed target means\n"
        + indent + "\t* identical bytes converge on the same object, and only the final directory\n"
        + indent + "\t* entry is swapped. */\n"
        + indent + "\tif (linkFallbackError instanceof Error && \"code\" in linkFallbackError"
        + " && linkFallbackError.code === \"EEXIST\") throw linkFallbackError;\n"
        + indent + "\tconst partial = join(staging, `" + "${randomUUID()}.copy`);\n"
        + indent + "\ttry {\n"
        + indent + "\t\tawait copyFile(temporary, partial, constants.COPYFILE_EXCL);\n"
        + indent + "\t\tawait rename(partial, target);\n"
        + indent + "\t} finally {\n"
        + indent + "\t\tawait rm(partial, { force: true });\n"
        + indent + "\t}\n"
        + indent + "}\n"
    )
    lines[i] = wrap
    out = "".join(lines)
    mode = "tolerant"

# ---------------------------------------------------------------------------
# Garante o import de copyFile na linha do node:fs/promises (obrigatorio para
# ambos os modos de patch).
# ---------------------------------------------------------------------------
m = re.search(r'^import \{[^}]*\} from "node:fs/promises";$', out, re.M)
if not m:
    sys.stderr.write("ERRO: linha de import de node:fs/promises nao encontrada.\n")
    sys.exit(4)
names = [n.strip() for n in m.group(0)[m.group(0).index("{") + 1 : m.group(0).index("}")].split(",")]
if "copyFile" not in names:
    if names == sorted(names):        # lista ordenada (caso upstream) -> insere na ordem
        pos = next((k for k, n in enumerate(names) if n > "copyFile"), len(names))
        names.insert(pos, "copyFile")
        new_import = 'import { ' + ", ".join(names) + ' } from "node:fs/promises";'
    else:                             # ordem estranha -> so garante a presenca
        new_import = m.group(0).replace("{", "{ copyFile,", 1)
    out = out[: m.start()] + new_import + out[m.end() :]

if out == src:
    print("OK: nada a alterar.")
    sys.exit(0)

with open(path, "w", encoding="utf-8") as f:
    f.write(out)

print("PATCH APLICADO (modo: %s)" % mode)
PY

  # validacao sintatica: copia como .mjs para o node --check parsear como ESM
  tmp_check="$(mktemp --suffix=.mjs)"
  cp "$FILE" "$tmp_check"
  node --check "$tmp_check" || { rm -f "$tmp_check"; echo "ERRO: sintaxe invalida apos patch." >&2; exit 5; }
  rm -f "$tmp_check"
  echo "sintaxe: OK (node --check)"

  grep -qE "$PATCHED_PATTERN" "$FILE" || { echo "ERRO: marcador do patch ausente apos gravacao." >&2; exit 6; }
fi

# ---------------------------------------------------------------------------
# Regressao ponta-a-ponta: salva a foto real pelo store, exatamente como a
# read_image faria (persistencia + verificacao de integridade + readback).
# ---------------------------------------------------------------------------
TEST="${DSH_REGRESSION_TEST:-$(dirname "$0")/test_store.mjs}"
if [ "${SKIP_TEST:-0}" = "1" ]; then
  echo "regressao: pulada (SKIP_TEST=1)"
elif [ -f "$TEST" ]; then
  echo "regressao: node $TEST"
  node "$TEST"
else
  echo "regressao: script nao encontrado ($TEST) — pulada"
fi

echo "fix-attachment-patch: pronto."
