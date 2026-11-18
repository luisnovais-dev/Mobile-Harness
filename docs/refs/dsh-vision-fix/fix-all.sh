#!/usr/bin/env bash
# =============================================================================
# fix-all.sh — Reaplica TODAS as correcoes do DSH de uma vez.
# Rode depois de cada reinicio do PocketDev e depois de cada atualizacao do DSH.
#
#   1) fix-vision.sh            -> input: [text, image] na settings.yaml
#                                  (o app regenera a settings no restart)
#   2) fix-attachment-patch.sh  -> fallback copy+rename no lugar do link(2)
#                                  (uma atualizacao do DSH reescreve o pacote)
# =============================================================================
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"

"$here/fix-vision.sh"
echo
"$here/fix-attachment-patch.sh"
echo
echo "Todas as correcoes aplicadas."
