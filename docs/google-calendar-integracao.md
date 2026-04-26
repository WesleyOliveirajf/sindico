# Integracao com Google Agenda

Este projeto ja possui o fluxo de criacao de compromissos (manutencao e reuniao) com tentativa de sincronizacao para a agenda do sindico.

## O que foi implementado

- tabela `compromissos` com colunas para status e rastreio de sincronizacao
- formulario no dashboard para criar compromisso
- sincronizacao no momento do cadastro (cria/atualiza evento no Google Calendar)
- registro de status da sincronizacao: `PENDENTE`, `SINCRONIZADO` ou `FALHA`

## Variaveis de ambiente

Defina as variaveis abaixo antes de subir a aplicacao:

- `GOOGLE_CALENDAR_ENABLED=true`
- `GOOGLE_CALENDAR_ID=<id do calendario do sindico>`
- `GOOGLE_CLIENT_ID=<oauth client id>`
- `GOOGLE_CLIENT_SECRET=<oauth client secret>`
- `GOOGLE_REFRESH_TOKEN=<refresh token com escopo de calendar>`

Se `GOOGLE_CALENDAR_ENABLED=false`, o compromisso e salvo localmente sem envio para o Google.

## Como obter as credenciais

1. Criar um projeto no Google Cloud.
2. Ativar a Google Calendar API.
3. Criar credencial OAuth 2.0 (tipo Web application).
4. Autorizar o usuario sindico no escopo `https://www.googleapis.com/auth/calendar`.
5. Obter e guardar o `refresh_token`.

## Observacoes

- Em caso de erro no Google, o sistema salva o compromisso e marca `google_sync_status = FALHA`.
- O campo `google_sync_erro` guarda a mensagem retornada para facilitar suporte.
- O horario enviado usa o timezone padrao da JVM (`ZoneId.systemDefault()`).
