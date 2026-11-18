# DSH + Visão: Problema e Resolução

> **Data:** 16/11/2026 · **Ambiente:** PocketDev (Android) rodando o DSH dentro de PRoot
> **Objetivo deste documento:** registrar o diagnóstico completo das duas falhas que
> impediam o envio/leitura de imagens neste runtime, a correção aplicada e os scripts
> de reaplicação para que as correções sobrevivam a reinícios e atualizações.

---

## Resumo executivo

| # | Sintoma | Causa raiz | Correção | Script |
|---|---------|------------|----------|--------|
| 1 | `read_image` falha: *"model ... does not declare image input"* | A rota não declara a modalidade `image`; sem declaração o DSH assume `DEFAULT_INPUT = ["text"]` | Declarar `input: [text, image]` no modelo dentro da `settings.yaml` | `fix-vision.sh` |
| 2 | Persistência falha: *"Unable to persist image attachment"* (cause: `EACCES`) | O commit de anexos publica objetos com `link(2)` (hard link); o domínio Android `u:r:untrusted_app_27` **nega hard links em qualquer filesystem** | Patch em `@deepseek-ai/dsh-attachment-local`: fallback `copyFile` + `rename` atômico | `fix-attachment-patch.sh` |

Os dois scripts podem ser rodados juntos com **`./fix-all.sh`**.

⚠️ **Os dois problemas voltam a aparecer sem os scripts:**
- o PocketDev **regenera a `settings.yaml` a cada reinício** (observado às 14:03 e 14:06 de 16/11/2026) → rodar `fix-vision.sh` após cada restart;
- uma **atualização do DSH sobrescreve** `node_modules/.../dsh-attachment-local/lib/index.js` → rodar `fix-attachment-patch.sh` após cada update.

---

## 1. Ambiente

| Item | Valor |
|------|-------|
| Runtime | PocketDev, app Android com PRoot (`libproot.so`) |
| `DSH_HOME` | `/root/.dsh` |
| Modelo | `command-code/xiaomi/mimo-v2.6-flash` (provider `mh-custom`, `api: openai-completions`, `baseURL: http://100.82.32.64:20128/v1`) |
| UID real do processo | **10400** (app Android), SELinux `u:r:untrusted_app_27` — o `id` mostra `root` porque o **proot falsifica o uid** nas respostas das syscalls |
| `PROOT_NO_SECCOMP` | `1` (o proot não instala filtro próprio; o seccomp ativo vem de fora — sandbox do app) |
| Ferramentas | Node v24.19.0 (npm 11.17.0), Python 3.8.10, Git 2.25.1, módulo Node `yaml` 2.9.0 |
| Stack Android (gradle/sdk/aapt2) | **não instalada** — adicionar em *PocketDev Settings* antes de tentar buildar um app |

---

## 2. Problema 1 — visão bloqueada ("does not declare image input")

### Mecanismo
A tool **`read_image`** (do pacote `@deepseek-ai/dsh-tool-fs`) roda, antes de ler,
um `assertImageCapableRoute()`: ele resolve a rota ativa e exige que
`inputModalities` inclua `"image"`.

A resolução da modalidade está em `dsh-llm-pi-ai/lib/index.js`:

```js
input: declaredInput(entry.input) ?? base?.input ?? [...request.defaultInput]
// MODALITIES válidos: apenas "text" e "image"
// DEFAULT_INPUT (padrão): ["text"]
```

Na configuração original **nada era declarado** — nem `input` no modelo, nem
`defaultInput` na rota — então a rota resolvia como `["text"]` puro e a tool
recusava a leitura. Não há no DSH nenhum catálogo por modelo (nada de "Hermes"
etc.): a capacidade de visão é **genérica, declarada por modalidade**.

### Correção
`/root/.dsh/settings.yaml`, dentro do item do modelo:

```yaml
      models:
        - id: 'command-code/xiaomi/mimo-v2.6-flash'
          input: [text, image]      # ← esta linha
```

### Validado
- O gate passou e a `read_image` leu a foto de ponta a ponta na mesma sessão
  (o DSH re-le a `settings` em runtime; se persistir erro, reiniciar a sessão).
- `fix-vision.sh` valida o YAML com o módulo `yaml` do Node depois de gravar.

### Por que reaplicar
O PocketDev regrava a `settings.yaml` no restart **sem** o campo `input`.
Backup do estado pré-fix: `/root/.dsh/settings.yaml.bak-vision`.

---

## 3. Problema 2 — anexo não persiste ("Unable to persist image attachment")

### Onde falha
`@deepseek-ai/dsh-attachment-local` → `commitPreparedImageFile()` (linha ~400 do
`lib/index.js`): escreve o objeto em `tmp/` e o publica com
**`link(temporary, target)`** (hard link) em `attachments/v1/objects/<2 chars>/<sha256>`.

Neste ambiente `link()`/`linkat()` retornam **`EACCES` em qualquer filesystem**,
inclusive no tmpfs real — o commit morre e o erro vira
`ATTACHMENT_WRITE_FAILED: "Unable to persist image attachment."`.

### Evidências do diagnóstico (16/11/2026)

| Teste | Resultado | O que prova |
|-------|-----------|-------------|
| `link` em `/tmp`, `/root/.dsh`, workspace | `EACCES` | falha em *todo* filesystem, inclusive os realmente nossos |
| `link(src inexistente)` | **`ENOENT`** | não é seccomp bloqueando cego: o syscall chega ao kernel |
| `linkat` via `/proc/self/fd` (path real do host, sem máscara do proot) | **`EACCES`** | **não é o path mascarado** — mesmo inodo, sem tradução, mesmo erro |
| varredura `linkat` sem seguir symlink | `EXDEV` | prova que `/proc` passa cru ao kernel (o proot não traduz ali) |
| `/proc/<pid>/root/` (raiz do tracer proot) | mesma raiz mascarada, sem `/data` | não há raiz "real" acessível de dentro |
| `rename`, `symlink`, criação/escrita de arquivo | `OK` | **só `link`/`linkat` é negado** |
| consulta SELinux (`/sys/fs/selinux/enforce`, `dmesg`) | negada | domínio `untrusted_app_27` bem confinesado; política não consultável daqui |

**Conclusão:** a negação acontece **depois da resolução do path, no nível do
inodo/LSM** — suspeito nº 1 a **SELinux** no domínio `u:r:untrusted_app_27`
(não concede `file:link`). O path do arquivo (mascarado ou real) é irrelevante;
o que importa é o **contexto do processo**. Hard links são, portanto,
**permanentemente indisponíveis** neste runtime — não há configuração que libere.

> **Sobre a hipótese original ("é o path mascarado pelo proot"):** o proot
> *está* mascarando paths (confirmado: `/data` some, `stat` mostra uid
> falsificado) — mas para *este* erro em específico o caminho não é o culpado.

### Rastro forense que confirmou o diagnóstico
`attachments/v1/objects/1d/` existia **vazio**, assim como `tmp/` — o rastro
exato de uma tentativa que falhou **no `link`** e limpou o temporário. O hash da
foto (`1de10124...`) cairia no bucket `1d/`.

### A correção
Patch em `dsh-attachment-local/lib/index.js` (o **único** `link()` de todo o
DSH — varredura confirmou). Dois hunks:

1. **Import:** `copyFile` adicionado ao `from "node:fs/promises"`.
2. **Fallback no `catch` do `link`:** se o erro for `EEXIST`, mantém a
   verificação de integridade upstream; **qualquer outro erro** publica com
   cópia privada + troca atômica:

```js
} else {
    /* Host LSM fallback: link(2) is denied for this sandbox (Android untrusted_app
    * cannot create hard links on any filesystem, including tmpfs). Publish through
    * a private copy plus atomic rename instead; the content-addressed target means
    * identical bytes converge on the same object, and only the final directory
    * entry is swapped. */
    const partial = join(staging, `${randomUUID()}.copy`);
    try {
        await copyFile(temporary, partial, constants.COPYFILE_EXCL);
        await rename(partial, target);
    } finally {
        await rm(partial, { force: true });
    }
}
```

**Por que é seguro:** o storage é *content-addressed* — o nome do objeto é o
`sha256` do conteúdo. Bytes idênticos convergem para o mesmo objeto, o
`rename` é atômico no mesmo diretório e o `COPYFILE_EXCL` evita corrida de
cópia. A semântica do protocolo upstream é preservada.

### Validação (com a foto real)
```
foto lida: 3.338.338 bytes
REF salva: sha256:1de10124... (1773×2364, 614.628 bytes, normalizada)
readback verificado: 614.628 bytes ✓
objeto em objects/1d/1de10124...  modo 0400 ✓   ← o bucket que estava vazio
```

---

## 4. Os scripts

Todos em `/workspace/curious-ramanujan/`, executáveis, idempotentes.

### `./fix-vision.sh`
Reaplica `input: [text, image]` na `settings.yaml`.
- Edição *line-based* (não há PyYAML no ambiente) + validação com o módulo `yaml` do Node.
- Idempotente: se já declarado, apenas valida e sai.
- Cria backup `/root/.dsh/settings.yaml.bak-vision` antes de alterar.
- Variáveis: `DSH_SETTINGS`, `DSH_MODEL_ID`, `DSH_INPUT_MODALITIES`.

### `./fix-attachment-patch.sh`
Reaplica o patch de fallback no pacote de anexos.
- **Modo `exact`:** o trecho upstream ainda é o original → reproduz o patch
  validado **byte a byte**.
- **Modo `tolerante`:** o upstream mudou a volta do `link` → envolve a linha
  única `await link(temporary, target);` com um `try/callback` próprio
  (EEXIST re-throw mantém a lógica de integridade upstream; demais erros caem no
  copy+rename). Não depende do texto ao redor.
- **Backup de referência:** `lib/index.js.orig-pristine` (uma única vez).
- **Validações:** `node --check` (cópia `.mjs`), marcador presente, e
  **regressão ponta a ponta** com `test_store.mjs` (salva a foto real pelo store,
  verifica hash e readback — falharia com `EACCES` se o patch estivesse ausente).
- Variáveis: `DSH_ATTACHMENT_INDEX`, `DSH_REGRESSION_TEST`, `SKIP_TEST=1`.

### `./fix-all.sh`
Roda os dois, nesta ordem.

### Quando rodar
```bash
cd /workspace/curious-ramanujan
./fix-all.sh          # após cada reinício do PocketDev e após cada update do DSH
```

---

## 5. Matriz de testes executada

| Cenário | Resultado |
|---------|-----------|
| `fix-vision.sh` com config sem `input` | ✅ aplica + YAML validado (`input=["text","image"]`) |
| `fix-vision.sh` rodado de novo (idempotência) | ✅ "já declarado", nenhuma escrita |
| `fix-attachment-patch.sh` com patch já presente | ✅ detecta, roda regressão, exit 0 |
| Simulação de update (restaura pristine) → script | ✅ modo `exact`, **byte-idêntico** ao patch validado, regressão OK |
| Simulação de derivação upstream (comentário alterado) → script | ✅ modo `tolerante`, `node --check` OK, regressão OK |
| `fix-attachment-patch.sh` sobre patch tolerante (idempotência) | ✅ "já presente" |
| `./fix-all.sh` completo | ✅ exit 0 |
| `bash -n` nos 3 scripts | ✅ sem erros de sintaxe |

Referências usadas nos testes (em `.ref/`):
- `deepseek-ai-dsh-attachment-local-0.1.2-rc.1.tgz` + `package/` — pristine do npm
  (obtido com `npm pack @deepseek-ai/dsh-attachment-local@0.1.2-rc.1`);
- `patched-validated.js` — o patch validado, para diff byte a byte.

---

## 6. Limitações honestas

1. **O upstream pode não processar imagens.** Declarar `image` faz o DSH
   *enviar* a imagem para `http://100.82.32.64:20128/v1`. Se o endpoint não
   aceitar, o próximo erro vem do provider — teste único para saber.
2. **Hard links continuam negados.** A correção *contorna* (copy+rename), não
   libera `link(2)` — isso é política de segurança do app Android e não é
   alterável de dentro.
3. **Atualizações do DSH** reescrevem o pacote → rodar `fix-attachment-patch.sh`.
4. **Reinícios do PocketDev** reescrevem a `settings.yaml` → rodar `fix-vision.sh`.
   (Se o app tiver um campo de "input modalities"/"vision" na UI do provider, é
   lá que a declaração deveria nascer — aí ele grava sozinho a cada start.)
5. O patch vive em `/usr/local/lib/dsh/node_modules/...` — fora do workspace,
   sujeito a qualquer gestor de pacotes do DSH.

---

## 7. Rollback

```bash
# desfazer o patch do anexo:
cp -p /usr/local/lib/dsh/node_modules/@deepseek-ai/dsh-attachment-local/lib/index.js.orig-pristine \
      /usr/local/lib/dsh/node_modules/@deepseek-ai/dsh-attachment-local/lib/index.js

# desfazer a declaração de visão:
cp -p /root/.dsh/settings.yaml.bak-vision /root/.dsh/settings.yaml
```

---

## 8. Arquivos deste workspace

| Arquivo | Papel |
|---------|-------|
| `fix-vision.sh` | reaplica `input: [text, image]` na settings |
| `fix-attachment-patch.sh` | reaplica o fallback `copy+rename` no pacote de anexos |
| `fix-all.sh` | roda os dois |
| `test_store.mjs` | regressão ponta a ponta do store de anexos (foto real) |
| `DSH-VISION-FIX.md` | este documento |
| `.ref/` | pristine do npm + patch validado (referência de diff) |
| `thumb_preview.jpg` | miniatura gerada durante o diagnóstico (Pillow em `pylib/`) |
| `attachments/` | fotos recebidas na sessão |
