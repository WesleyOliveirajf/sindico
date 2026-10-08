# Deploy

Visão geral da produção, variáveis, rate limit e rollback. Passo a passo da VPS em
[`deploy/vps/README.md`](../deploy/vps/README.md).

## Arquitetura

```text
Navegador
   │  https://sindico-seven.vercel.app (SPA React)
   ▼
Vercel ── Edge Middleware (frontend/middleware.js) ──► /api/*
                                                          │ + X-Sindico-Client-Ip
                                                          │ + X-Sindico-Proxy-Secret
                                                          ▼
VPS Ubuntu: Traefik (TLS) ─► container Spring Boot (:8080)  https://app.analisandoia.com.br
                                                          │
                                                          ▼
                                            Supabase (PostgreSQL; Storage opcional)
```

- **Front:** Vercel, build `npm run build` (`frontend/vercel.json`).
- **Proxy de API:** `frontend/middleware.js` intercepta `/api/*`, remove `Origin`/`Host`/`Referer`
  (o Spring não aciona CORS) e encaminha para a URL fixa `BACKEND_URL` do arquivo. O rewrite de
  `vercel.json` é só o fallback.
- **Back-end:** `deploy/vps/docker-compose.yml` (perfil `supabase`), atrás do Traefik existente na
  rede `n8n_default`. Healthcheck: `GET /actuator/health`.
- **Banco:** Supabase, projeto `gbzmribcjrgxvzbibcqp`, conexão direta (porta 5432; a VPS tem IPv6).
  O Flyway aplica as migrações no start (baseline em V10).
- **Anexos:** volume `uploads` da VPS (`APP_STORAGE_PROVIDER=local`) ou Supabase Storage.
- **CI:** `.github/workflows/ci.yml` (build + testes do back-end; lint + build do front-end).

## Variáveis de ambiente

| Onde | Variável | Observação |
|---|---|---|
| VPS | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Obrigatórias. `DB_POOL_SIZE` (padrão 5) |
| VPS | `APP_JWT_SECRET` | Obrigatória; `openssl rand -base64 48` |
| VPS | `APP_CORS_ORIGINS`, `APP_PUBLIC_BASE_URL` | Origens do front, separadas por vírgula |
| VPS | `APP_TRUSTED_PROXY_SECRET` | Igual a `PROXY_SHARED_SECRET` do Vercel; `openssl rand -hex 32` |
| VPS | `APP_TRUSTED_PROXY_SECRET_PREVIOUS` | Opcional; segredo anterior durante rotação sem downtime ([guia](../deploy/vps/ROTACAO_SEGREDO_PROXY.md)) |
| VPS | `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` | Opcionais (admin inicial) |
| VPS | `APP_STORAGE_PROVIDER`, `SUPABASE_URL`, `SUPABASE_SERVICE_KEY`, `SUPABASE_STORAGE_BUCKET` | Chaves do Supabase só com provider `supabase` |
| VPS | `SERVER_FORWARD_HEADERS_STRATEGY=native` | Já definida no compose (IP/scheme atrás do Traefik) |
| Vercel | `PROXY_SHARED_SECRET` | Igual a `APP_TRUSTED_PROXY_SECRET` |

Modelo da VPS: `deploy/vps/env.example`. Nunca commite o `.env`.

## Rate limit de autenticação

Implementado em `AuthRateLimitFilter`: janela fixa por IP, em memória (reinicia com o container e
não é compartilhado entre instâncias). Só vale para requisições `POST`.

| Faixa | Rotas | Limite padrão | Propriedades |
|---|---|---|---|
| login | `/api/auth/login`, `/api/auth/google`, `/login` | 10 por 60 s | `app.security.rate-limit.login.max-requests` / `.window-seconds` |
| conta | `/api/auth/register`, `/cadastro`, `/esqueci-senha`, `/redefinir-senha` | 5 por 600 s | `app.security.rate-limit.account.max-requests` / `.window-seconds` |

`app.security.rate-limit.enabled=false` desliga o filtro. Ao exceder, o back-end responde
`429` com o cabeçalho `Retry-After` (segundos até a janela reabrir) e o corpo
`{"error":"Muitas tentativas. Aguarde alguns instantes e tente novamente.","status":429}`.

## Fluxo do IP real do cliente

O back-end enxerga o IP do Vercel, não o do usuário. Para o rate limit (e o IP do aceite LGPD)
usarem o IP real:

1. O middleware lê `x-real-ip` / `x-vercel-forwarded-for` (o Vercel os define; na falta, usa o
   **último** hop de `x-forwarded-for`, nunca o primeiro).
2. Se `PROXY_SHARED_SECRET` existe, envia `X-Sindico-Client-Ip` e `X-Sindico-Proxy-Secret`.
   Cabeçalhos `x-sindico-*` vindos do cliente são descartados.
3. `TrustedProxyClientIpFilter` compara o segredo em tempo constante e, se confere, valida o IP
   (IPv4/IPv6 literal, sem DNS) e passa a devolver esse valor em `getRemoteAddr()`.
4. `AuthRateLimitFilter` usa esse IP como chave.

Sem o segredo no Vercel, o middleware registra `PROXY_SHARED_SECRET ausente em producao` nos logs
e **todos os usuários compartilham o mesmo bucket** (um atacante bloqueia o login de todos).
Sem o segredo na VPS, o filtro fica inativo e o cabeçalho é ignorado.

Validar: defina o segredo nos dois lados, faça redeploy e confira que 11 logins seguidos de um IP
retornam `429` sem afetar outro IP.

## Atualizar

```bash
cd /opt/sindico && git pull && cd deploy/vps && docker compose up -d --build
```

O front atualiza sozinho no push para a `main` (Vercel). Migrações Flyway rodam no start do
container; a última é V19 e a próxima é V20.

## Rollback

- **Back-end:** na VPS, `git log --oneline` para achar o commit estável, `git checkout <sha>` e
  `docker compose up -d --build`. Volte para a `main` depois com `git checkout main`.
- **Banco:** o Flyway não desfaz migrações. Se uma migração nova for o problema, escreva uma
  `V{n+1}` corretiva; nunca edite uma já aplicada. Restaure o Supabase pelo backup do dashboard
  apenas em último caso.
- **Front:** no painel do Vercel, promova um deployment anterior (Promote to Production).
- **Anexos:** restauração do volume em `deploy/vps/README.md` (seção Backup dos anexos).
- **Proxy/segredo:** se o rate limit bloquear usuários legítimos, defina
  `APP_SECURITY_RATE_LIMIT_ENABLED=false` (adicione a variável em `environment:` do
  `docker-compose.yml`, que não repassa variáveis fora da lista) e rode
  `docker compose up -d` até corrigir a causa.
