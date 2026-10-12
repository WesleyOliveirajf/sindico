# Deploy do back-end em VPS Ubuntu (Docker Compose + Traefik)

Somente o back-end Spring Boot. O banco continua no Supabase.
HTTPS e roteamento ficam a cargo do Traefik ja rodando na VPS (rede `n8n_default`,
resolver `mytlschallenge`). Nao ha Caddy nem Postgres neste compose.

## Pre-requisitos

- Registro DNS `A` do dominio (`DOMAIN`) apontando para o IP da VPS.
- Traefik na rede `n8n_default` com entrypoint `websecure` e resolver `mytlschallenge`
  (ajuste os labels do `docker-compose.yml` se o seu for diferente).

## Subir

```bash
git clone https://github.com/WesleyOliveirajf/sindico.git /opt/sindico
cd /opt/sindico/deploy/vps
cp env.example .env && nano .env      # preencher DB_PASSWORD, APP_JWT_SECRET etc.
docker compose up -d --build
docker compose logs -f app
```

Teste: `https://<DOMAIN>/actuator/health` -> `{"status":"UP"}`.

## Front (Vercel)

O front chama `/api/*` por rewrite em `frontend/vercel.json`. Troque o `destination`
para `https://<DOMAIN>/api/:path*` e faca redeploy.

## Anexos

- `APP_STORAGE_PROVIDER=local`: grava no volume `uploads` (persistente, mas so nesta VPS;
  inclua o volume nos backups da VPS).
- `APP_STORAGE_PROVIDER=supabase`: usa Supabase Storage (`SUPABASE_URL`, `SUPABASE_SERVICE_KEY`).

## Backup dos anexos

O banco fica no Supabase (com backup proprio). Os anexos ficam no volume `sindico_uploads`,
so nesta VPS. Backup diario as 03:00, mantendo 14 dias:

```bash
chmod +x /opt/sindico/deploy/vps/backup-uploads.sh
( crontab -l 2>/dev/null; echo "0 3 * * * /opt/sindico/deploy/vps/backup-uploads.sh >> /var/log/sindico-backup.log 2>&1" ) | crontab -
```

Backup so na mesma VPS nao protege contra perda da maquina: configure copia off-site
(rclone) no final do script. Restaurar:
`docker run --rm -v sindico_uploads:/data -v /var/backups/sindico:/in alpine tar xzf /in/uploads-XXXX.tar.gz -C /data`
(depois ajuste o dono: `chown -R 10001:10001` no volume).

## Atualizar versao

```bash
cd /opt/sindico && git pull && cd deploy/vps && docker compose up -d --build
```

## Rotacao do segredo do proxy (sem downtime)

O segredo compartilhado (`APP_TRUSTED_PROXY_SECRET` na VPS e `PROXY_SHARED_SECRET` no Vercel)
autentica o IP real repassado pelo middleware para rate limit e auditoria LGPD.

O back-end aceita simultaneamente dois segredos (`APP_TRUSTED_PROXY_SECRET` e `APP_TRUSTED_PROXY_SECRET_PREVIOUS`),
permitindo rotacionar sem que nenhum usuario perca seu bucket de rate limit nem receba 429 indevido.

Procedimento passo a passo:

1. **Gerar novo segredo:**
   ```bash
   openssl rand -hex 32
   ```

2. **Atualizar o back-end (VPS) mantendo o antigo como anterior:**
   No arquivo `/opt/sindico/deploy/vps/.env`:
   - Defina `APP_TRUSTED_PROXY_SECRET=<novo_segredo>`
   - Defina `APP_TRUSTED_PROXY_SECRET_PREVIOUS=<segredo_antigo>`
   Aplique a alteracao no container:
   ```bash
   cd /opt/sindico/deploy/vps && docker compose up -d
   ```
   *Neste momento, o back-end aceita tanto o novo segredo quanto o anterior.*

3. **Atualizar o front-end (Vercel):**
   - Acesse o painel do Vercel > Settings > Environment Variables.
   - Atualize `PROXY_SHARED_SECRET` com `<novo_segredo>`.
   - Realize o redeploy da aplicacao para que a borda passe a enviar o novo segredo.

4. **Verificar os logs na VPS:**
   ```bash
   docker compose logs -f app
   ```
   Acompanhe os logs. Se houver descompasso de segredo, o back-end registrara alertas `WARN`
   para rotas `/api/auth/*` informando que o cabecalho confiavel nao foi validado.
   Certifique-se de que os logins estao normais e sem novos alertas.

5. **Remover o segredo anterior da VPS:**
   Apos a propagacao do Vercel, no arquivo `/opt/sindico/deploy/vps/.env`:
   - Deixe `APP_TRUSTED_PROXY_SECRET_PREVIOUS=` vazio.
   Aplique a alteracao:
   ```bash
   cd /opt/sindico/deploy/vps && docker compose up -d
   ```

## Rollback

Ver `docs/DEPLOY.md` (secao Rollback).
