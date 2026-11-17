#!/usr/bin/env sh
# Confere as chaves de .env sem exibir valores.
# As chaves esperadas são as de .env.example (linhas CHAVE=). Uma chave é
# considerada presente quando existe em .env com valor não vazio.
# Saída: uma linha por chave, com OK, VAZIA ou AUSENTE. Nunca imprime valores.
# Código de saída: 0 se todas as chaves estão preenchidas, 1 caso contrário.

set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
EXAMPLE="$ROOT/.env.example"
ENV_FILE="$ROOT/.env"

if [ ! -f "$EXAMPLE" ]; then
  echo "env-check: .env.example não encontrado em $ROOT" >&2
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  echo "env-check: .env ausente. Crie-o a partir de .env.example (valores locais, não versionados)." >&2
  exit 1
fi

status=0
keys=$(sed -n 's/^\([A-Za-z_][A-Za-z0-9_]*\)=.*/\1/p' "$EXAMPLE")

for key in $keys; do
  if ! grep -q "^${key}=" "$ENV_FILE"; then
    echo "AUSENTE $key"
    status=1
    continue
  fi
  value=$(sed -n "s/^${key}=\(.*\)$/\1/p" "$ENV_FILE" | tail -n 1)
  if [ -z "$value" ]; then
    echo "VAZIA   $key"
    status=1
  else
    echo "OK      $key"
  fi
done

exit "$status"
