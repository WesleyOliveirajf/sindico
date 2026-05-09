#!/usr/bin/env bash
# Aplica src/main/resources/db/migration no Postgres do Supabase via Flyway.
# Pré-requisito: no painel do projeto, copiar senha do usuário postgres (Database settings).
#
# Uso:
#   cp .env.example .env.supabase  # ajuste valores; ou exporte as variáveis abaixo
#   ./scripts/migrate-supabase.sh
#
# Variáveis:
#   FLYWAY_URL      JDBC direto, ex.: jdbc:postgresql://db.qxxqkkotrxkqzyupibkl.supabase.co:5432/postgres?sslmode=require
#   FLYWAY_USER     padrão postgres
#   FLYWAY_PASSWORD senha do banco no Supabase
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ -f .env.supabase ]]; then
  set -a
  # shellcheck source=/dev/null
  source .env.supabase
  set +a
fi

: "${FLYWAY_URL:?Defina FLYWAY_URL (JDBC da conexão direta Supabase, porta 5432)}"
: "${FLYWAY_PASSWORD:?Defina FLYWAY_PASSWORD}"
FLYWAY_USER="${FLYWAY_USER:-postgres}"

exec mvn -q flyway:migrate \
  -Dflyway.url="$FLYWAY_URL" \
  -Dflyway.user="$FLYWAY_USER" \
  -Dflyway.password="$FLYWAY_PASSWORD"
