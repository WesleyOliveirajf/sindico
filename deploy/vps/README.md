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

## Atualizar versao

```bash
cd /opt/sindico && git pull && cd deploy/vps && docker compose up -d --build
```

## Rollback

Enquanto o Railway estiver de pe, basta voltar o `destination` do `frontend/vercel.json`.
