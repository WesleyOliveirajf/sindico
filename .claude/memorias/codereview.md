# Code review — sindico (2026-10-13)

## Verificação do deploy no Vercel

`https://sindico-seven.vercel.app/` **corresponde a este repositório**.

| Item | Valor |
|---|---|
| Projeto Vercel | `sindico` (`prj_NlNq6YSFBl8z4SKGSHxDBNRNlqfh`) |
| Domínios | `sindico-seven.vercel.app`, `www.wesleyoliveira.tech` (`wesleyoliveira.tech` redireciona para o `www`) |
| Repositório Git ligado | `WesleyOliveirajf/sindico` (branch `main`) |
| Último deploy de produção | `READY`, commit `59d6f9a` (merge do PR #11, react-router-dom) |
| Proteção | SSO ativo (`all_except_custom_domains`); sem senha |

Pontos de atenção:
- `framework: null` no projeto: é só o valor guardado na UI. Quem manda no build é o `frontend/vercel.json` (`framework: vite`, `buildCommand: npm run build`, `outputDirectory: dist`). Não é problema. Root Directory e Build Command conferidos por evidência indireta (item 15 do histórico).
- Merges seguidos do Dependabot geram deploys `CANCELED`. É esperado, não é falha.
- O SSO bloqueia o domínio `*.vercel.app` para quem não tem login; o domínio próprio fica aberto.

## Achados do code review (back-end/front-end de auth e upload)

| # | Arquivo | Achado | Estado |
|---|---|---|---|
| 1 | `AuthApiController` | `catch (Exception)` + `Map.of` com mensagem nula gerava 500 | Corrigido (commit `74be065`) |
| 2 | `UploadExceptionHandler` | Chave `message` vs `error` e advice sem seletor | Falso positivo: o Javadoc explica a ausência de seletor e o `api.js:82` lê `message` |
| 3 | `MultipartConfig` | Bean desligava o binding de `spring.servlet.multipart.*` | Corrigido: bean removido, limites no `application.yml` |
| 4 | `AuthService` | Exceção como fluxo de controle e strings de status duplicadas | Corrigido (commit `65baa00`): enum `StatusUsuario`. `CadastroPendenteException` mantida de propósito (o controller já a traduz em 202; um tipo de resultado reescreveria controller e testes sem ganho) |
| 5 | `AuthService` | `findByEmailNormalizado` ignora status; outros fluxos usam lookup só de ativos | Corrigido: renomeado para `findQualquerStatusPorEmailNormalizado` com Javadoc; o comportamento é intencional (login Google precisa reconhecer conta pendente/inativa) |
| 6 | `AuthService` | `cadastrar` + nova consulta por e-mail não é atômico | Corrigido: `cadastrar` retorna o `Usuario`; corrida tratada com `DataIntegrityViolationException` (2 testes novos) |
| 7 | `LoginPage.jsx` | Senha permanece no estado após o cadastro; `setMode` pode limpar `success` | Corrigido: senha e confirmação limpas. `setMode` não limpa `success` (só os botões de alternar limpam) |
| 8 | `api.js` | `loginComGoogle` retorna `{pendente}` como se fosse usuário | Sem alteração: o único chamador (`LoginPage`) já trata `user?.pendente` antes de `onLogin` |

Regra do projeto: no máximo 3 `if` seguidos por solução (ainda não auditada no restante do código).

## Issues e PRs

- **Issues:** as 14 abertas (#12 a #26) são válidas e permanecem abertas. #12 e #13 são P1 de segurança e dependem de ação do dono.
- **PRs fechados:** #44 (duplicado do #42), #3 (Spring Boot 4, migração grande), #31 (Temurin 26 não é LTS).
- **PRs mesclados:** #27, #28, #7, #9, #11.
- **PRs com conflito, rebase pedido ao Dependabot:** #29, #30, #8, #10.
- **PRs mantidos:** #42 (rotação do segredo do proxy) e #4, #5, #6 (jjwt 0.13, mesclar juntos).

## Histórico do que foi feito (ordem cronológica)

1. **Review** (`/code-review medium`) das mudanças locais de auth e upload: 8 achados (tabela acima).
2. **Triagem** de 14 issues e 17 PRs abertos via `gh`. Nenhuma issue fechada.
3. **Fechamento de PRs** com comentário explicando o motivo: #44, #3 e #31.
4. **Correções aplicadas** (achados 1 e 3):
   - `AuthApiController`: catch restrito a `IllegalArgumentException | IllegalStateException`; `erro()` aceita mensagem nula (`"Requisicao invalida"`).
   - `MultipartConfig.java` removido; `application.yml` ganhou `spring.servlet.multipart.max-file-size` (`APP_MAX_FILE_SIZE_BYTES`, padrão 10 MB) e `max-request-size` (`APP_MAX_REQUEST_SIZE_BYTES`, padrão 11 MB).
5. **Commit `74be065`** na `main`, publicado com push (`a5f9c01..74be065`). Incluiu também o trabalho em andamento de login Google com cadastro pendente (`CadastroPendenteException`, `AuthService`, `LoginPage.jsx`, `api.js`, `AuthServiceTest`), porque o controller dependia desses arquivos. `CLAUDE.md` ficou fora do commit.
6. **CI da `main`:** run após o push terminou em `success` (back-end `mvn verify` e front-end lint + build).
7. **Merges do Dependabot:** #27, #28, #7, #9 e #11 mesclados (squash). O merge em lote foi barrado uma vez pelo classificador de permissões e só foi feito depois da liberação explícita do usuário.
8. **Rebase:** #29, #30, #8 e #10 tinham conflito; comentei `@dependabot rebase` em cada um. Depois do rebase os 4 ficaram `MERGEABLE`.
9. **Verificação do Vercel:** `sindico-seven.vercel.app` pertence ao projeto `sindico`, ligado a este repositório (seção acima).
10. **Merge dos 4 PRs rebaseados** (CI verde em todos, mesclados um por vez): #29 (`setup-java` 6), #30 (`setup-node` 7), #8 (`vite` 8.3.4), #10 (`eslint-plugin-react-refresh` 0.5.7). CI da `main` após os merges: `success`; o run do #8 apareceu `cancelled` só por ser substituído pelo seguinte.
11. **jjwt 0.12.6 → 0.13.0:** os PRs #4, #5 e #6 estavam sem CI e eram conflitantes entre si. Criei um PR único, #45 (branch `deps/jjwt-0.13.0`, só o `pom.xml`, 3 versões). CI verde (back-end build + testes, front-end, preview Vercel). Mesclado (squash, commit `654252d`); #4, #5 e #6 fechados com comentário apontando para o #45. Uso no código restrito a `JwtService`.

12. **Achados 4 a 7 tratados** (achado 8 sem alteração): commit `65baa00`, push em `d067a1a..65baa00`. Build pela IDE ok; `AuthServiceTest` 21/21 local (2 testes novos). `CadastroServiceTest` e `CadastroControllerTest` não rodaram local (JVM sem memória, crash dumps `hs_err_pid*.log` apagados da raiz). CI da `main` após o push: **success** (back-end build + testes e front-end).
13. **Auditoria da regra "no máximo 3 `if` seguidos"** em `src/main/java` (159 `if` no total): **0 violações**. Contei `if` irmãos consecutivos no mesmo bloco e cadeias `else if` com mais de 3 ramos. Dois pontos no limite exato de 3, dentro da regra: `AnotacaoService` linhas 73 a 81 (filtros opcionais da `Specification`) e `SecurityConfig` linhas 52 a 62 (validação das variáveis obrigatórias de produção). Nenhuma alteração. Tests e front-end ficaram de fora da auditoria.

14. **Revisão do PR #42** (rotação do segredo do proxy, issue #36): revisão postada como comentário no PR. Veredito: aprovável com ajustes pequenos; a lógica de segurança estava correta (`MessageDigest.isEqual` sem curto-circuito, filtro inativo sem segredo, cooldown de log seguro contra concorrência). Ajustes aplicados em commit `4406cba` na branch do PR: (1) doc sem a promessa de "zero downtime", com aviso de que `docker compose up -d` recria o container; (2) emergência sem sugerir desligar o rate limit de login; (3) Javadoc com HTML válido e sem acentos; (4) `ListAppender` movido para `@BeforeEach`/`@AfterEach`; (5) dois testes novos (segredo válido sem IP ou com IP inválido alerta e mantém `remoteAddr`). `TrustedProxyClientIpFilterTest` 30/30 local. CI do PR verde; mesclado (squash, commit `faf501f`), issue #36 fechada automaticamente. CI da `main` após o merge: **success**. Fica manual: reescrever a descrição do PR (caractere de controle corrompido e resumo do Sourcery repetido); o PR já está mesclado.

15. **Conferência do Root Directory do Vercel:** a API do Vercel não expõe Root Directory nem Build Command (`get_project`), os logs deram 403 (conector sem permissão no escopo `wesley-oliveiras-projects-4feaf766`) e a lista de arquivos do deploy deu 404 (deploy vindo do Git). Conclusão por evidência: **Root Directory = `frontend`**. Motivos: não existe `vercel.json` na raiz do repositório, só `frontend/vercel.json`, e o Vercel só lê esse arquivo no Root Directory; `package.json`, `vite.config.js` e `middleware.js` ficam em `frontend/`; em produção (`www.wesleyoliveira.tech` e `sindico-seven.vercel.app`) a SPA responde 200 com `Content-Language: pt-BR` (cabeçalho definido no `frontend/vercel.json`) e `/api/auth/me` responde 401 em JSON, ou seja, o proxy para a VPS funciona. Confirmação definitiva é manual: Settings > General > Root Directory no painel, ou `vercel project inspect sindico`.

## Decisões e lições

- **Falso positivo do review:** o achado 2 (`UploadExceptionHandler` sem seletor) é intencional. O multipart falha antes de existir handler, então um advice com `annotations = RestController.class` não se aplicaria. Conferir o Javadoc e os chamadores antes de aceitar um achado.
- **Bean `MultipartConfigElement` desliga o `MultipartAutoConfiguration`.** Prefira as propriedades `spring.servlet.multipart.*`.
- **Commit parcial pode quebrar o build:** o controller já dependia de arquivos não versionados, então o commit levou a feature inteira. Antes de commitar, rodar `git diff` e checar dependências entre arquivos.
- **Ambiente sem Maven:** `mvn` e `mvnw` não existem na máquina; a validação dos testes depende do CI do GitHub Actions.
- **Dependabot:** merges seguidos geram conflitos e deploys `CANCELED` no Vercel. Mesclar em lotes pequenos ou pedir `@dependabot rebase`.
- **jjwt:** os PRs #4, #5 e #6 precisam ser mesclados juntos (mesma versão 0.13.0 em api, impl e jackson).
- **Memória da máquina:** rodar vários testes pela IDE seguidos derrubou a JVM (`insufficient memory`) e deixou `hs_err_pid*.log`/`replay_pid*.log` na raiz do projeto. Rodar uma classe por vez e apagar os dumps.
- **Renomear para revelar intenção:** um achado de "inconsistência" era só nome enganoso; renomear o método resolveu sem mudar comportamento.
- **PR de terceiros no próprio repositório:** a revisão virou comentário no PR e os ajustes foram um commit na branch dele, com `git stash` só dos arquivos locais não relacionados (`CLAUDE.md`, `package-lock.json`) para poder trocar de branch. Voltar para a `main` e restaurar o stash ao final.
- **Scripts com aspas e heredoc:** um script Python longo passado por heredoc no shell quebrou por aspas; gravar o script em arquivo (scratchpad) e executar.
- **Conector do Vercel com escopo limitado:** o MCP lê projeto, domínios e deploys, mas não logs nem arquivos de deploy Git (403/404). Para configuração de build, inferir pelo `vercel.json` do repositório e testar a produção com `curl`; confirmar no painel quando precisar de certeza.
- **Ações externas** (fechar PR, mesclar, push) só são feitas depois de confirmação explícita do usuário.

## Pendências

1. **Testar um login real** após o deploy do jjwt 0.13.0: não há teste de integração do fluxo de login, só build e testes unitários.
2. Confirmar manualmente o Root Directory (`frontend`) no painel do Vercel: Settings > General. Pela evidência já está correto; é só confirmação visual.
3. Rotação do segredo do proxy: `APP_TRUSTED_PROXY_SECRET_PREVIOUS` só deve ser preenchida durante uma rotação (procedimento em `deploy/vps/ROTACAO_SEGREDO_PROXY.md`).
4. Issues abertas continuam válidas; #12 e #13 (P1 de segurança) dependem de ação do dono.
5. Não há Maven local (`mvn`/`mvnw` ausentes): testes rodam no CI ou pela IDE (uma classe por vez).
6. `CLAUDE.md` e `frontend/package-lock.json` seguem modificados e fora dos commits.
7. Ajuste de saldo por período (21/10/2026): o que foi feito, a fórmula e o 404 do dashboard estão em `memoria.md` na raiz do repositório. A VPS ainda precisa do deploy para a V21 gravar o ajuste.
