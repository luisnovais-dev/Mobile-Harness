# ADR 0001: Acesso ao armazenamento compartilhado por distribuição

- Status: aceita (decisão 3 substituída pela ADR 0002)
- Data: 2026-11-16
- Issue: #4 (épico #3)

## Contexto

A sessão PRoot do Mobile Harness precisa enxergar o armazenamento compartilhado do aparelho (`/storage/emulated/0`) para que o usuário trabalhe nos próprios arquivos a partir de um projeto. A montagem depende de a permissão estar concedida. O app tem duas distribuições:

- **sideload** (APK direto, `targetSdk` 28): usa `WRITE_EXTERNAL_STORAGE` e `READ_EXTERNAL_STORAGE`, pedidas em runtime.
- **Play** (`-PplayBuild=true`, `targetSdk` 36): o modelo de armazenamento do Android 11+ exige `MANAGE_EXTERNAL_STORAGE` para acesso amplo, concedido pelo usuário na tela de "Acesso a todos os arquivos".

## Decisão

1. O manifesto por distribuição é escolhido por `androidComponents.onVariants`, com `variant.sources.manifests.addStaticManifestFile`, conforme a propriedade `playBuild`:
   - sideload: `app/src/sideload/AndroidManifest.xml` declara `WRITE` e `READ`;
   - Play: `app/src/play/AndroidManifest.xml` declara `MANAGE`.
2. A decisão de acesso fica em `SharedStorageAccess.isGranted`:
   - sideload: `WRITE` e `READ` concedidas;
   - Play: API 30+ e `Environment.isExternalStorageManager()`.
3. Com acesso, `RuntimeInstaller.process` acrescenta `-b /storage/emulated/0:/sdcard` ao argv do PRoot. Sem acesso, o argv fica idêntico ao anterior.
4. A solicitação segue a distribuição: sideload abre o pedido de `WRITE` e `READ`; Play abre `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`.
5. Sem feature flag. Acesso pelo projeto é capacidade do usuário, não comportamento incompleto.

## Alternativas descartadas

- **Seletor do sistema (`ACTION_OPEN_DOCUMENT_TREE`) como único caminho:** entrega URIs de documento, e a sessão PRoot precisa de um caminho POSIX montado. O seletor fica para a #5, para escolher a pasta do projeto, e não substitui o acesso ao volume primário nesta entrega.
- **`MANAGE_EXTERNAL_STORAGE` também no sideload:** o sideload já aceita `WRITE`/`READ` em `targetSdk` 28, então uma permissão ampla não seria necessária.
- **`WRITE`/`READ` no build Play:** no `targetSdk` 36 o Android não concede acesso amplo por essas permissões. O código não depende disso.

## Verificação (premissas P1–P6 da Spec)

| # | Premissa | Resultado |
|---|---|---|
| P1 | `WRITE`+`READ` dão leitura e escrita em `/sdcard` no `targetSdk` 28 | Verificada no aparelho |
| P2 | Sem permissão, o app inicia e não acessa `/sdcard` | Verificada |
| P3 | Revogação em runtime não quebra o app | Verificada |
| P3b | Revogação não quebra a sessão PRoot | Verificada: sessão sobe sem bind |
| P4 | O bind dá `ls /sdcard` real na sessão | Verificada |
| P5 | No build Play, `isExternalStorageManager()` controla o bind | Verificada com `appops` no spike |
| P6 | O manifesto varia por `playBuild` | Verificada nos dois builds |

## Consequências

- O sideload expõe o armazenamento compartilhado a partir da sessão, com o consentimento do usuário em runtime.
- O build Play depende de aprovação da Play Console para `MANAGE_EXTERNAL_STORAGE`, com justificativa de uso. A aprovação não é prioridade agora, e o risco de rejeição fica registrado.
- A revogação encerra o processo do app, e o Android o reinicia. A sessão sobe sem o bind.
- Worktrees e Git continuam fora desta decisão: ficam na #6.

## Referências

- Spec da issue #4 e progresso em `.agent/progresso.md` (não versionado).
- Épico #3, Q4 (decisão de distribuição).
