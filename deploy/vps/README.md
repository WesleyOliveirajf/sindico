# Deploy em VPS Ubuntu (Docker Compose + Caddy + Postgres)

Stack: `caddy` (HTTPS automatico) -> `app` (Spring Boot) -> `db` (Postgres 16, rede interna).

## 1. Preparar a VPS

```bash
# Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # relogar depois

# Firewall: so SSH, HTTP, HTTPS
sudo ufw allow OpenSSH && sudo ufw allow 80,443/tcp && sudo ufw enable
```

Dominio: aponte um registro **A** para o IP da VPS (ex.: subdominio gratis em duckdns.org).
Portas 80/443 precisam estar livres para o Caddy emitir o certificado.

## 2. Subir

```bash
git clone <repo> sindico && cd sindico/deploy/vps
cp env.example .env && nano .env      # preencher
docker compose up -d --build
docker compose logs -f app
```

## 3. Migrar dados do Supabase

Rode **antes** do primeiro start do app (ou pare o `app`: `docker compose stop app`).

```bash
# Na VPS (ou local), dump so do schema public, sem owners/roles do Supabase:
pg_dump "postgresql://postgres:SENHA@db.gbzmribcjrgxvzbibcqp.supabase.co:5432/postgres" \
  --schema=public --no-owner --no-privileges --no-acl -Fc -f sindico.dump

# Restaurar:
docker compose up -d db
docker compose exec -T db pg_restore -U sindico -d sindico --no-owner < sindico.dump
docker compose up -d app
```

O dump traz `flyway_schema_history`, entao o Flyway continua de onde parou.
Se os anexos estavam no Supabase Storage, baixe os arquivos do bucket e importe
no volume `uploads` (ou mantenha `APP_STORAGE_PROVIDER=supabase` com `SUPABASE_URL`/`SUPABASE_SERVICE_KEY`).

## 4. Front (Vercel)

Trocar a URL da API para `https://$DOMAIN` (variavel de ambiente do front) e redeployar.

## 5. Backup

```bash
chmod +x backup.sh
sudo mkdir -p /var/backups/sindico && sudo chown $USER /var/backups/sindico
( crontab -l 2>/dev/null; echo "0 3 * * * $(pwd)/backup.sh >> /var/log/sindico-backup.log 2>&1" ) | crontab -
```

Configure tambem copia off-site (rclone para Drive/B2/R2). Backup so na mesma VPS nao protege contra perda da maquina.

Restaurar backup: `gunzip -c db-XXXX.sql.gz | docker compose exec -T db psql -U sindico -d sindico`.

## Atualizar versao

```bash
git pull && docker compose up -d --build
```
