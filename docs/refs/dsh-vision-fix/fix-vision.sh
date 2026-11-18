#!/usr/bin/env bash
# =============================================================================
# fix-vision.sh — Reaplica a declaracao de visao (input: [text, image]) na
# settings.yaml global do DSH.
#
# PROBLEMA: sem o campo `input` no modelo, a rota resolve como text-only
# (DEFAULT_INPUT = ["text"]) e a tool read_image falha com:
#   "model ... does not declare image input"
#
# POR QUE RODAR DE NOVO: o PocketDev REGRAVA a settings.yaml a cada
# reinicio do app (observado em 16/11/2026 14:03 e 14:06), desfazendo a
# declaracao. Rode este script depois de cada restart (ou use fix-all.sh).
#
# Uso:
#   ./fix-vision.sh
# Variaveis opcionais:
#   DSH_SETTINGS            caminho da settings.yaml (padrao: $DSH_HOME/settings.yaml)
#   DSH_MODEL_ID            id do modelo (padrao: command-code/xiaomi/mimo-v2.6-flash)
#   DSH_INPUT_MODALITIES    modalidades (padrao: [text, image])
# =============================================================================
set -euo pipefail

SETTINGS="${DSH_SETTINGS:-${DSH_HOME:-/root/.dsh}/settings.yaml}"
MODEL_ID="${DSH_MODEL_ID:-command-code/xiaomi/mimo-v2.6-flash}"
MODALITIES="${DSH_INPUT_MODALITIES:-[text, image]}"

if [ ! -f "$SETTINGS" ]; then
  echo "ERRO: settings nao encontrada: $SETTINGS" >&2
  exit 1
fi

echo "== fix-vision =="
echo "settings: $SETTINGS"
echo "modelo:   $MODEL_ID"

# --- edicao idempotente (line-based; nao ha PyYAML no ambiente) -------------
python3 - "$SETTINGS" "$MODEL_ID" "$MODALITIES" <<'PY'
import os, sys, tempfile

path, model, modalities = sys.argv[1], sys.argv[2], sys.argv[3]

with open(path, encoding="utf-8") as f:
    original = f.read()
lines = original.splitlines(keepends=True)

# localiza a linha `- id: '<model>'` dentro de models:
idx = None
for i, line in enumerate(lines):
    s = line.strip()
    if (s.startswith("- id:") or s.startswith("- id :")) and model in line:
        idx = i
        break
if idx is None:
    sys.stderr.write(
        "ERRO: modelo %r nao encontrado em %s.\n"
        "       (edite DSH_MODEL_ID ou verifique o formato da settings.)\n" % (model, path)
    )
    sys.exit(2)

base_indent = len(lines[idx]) - len(lines[idx].lstrip(" "))

# o item da lista segue enquanto houver linhas mais indentadas que o `-`:
input_idx = None
j = idx + 1
while j < len(lines):
    line = lines[j]
    if not line.strip():
        j += 1
        continue
    indent = len(line) - len(line.lstrip(" "))
    if indent <= base_indent:
        break
    if line.strip().startswith("input:"):
        input_idx = j
    j += 1

key_indent = " " * (base_indent + 2)
desired = "%sinput: %s\n" % (key_indent, modalities)

changed = False
if input_idx is None:
    lines.insert(idx + 1, desired)          # cria a chave como irma de `id:`
    changed = True
else:
    existing = lines[input_idx]
    if "image" not in existing:             # ex.: `input: [text]` -> inclui image
        nl = "\n" if existing.endswith("\n") else ""
        ind = " " * (len(existing) - len(existing.lstrip(" ")))
        lines[input_idx] = "%sinput: %s%s" % (ind, modalities, nl)
        changed = True

if not changed:
    print("OK: ja declarado -> %s" % lines[idx + 1].strip())
    sys.exit(0)

# backup do estado pre-fix (sobrescrito a cada correcao; referencia curta):
backup = path + ".bak-vision"
with open(backup, "w", encoding="utf-8") as f:
    f.write(original)

# gravacao atomica no mesmo diretorio:
directory = os.path.dirname(path) or "."
fd, tmp = tempfile.mkstemp(dir=directory, prefix=".settings.", suffix=".tmp")
try:
    with os.fdopen(fd, "w", encoding="utf-8") as f:
        f.write("".join(lines))
    os.replace(tmp, path)
except Exception:
    try:
        os.unlink(tmp)
    except OSError:
        pass
    raise

print("APLICADO: %s (backup em %s)" % (desired.strip(), backup))
PY

# --- validacao de sintaxe + semantica via modulo yaml do Node ---------------
node - "$SETTINGS" "$MODEL_ID" <<'JS'
const fs = require("fs");
let yaml;
try { yaml = require("/usr/local/lib/dsh/node_modules/yaml"); }
catch { yaml = require("yaml"); }

const [settingsPath, modelId] = process.argv.slice(2);
const doc = yaml.parse(fs.readFileSync(settingsPath, "utf8"));

let found = null;
const providers = (doc && doc["llm-pi-ai"] && doc["llm-pi-ai"].providers) || {};
for (const [name, provider] of Object.entries(providers)) {
  const models = Array.isArray(provider.models) ? provider.models : [];
  const hit = models.find((m) => m && m.id === modelId);
  if (hit) { found = { name, hit }; break; }
}

if (!found) {
  console.error("VALIDACAO FALHOU: modelo %s nao encontrado no YAML parseado", modelId);
  process.exit(1);
}
if (!Array.isArray(found.hit.input) || !found.hit.input.includes("image")) {
  console.error("VALIDACAO FALHOU: input do modelo = %s (sem 'image')",
    JSON.stringify(found.hit.input));
  process.exit(1);
}
console.log("YAML valido | provider=%s | input=%s",
  found.name, JSON.stringify(found.hit.input));
JS

echo "fix-vision: pronto. Se a sessao estiver aberta, o gate da read_image"
echo "            re-le a settings em runtime; caso erro persista, reinicie a sessao."
