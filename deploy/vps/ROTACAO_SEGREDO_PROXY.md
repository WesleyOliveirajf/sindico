# Procedimento de Rotação do Segredo do Proxy

Este documento descreve o procedimento operacional para rotacionar o segredo compartilhado entre o **Vercel Edge Middleware** e o **back-end na VPS** (`APP_TRUSTED_PROXY_SECRET` / `PROXY_SHARED_SECRET`), sem janela de segredo divergente entre Vercel e VPS (que derrubaria o IP real e o rate limit por usuário) e preservando a integridade das regras de rate limit e auditoria LGPD.

---

## 1. Contexto e Motivação

O front-end React é servido pela borda da Vercel (`sindico-seven.vercel.app`) e encaminha requisições de API para a VPS Ubuntu (`app.analisandoia.com.br`). Para que o back-end Spring Boot conheça o **IP real do cliente** (essencial para rate limit contra ataques de força bruta no login e registro legal de aceite dos termos LGPD):

1. O middleware de borda do Vercel intercepta a requisição e anexa o IP real do usuário no cabeçalho `X-Sindico-Client-Ip`;
2. Para que um atacante não consiga forjar esse cabeçalho diretamente na VPS, o middleware também envia um segredo criptográfico compartilhado em `X-Sindico-Proxy-Secret`;
3. O filtro `TrustedProxyClientIpFilter` valida o segredo. Se correto, sobrescreve `request.getRemoteAddr()`.

Se o segredo for alterado em um sistema sem sincronização com o outro:
- As requisições deixam de ser autenticadas pelo filtro;
- O back-end passa a enxergar o IP do nó da Vercel para todas as requisições;
- Todos os usuários caem no mesmo bucket compartilhado de rate limit (um usuário ou atacante bloqueia o login de todos);
- O filtro emite alertas em log com limitação de frequência para requisições em `/api/auth/*`.

Para evitar qualquer impacto durante a troca de chaves (periódica ou preventiva), o back-end aceita simultaneamente **dois segredos**:
- `APP_TRUSTED_PROXY_SECRET`: segredo principal/ativo;
- `APP_TRUSTED_PROXY_SECRET_PREVIOUS`: segredo em transição/anterior.

---

## 2. Passo a Passo de Rotação

### Passo 1: Gerar o novo segredo
Gere uma chave segura e de alta entropia (32 bytes hexadecimais):
```bash
openssl rand -hex 32
```
*Exemplo de saída:* uma string hexadecimal de 64 caracteres

---

### Passo 2: Atualizar o back-end na VPS (ativação dupla)
Acesse a VPS e navegue até a pasta de deploy:
```bash
cd /opt/sindico/deploy/vps
```

Abra o arquivo `.env`:
```bash
nano .env
```

Atualize as variáveis:
1. Mova o segredo atual de `APP_TRUSTED_PROXY_SECRET` para `APP_TRUSTED_PROXY_SECRET_PREVIOUS`;
2. Defina o novo segredo gerado no Passo 1 em `APP_TRUSTED_PROXY_SECRET`.

*Exemplo de configuração transitória:*
```env
APP_TRUSTED_PROXY_SECRET=<novo_segredo_gerado_no_passo_1>
APP_TRUSTED_PROXY_SECRET_PREVIOUS=<segredo_antigo_que_estava_em_uso>
```

Reinicie a aplicação para carregar as novas variáveis:
```bash
docker compose up -d
```

> **Atenção:** `docker compose up -d` recria o container e a API fica indisponível por alguns segundos durante o reinício. Faça em horário de baixo uso.
>
> **Importante:** A partir deste momento, o back-end aceita tanto o segredo novo quanto o antigo. Requisições em trânsito com o segredo antigo continuam funcionando sem qualquer interrupção.

---

### Passo 3: Atualizar o front-end no Vercel
1. Acesse o painel da Vercel: [vercel.com](https://vercel.com);
2. Selecione o projeto `sindico`;
3. Vá em **Settings** > **Environment Variables**;
4. Localize a variável `PROXY_SHARED_SECRET`;
5. Edite o valor substituindo pelo **novo segredo** (o mesmo valor de `APP_TRUSTED_PROXY_SECRET`);
6. Salve a alteração;
7. Vá na aba **Deployments** e faça um **Redeploy** do deployment de produção ativo (ou dispare um push para a branch `main`).

---

### Passo 4: Monitorar logs e validar tráfego
Após a conclusão do redeploy na Vercel:
1. Faça um teste de login na aplicação pelo navegador (`https://sindico-seven.vercel.app/login`);
2. Na VPS, acompanhe os logs da aplicação:
```bash
docker compose logs -f --tail=100 app
```
3. Confirme que **não** estão surgindo mensagens de aviso do filtro:
`Requisicao para /api/auth/... recebida sem cabecalho de proxy confiavel valido`

Se aparecer algum aviso:
- Verifique se o valor de `PROXY_SHARED_SECRET` na Vercel está idêntico ao `APP_TRUSTED_PROXY_SECRET` na VPS;
- Verifique se o redeploy na Vercel já foi concluído com sucesso.

---

### Passo 5: Remover o segredo anterior da VPS (finalização)
Após confirmar que o tráfego está 100% migrado para o novo segredo:
1. Na VPS, edite novamente o `.env`:
```bash
nano /opt/sindico/deploy/vps/.env
```
2. Deixe a variável `APP_TRUSTED_PROXY_SECRET_PREVIOUS` vazia:
```env
APP_TRUSTED_PROXY_SECRET_PREVIOUS=
```
3. Reinicie a aplicação:
```bash
docker compose up -d
```

A rotação está concluída. Não houve janela de segredo divergente; cada `docker compose up -d` recria o container e causa alguns segundos de indisponibilidade da API durante o reinício.

---

## 3. Procedimento Emergencial (Vazamento de Segredo)

Caso o segredo atual tenha sido comprometido:
1. **Não** utilize `APP_TRUSTED_PROXY_SECRET_PREVIOUS`.
2. Gere um novo segredo: `openssl rand -hex 32`.
3. Defina o novo segredo imediatamente em `APP_TRUSTED_PROXY_SECRET` na VPS e rode `docker compose up -d`.
4. Atualize imediatamente `PROXY_SHARED_SECRET` na Vercel e dispare redeploy instantâneo.
5. Durante a janela entre a troca na VPS e o redeploy do Vercel, todos os usuários caem no mesmo bucket de rate limit. **Não desligue o rate limit de login**: isso abre brute-force. Prefira encurtar a janela (deixe o redeploy do Vercel pronto antes de reiniciar a VPS).
6. Só em último caso, se o bucket compartilhado estiver bloqueando usuários legítimos, siga a orientação do Rollback em [`docs/DEPLOY.md`](../../docs/DEPLOY.md) (`APP_SECURITY_RATE_LIMIT_ENABLED=false` exige adicionar a variável em `environment:` do `docker-compose.yml`) e reative o rate limit assim que o redeploy terminar.
