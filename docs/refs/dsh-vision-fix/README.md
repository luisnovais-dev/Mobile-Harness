# Referência — correção de visão do DSH (issue #28)

> ⚠️ **Arquivos de REFERÊNCIA, não devem ser incorporados ao produto.**
>
> Esta pasta existe apenas para que a investigação e a solução local **não se
> percam**. O desenvolvimento da correção definitiva **não acontece neste
> repositório** — será aplicado no aplicativo quando a
> [issue #28](https://github.com/luisnovais-dev/Mobile-Harness/issues/28) for
> atendida.
>
> **Destinação:** quando a issue #28 for resolvida, esta pasta inteira deve ser
> **aposentada** (removida da árvore) — os scripts aqui não são parte do
> build, do app nem do processo do projeto.

## Contexto

Neste runtime (PocketDev → DSH dentro de PRoot/Android) a leitura de imagens
pela tool `read_image` quebrava por duas causas independentes:

1. **`does not declare image input`** — a rota não declarava `input: [text, image]`
   no `settings.yaml`, e o DSH assume `["text"]` por padrão.
2. **`Unable to persist image attachment` (EACCES)** — o `link(2)` de hard link
   usado no commit de anexos (`dsh-attachment-local`) é negado pelo SELinux no
   domínio `u:r:untrusted_app_27`, em qualquer filesystem.

Ambas foram diagnosticadas, corrigidas localmente e validadas de ponta a ponta
(foto real persistida + `read_image` funcionando). O relatório completo está em
[`DSH-VISION-FIX.md`](DSH-VISION-FIX.md).

## Conteúdo

| Arquivo | O que é |
|---|---|
| `DSH-VISION-FIX.md` | Documentação completa: ambiente, causas raiz, evidências do diagnóstico, diff do patch, matriz de testes, limitações |
| `fix-vision.sh` | Reaplica `input: [text, image]` no `settings.yaml` (idempotente; o PocketDev regenera o arquivo a cada restart) |
| `fix-attachment-patch.sh` | Reaplica o fallback `copyFile`+`rename` no lugar do `link()` de `dsh-attachment-local` (modos `exact` e tolerante a deriva upstream; inclui regressão ponta a ponta) |
| `fix-all.sh` | Executa os dois acima |
| `test_store.mjs` | Regressão: salva uma imagem real pelo store do DSH e confere sha256 + readback |
| `issue-body.md` | Corpo da issue #28 (para atualizar com `gh issue edit 28 --body-file`) |

## Como usar (ambiente afetado, apenas como referência)

```bash
./fix-all.sh
```

Executar após **cada restart do PocketDev** (ele regenera o `settings.yaml`) e
após **cada atualização do DSH** (sobrescreve o pacote patcheado).

## Limitações conhecidas

- Os scripts dependem de caminhos do runtime PocketDev/DSH
  (`/root/.dsh/settings.yaml`, `/usr/local/lib/dsh/node_modules/...`) e não
  têm utilidade fora desse ambiente.
- A liberação definitiva de `link(2)` não é possível dado o domínio SELinux —
  a correção permanente é não depender de hard link no commit de anexos.
