# ADR 0002: Montagem somente da pasta do projeto

- Status: aceita
- Data: 2026-11-17
- Issue: #5 (épico #3)
- Substitui: decisão 3 da ADR 0001 (montagem de `/storage/emulated/0` como `/sdcard`)

## Contexto

A ADR 0001 montava o armazenamento compartilhado inteiro dentro da sessão PRoot, com `-b /storage/emulated/0:/sdcard`, quando a permissão existia. Isso dava à sessão acesso a todo o volume primário, e não só ao projeto.

Com a escolha do diretório do projeto (#5), o projeto passa a viver numa pasta do aparelho, por exemplo `/storage/emulated/0/MeuProjeto`. O que a sessão precisa é desse projeto, e não do volume inteiro.

## Decisão

1. **Montagem.** Com acesso, a sessão monta somente a pasta do projeto, como `/workspace/<slug>`. A montagem de `/storage/emulated/0` como `/sdcard` deixa de existir. `RuntimeInstaller.process` deixa de acrescentar `-b /storage/emulated/0:/sdcard`. O bind do workspace já existente passa a ser o único caminho externo do projeto.
2. **Leitura de pastas externas para contexto.** Faz parte da entrega da #5, não de ticket separado. A sessão não lê pastas externas montadas. O app lê a pasta, com a permissão que ele já tem, e copia os arquivos escolhidos para `attachments/<chatId>` dentro do workspace do projeto. Esse é o mecanismo de anexo já existente (`MainViewModel.copyChatAttachment`), que copia via `ContentResolver` e aplica os mesmos limites de tipo e tamanho. A autorização é por sessão: quando o usuário autoriza uma pasta, o app pode lê-la até o fim da sessão, e uma sessão nova pede autorização de novo. A escrita continua restrita ao checkout do projeto.
3. **Worktrees e histórico (#6, opção 1).** As worktrees de um projeto ficam dentro da pasta do projeto, em `.worktrees/<branch>`. Isso substitui a decisão 1 do épico #3, que colocava as worktrees em `Android/data/com.jarves.mh/...`. O histórico git (`gitdir`) fica no rootfs privado, em `/root/gd/<slug>`, e a pasta do projeto recebe um arquivo `.git` que aponta para ele (`--separate-git-dir`). `.worktrees/` entra em `info/exclude` do `gitdir`, e não no `.gitignore` do projeto do usuário.
4. **Compartilhamento com outro app.** Arquivos de projeto externo são copiados para `cache/camera/` antes de compartilhar, e o `file_paths.xml` não muda. Hoje não existe essa cópia: `shareWorkspaceFile` chama `getUriForFile` no arquivo do workspace (`MainViewModel.kt:4550`). A cópia vale para qualquer arquivo do projeto, não só para o externo, para manter um único caminho de compartilhamento. A cópia é removida depois de o app compartilhá-la, ou na próxima abertura do app, para não acumular arquivos do projeto no cache.

## Alternativas descartadas

- **Manter `/sdcard` montado e restringir por convenção.** Qualquer comando da sessão enxerga o volume inteiro. A restrição fica na política, não no mecanismo.
- **Montar cada pasta autorizada para leitura.** Cada pasta vira um bind novo, e o proot precisa de argv variável por sessão. Além disso, a montagem vale até o fim da sessão, o que não é o modelo pedido.
- **Expor leitura por `content://` próprio para a sessão.** Exige um provedor exportado e uma ponte de leitura nova no proot. Mais superfície do que a cópia pelo app.

## Consequências

- A permissão de armazenamento continua necessária. O app precisa dela para gravar a pasta do projeto e para ler pastas que o usuário autoriza. O que muda é o escopo da montagem na sessão.
- Pastas autorizadas para leitura são copiadas, e não vistas ao vivo. Uma mudança feita fora depois da cópia só aparece numa nova cópia.
- Worktrees ficam visíveis para outros apps dentro da pasta do projeto. Isso é aceito pelo mantenedor.
- A ADR 0001 mantém o restante: a escolha de manifesto por distribuição, a decisão de acesso em `SharedStorageAccess` e o fallback sem permissão.
- Git sobre armazenamento FUSE (`/sdcard`) não grava objetos nem índice (Q1, ver Verificação). Por isso o histórico fica no rootfs privado.
- Limitação conhecida: symlink versionado não é criado na pasta do projeto, e o bit de execução não persiste. O `git init` com `--separate-git-dir` testa o modo no rootfs e grava `core.filemode=true`, o que faria cada arquivo com `+x` aparecer como modificado. Por isso o clone externo grava `core.filemode=false` logo após o clone (`ProjectGitStore.filemodeConfigArgs`). Coberto pelo caso E2E `git-external-limits` (symlink e bit de execução) e pelo teste unitário `filemodeIsDisabledForExternalFolder`. A versão anterior desta ADR afirmava que o git detectava `core.filemode=false` sozinho; isso valia só para repositório dentro da pasta externa, que a opção 1 não usa.
- Perda do histórico ao limpar dados do app ou reinstalar o runtime: o `gitdir` some, e a sessão avisa (E3). O backup do app não inclui o histórico.

## Verificação

| # | Premissa | Resultado |
|---|---|---|
| P1 | A montagem de `/storage/emulated/0` está em `RuntimeInstaller.kt:1589` via `sharedStorageBinds` | Verificada |
| P2 | O anexo de chat copia para `attachments/<chatId>` dentro da raiz do projeto, sem montagem | Verificada em `MainViewModel.kt:4650` a `4680` |
| P3 | `.gitignore` inclui `.worktrees/` | **Falsa.** Inclui apenas `.claude/worktrees/` (linha 89). Precisa ser adicionado. |
| P4 | Compartilhamento hoje copia para `cache/` | **Falsa.** `shareWorkspaceFile` (`MainViewModel.kt:4550`) expõe o arquivo original. Não existe cópia. Ver decisão 4. |

Q1 do #6, com o processo do app em um aparelho Motorola One Action (Android 11, arm64-v8a):

| Item | Resultado |
|---|---|
| `git add` na pasta externa com o `gitdir` dentro dela | Falha: `failed to insert into database`, sem índice nem objetos (exit 128) |
| `ln -s` na pasta externa | Falha: o link não é criado |
| `chmod +x` na pasta externa | Não persiste: `-rw-rw----` |
| `git add`, `commit` e `worktree add` com `gitdir` privado e árvore de trabalho externa | Funciona |
| `.git` arquivo na pasta externa apontando para o `gitdir` privado | `git status` e `git worktree add` funcionam na sessão, sem variáveis de ambiente |
| `ln` (hard link) na pasta externa | O PRoot emula `link()` movendo o original para `.l2s.<nome>`; não usar hard link na pasta externa |

Decisão 4 do #6 (aprovada pelo mantenedor): opção 1, com as limitações acima.

## Referências

- ADR 0001, decisão 3, substituída por esta.
- Épico #3, decisão 1 (worktrees), substituída por esta no que se refere ao local das worktrees.
