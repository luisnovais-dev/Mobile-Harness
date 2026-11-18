## Contexto

O envio/leitura de imagens estava quebrado de ponta a ponta neste runtime (PocketDev → DSH dentro de PRoot, em Android). O agente não conseguia usar a tool `read_image` para ver uma foto anexada. Foram identificadas **duas falhas independentes**, ambas com diagnóstico fechado por evidência, e uma **solução local já implementada e validada** neste workspace. Este issue registra a situação e propõe a correção **definitiva no app**, para que ela deixe de depender de scripts de reaplicação.

## Capacidade entregue

Como usuário, consigo anexar uma foto numa sessão e pedir ao agente para lê-la, sem rodar nenhum script de correção manual — mesmo após reiniciar o PocketDev ou atualizar o DSH.

## As duas causas raiz

| # | Sintoma | Causa raiz | Correção local atual |
|---|---------|------------|----------------------|
| 1 | `read_image` falha: *"model ... does not declare image input"* | A rota não declara a modalidade `image`; sem declaração o DSH assume `DEFAULT_INPUT = ["text"]` | Declarar `input: [text, image]` no modelo dentro de `/root/.dsh/settings.yaml` |
| 2 | Persistência falha: *"Unable to persist image attachment"* (cause: `EACCES`) | O commit de anexos publica objetos com `link(2)` (hard link); o domínio Android `u:r:untrusted_app_27` **nega hard links em qualquer filesystem** | Patch em `@deepseek-ai/dsh-attachment-local`: fallback `copyFile` + `rename` atômico |

### Problema 1 — visão bloqueada

A tool `read_image` (`@deepseek-ai/dsh-tool-fs`) roda, antes de ler, um `assertImageCapableRoute()`: resolve a rota ativa e exige que `inputModalities` inclua `"image"`. A resolução está em `dsh-llm-pi-ai/lib/index.js`:

```js
input: declaredInput(entry.input) ?? base?.input ?? [...request.defaultInput]
// MODALITIES válidos: apenas "text" e "image"; DEFAULT_INPUT (padrão): ["text"]
```

Na configuração original **nada era declarado** — nem `input` no modelo, nem `defaultInput` na rota — então a rota resolvia como `["text"]` puro e a tool recusava a leitura. Não há catálogo por modelo no DSH: a capacidade de visão é **genérica, declarada por modalidade**.

**Ponto crítico:** o PocketDev **regenera a `settings.yaml` a cada reinício**, desfazendo a declaração (observado às 14:03 e 14:06 de 16/11/2026). Ou seja, o fix manual é perdido a cada start.

### Problema 2 — anexo não persiste

`@deepseek-ai/dsh-attachment-local` → `commitPreparedImageFile()`: escreve o objeto em `tmp/` e o publica com **`link(temporary, target)`** (hard link) em `attachments/v1/objects/<2 chars>/<sha256>`. Neste ambiente `link()`/`linkat()` retornam **`EACCES` em qualquer filesystem**, inclusive no tmpfs real.

Evidências do diagnóstico:

| Teste | Resultado | O que prova |
|-------|-----------|-------------|
| `link` em `/tmp`, `/root/.dsh`, workspace | `EACCES` | falha em *todo* filesystem |
| `link(src inexistente)` | `ENOENT` | não é seccomp cego: o syscall chega ao kernel |
| `linkat` via `/proc/self/fd` (path real do host) | `EACCES` | **não é o path mascarado pelo proot** |
| `rename`, `symlink`, escrita de arquivo | `OK` | **só `link`/`linkat` é negado** |

Conclusão: a negação acontece **depois da resolução do path, no nível do inodo/LSM** — SELinux no domínio `untrusted_app_27` não concede `file:link`. Hard links são **permanentemente indisponíveis** neste runtime; não há configuração que libere. A correção local **contorna** com `copyFile` + `rename` (o storage é *content-addressed*, então bytes idênticos convergem no mesmo objeto e a semântica é preservada).

## Solução local já aplicada e validada

Dois scripts idempotentes neste workspace (`/workspace/curious-ramanujan/`):

- **`fix-vision.sh`** — adiciona `input: [text, image]` na `settings.yaml` (edição *line-based* + validação com o módulo `yaml` do Node + backup).
- **`fix-attachment-patch.sh`** — reaplica o fallback no pacote de anexos. Modo `exact` (reproduz o patch byte a byte quando o trecho upstream é o original) e modo `tolerante` (envolve a única linha `await link(...)`, caso o upstream mude). Valida com `node --check` + regressão ponta a ponta (`test_store.mjs`, salva a foto real, confere sha256 e readback).
- **`fix-all.sh`** — roda os dois.

Matriz de testes executada, todos passaram: aplicação do zero, idempotência, simulação de **update do DSH** (byte-idêntico), simulação de **derivação upstream** (modo tolerante), regressão ponta a ponta e teste ao vivo com `read_image`.

## Proposta de melhoria definitiva no app

Hoje as duas correções **são perdidas**: a `settings.yaml` é regenerada a cada restart e o pacote do DSH é sobrescrevido a cada update. Para aplicar de forma definitiva, sugere-se:

1. **Gerar a declaração de visão na origem.** Se o PocketDev monta a `settings.yaml` do provider na UI, ele deveria gravar `input: [text, image]` (ou expor um campo "input modalities"/"vision" por modelo) a cada start, em vez de descartar o campo. Assim a declaração nasce no app e sobrevive aos restarts.
2. **Não depender de hard link no commit de anexos.** Ou o app aplica o fallback `copyFile` + `rename` de forma definitiva (upstream do `dsh-attachment-local`, ou um hook/patch aplicado na instalação/atualização do DSH), ou o runtime passa a garantir que `link(2)` seja permitido — o que, dado o domínio SELinux, **não é possível**. Logo, o fallback de cópia é o caminho correto.
3. **Aplicar o patch automaticamente após cada atualização do DSH**, para que o usuário nunca precise rodar `fix-attachment-patch.sh` manualmente.

## Pendências bloqueantes

Nenhuma pendência de diagnóstico — as causas estão fechadas e as correções validadas. A pendência é de **escopo/decisão de implementação no app** (onde o app regenera a config e como ele distribui o patch do pacote).

## Fora de escopo / limitações honestas

- Declarar `image` faz o DSH **enviar** a imagem para o endpoint do provider (`baseURL` da rota). Se o upstream não processar imagens, o próximo erro vem do provider — teste único para saber.
- O patch vive em `/usr/local/lib/dsh/node_modules/...`, fora do workspace, sujeito a qualquer gestor de pacotes do DSH.
- Hard links continuam negados; a correção contorna, não libera `link(2)`.

## Referências

- Documentação completa do diagnóstico, diff do patch, matriz de testes e rollback: `DSH-VISION-FIX.md` (workspace da sessão).
- Scripts: `fix-vision.sh`, `fix-attachment-patch.sh`, `fix-all.sh`, `test_store.mjs`.
