# Documentação Viva do Sistema LiveSindIA

Última atualização: 10/05/2026  
Objetivo do arquivo: servir como ponto único para entender onde o projeto está, o que já existe, como o sistema funciona, quais riscos existem e quais próximos passos fazem sentido.

---

## 1. Visão Geral

O **LiveSindIA** é um sistema para centralizar o histórico operacional e administrativo de condomínios. A aplicação ajuda o síndico a registrar, consultar e acompanhar:

- manutenções preventivas e corretivas;
- reuniões ordinárias, extraordinárias e assembleias;
- participantes de reunião;
- anexos e evidências;
- prestadores de serviço;
- moradores e unidades;
- compromissos da agenda;
- anotações gerais;
- dados básicos do condomínio;
- pendências e acompanhamentos futuros.

O produto está caminhando para um **MVP funcional** com foco em rastreabilidade, histórico e consulta rápida. A arquitetura atual é um **monolito modular Spring Boot**, com uma SPA React consumindo APIs REST.

---

## 2. Estado Atual do Projeto

### Já implementado

- Backend Java 21 com Spring Boot 3.5.
- Autenticação via Spring Security.
- Login web tradicional com sessão.
- Login API com JWT Bearer para SPA React.
- Cadastro de usuário + condomínio.
- Associação usuário-condomínio.
- Seleção de condomínio para usuários com mais de um condomínio.
- Isolamento multi-tenant por `condominioId`.
- Dashboard Thymeleaf com métricas simples.
- CRUD de compromissos.
- CRUD de anotações.
- CRUD de prestadores de serviço.
- CRUD de unidades e moradores.
- CRUD de manutenções via API.
- CRUD de reuniões via API.
- Participantes de reunião.
- Upload, listagem e download de anexos para manutenção e reunião.
- Reset de senha.
- Perfil do usuário com atualização de dados e troca de senha.
- Migrations Flyway.
- Frontend React/Vite com páginas principais.
- Dockerfile multi-stage para build e runtime.
- Testes Web MVC e testes básicos de contexto.

### Em andamento ou parcialmente implementado

- Frontend React novo para manutenção e reunião.
- Upload de anexos integrado às telas de manutenção/reunião.
- Segurança de produção ainda precisa hardening.
- Permissões por papel ainda não foram refinadas.
- Pendências ainda existem no modelo inicial, mas não têm módulo completo no app.
- Timeline consolidada ainda não existe.
- Notificações ainda não existem.
- Integração real de e-mail ainda não existe; serviço atual é no-op.

### Ainda não implementado

- Convite de usuários.
- Controle granular por perfil (`SINDICO`, `MORADOR`, `ADMIN`, etc.).
- Auditoria de alterações.
- Busca global.
- Filtros avançados em manutenção e reunião.
- Exportação PDF.
- Integração real com storage externo, como S3.
- Observabilidade estruturada.
- Rate limiting.
- Pipeline CI/CD com SAST/SCA/container scan.
- Testes E2E.

---

## 3. Stack Técnica

### Backend

- Java 21.
- Spring Boot 3.5.0.
- Spring Web.
- Spring MVC.
- Spring Data JPA.
- Hibernate.
- Spring Security.
- Spring Validation.
- Flyway.
- PostgreSQL.
- JJWT 0.12.6 para JWT.
- Thymeleaf para telas server-side.

### Frontend

- React 19.
- Vite 8.
- JavaScript ESM.
- ESLint.
- Fetch API com wrapper próprio em `frontend/src/api.js`.

### Banco

- PostgreSQL em Supabase/VPS/prod.
- Flyway para versionamento de schema.
- `ddl-auto: validate`, então Hibernate valida o schema e não cria tabelas automaticamente.

### Deploy

- Dockerfile multi-stage:
  - build com `maven:3.9-eclipse-temurin-21-alpine`;
  - runtime com `eclipse-temurin:21-jre-alpine`;
  - jar final exposto na porta `8080`.

---

## 4. Arquitetura Atual

### Modelo geral

```text
React SPA (Vite)
  |
  | HTTP /api/* com Bearer JWT
  v
Spring Boot Monolito Modular
  |
  | Spring Data JPA
  v
PostgreSQL

Thymeleaf Web MVC
  |
  | Sessão JSESSIONID
  v
Spring Boot Monolito Modular
```

O sistema suporta dois estilos de UI:

- **SPA React**: usada para páginas modernas e chamadas `/api/**`.
- **Thymeleaf server-side**: usado em telas já existentes, como login, dashboard, cadastro, perfil, prestadores, moradores e anotações.

### Organização por camadas

Cada módulo tende a seguir o padrão:

- `Controller` ou `ApiController`: entrada HTTP.
- `Service`: regra de negócio e controle de tenant.
- `Repository`: acesso ao banco.
- `Entity`: entidade JPA.
- `Request/Form`: entrada de dados.
- `Response`: saída de API.
- `Enum`: tipos/status.

Exemplo:

```text
br.com.sindico.app.manutencao
  Manutencao.java
  ManutencaoApiController.java
  ManutencaoRepository.java
  ManutencaoRequest.java
  ManutencaoResponse.java
  ManutencaoService.java
  ManutencaoStatus.java
  ManutencaoTipo.java
```

---

## 5. Estrutura de Diretórios Importante

```text
.
├── src/main/java/br/com/sindico/app
│   ├── anexo
│   ├── anotacao
│   ├── auth
│   ├── cadastro
│   ├── compromisso
│   ├── condominio
│   ├── config
│   ├── dashboard
│   ├── email
│   ├── login
│   ├── manutencao
│   ├── morador
│   ├── prestador
│   ├── reuniao
│   ├── security
│   ├── senha
│   ├── usuario
│   └── web
├── src/main/resources
│   ├── application.yml
│   ├── db/migration
│   ├── static
│   └── templates
├── src/test/java/br/com/sindico/app
├── frontend
│   ├── src
│   ├── package.json
│   └── vite.config.js
├── docs
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 6. Módulos do Backend

### 6.1 Autenticação API

Pacote: `br.com.sindico.app.auth`

Arquivo principal:

- `AuthApiController`

Rotas:

| Método | Rota | Função |
|---|---|---|
| `POST` | `/api/auth/login` | Autentica com e-mail/senha e retorna JWT |
| `POST` | `/api/auth/register` | Cadastra usuário e condomínio |
| `GET` | `/api/auth/me` | Retorna usuário autenticado |
| `POST` | `/api/auth/logout` | Retorna sucesso e frontend apaga token |

Fluxo:

1. React envia e-mail e senha.
2. Backend usa `AuthenticationManager`.
3. Se credenciais são válidas, `JwtService` gera token.
4. Frontend guarda token em `localStorage`.
5. Próximas requisições enviam `Authorization: Bearer <token>`.

Observação importante:

- O logout API atual não revoga token no backend. Ele apenas responde sucesso; o frontend remove o token localmente.

### 6.2 Segurança

Pacote: `br.com.sindico.app.security`

Arquivos principais:

- `JwtService`
- `JwtAuthenticationFilter`
- `UsuarioTenantUserDetailsService`
- `UsuarioTenantPrincipal`
- `TenantAccessor`
- `SecurityTenantAccessor`
- `SindicoLoginSuccessHandler`
- `TenantSession`

Responsabilidades:

- Validar credenciais.
- Carregar usuário ativo por e-mail.
- Carregar vínculos usuário-condomínio.
- Definir condomínio padrão.
- Gerar e validar JWT.
- Injetar autenticação no `SecurityContext`.
- Descobrir o condomínio atual.
- Controlar seleção de condomínio na sessão web.

Como o tenant é resolvido:

1. `SecurityTenantAccessor` pega usuário autenticado.
2. Se existe condomínio selecionado na sessão e ele pertence ao usuário, usa esse.
3. Caso contrário, usa o condomínio padrão do principal.

Essa regra evita IDOR entre condomínios quando os services usam `tenantAccessor.condominioAtual()`.

### 6.3 Configuração de Segurança

Pacote: `br.com.sindico.app.config`

Arquivo:

- `SecurityConfig`

Pontos importantes:

- CORS configurado para `/api/**`.
- CSRF ignorado para `/api/**`.
- `/api/**` exige autenticação, exceto rotas públicas de auth.
- Login web usa `/login`.
- Sessão web limita uma sessão por usuário.
- Headers de segurança:
  - CSP básica;
  - `frame-ancestors 'none'`;
  - `X-Frame-Options: DENY`;
  - `Content-Type-Options`;
  - Referrer Policy.

### 6.4 Cadastro

Pacote: `br.com.sindico.app.cadastro`

Arquivos principais:

- `CadastroController`
- `CadastroService`
- `CadastroForm`

Responsabilidade:

- Criar usuário.
- Criar condomínio.
- Vincular usuário ao condomínio como `SINDICO`.
- Validar senha mínima:
  - mínimo 8 caracteres;
  - letras e números;
  - confirmação igual.
- Criptografar senha com BCrypt.

### 6.5 Condomínio

Pacote: `br.com.sindico.app.condominio`

Arquivos principais:

- `CondominioController`
- `CondominioSelecaoController`
- `CondominioService`
- `CondominioRepository`

Responsabilidade:

- Exibir e atualizar dados do condomínio.
- Permitir seleção de condomínio quando usuário possui mais de um vínculo.

### 6.6 Dashboard

Pacote: `br.com.sindico.app.dashboard`

Arquivo principal:

- `DashboardController`

Rota:

| Método | Rota | Função |
|---|---|---|
| `GET` | `/` | Dashboard Thymeleaf |
| `POST` | `/compromissos` | Cria compromisso pelo formulário web |

Dados exibidos:

- total de manutenções agendadas;
- total de reuniões agendadas;
- total de pendências;
- nome do condomínio;
- próximos compromissos.

### 6.7 Compromissos

Pacote:

- `br.com.sindico.app.compromisso`
- `br.com.sindico.app.compromisso.api`

Arquivos principais:

- `CompromissoService`
- `CompromissoRepository`
- `CompromissoApiController`
- `CompromissoRequest`
- `CompromissoResponse`
- `CompromissoTipo`
- `CompromissoStatus`

Rotas API:

| Método | Rota | Função |
|---|---|---|
| `GET` | `/api/compromissos` | Lista próximos compromissos |
| `POST` | `/api/compromissos` | Cria compromisso |

Regras:

- Compromisso sempre pertence ao condomínio atual.
- Data final deve ser maior que data inicial.
- Novo compromisso nasce com status `AGENDADO`.

### 6.8 Anotações

Pacote: `br.com.sindico.app.anotacao`

Arquivos principais:

- `AnotacaoController`
- `AnotacaoApiController`
- `AnotacaoService`
- `AnotacaoRepository`
- `Anotacao`
- `AnotacaoRequest`
- `AnotacaoResponse`
- `AnotacaoImportancia`

Rotas web:

| Método | Rota | Função |
|---|---|---|
| `GET` | `/anotacoes` | Tela Thymeleaf |
| `POST` | `/anotacoes` | Cria anotação |

Rotas API:

| Método | Rota | Função |
|---|---|---|
| `GET` | `/api/anotacoes` | Lista com filtros |
| `POST` | `/api/anotacoes` | Cria anotação |
| `PUT` | `/api/anotacoes/{id}` | Atualiza anotação |
| `DELETE` | `/api/anotacoes/{id}` | Remove anotação |

Filtros API:

- `texto`
- `dataInicio`
- `dataFim`

Regras:

- Consulta sempre filtra por condomínio atual.
- Atualização e exclusão validam que a anotação pertence ao condomínio atual.
- Se `dataFim` for antes de `dataInicio`, retorna erro de negócio.

### 6.9 Prestadores de Serviço

Pacote: `br.com.sindico.app.prestador`

Arquivos principais:

- `PrestadorServicoController`
- `PrestadorServicoApiController`
- `PrestadorServicoService`
- `PrestadorServicoRepository`
- `PrestadorServico`
- `PrestadorServicoRequest`
- `PrestadorServicoResponse`

Rotas web:

| Método | Rota | Função |
|---|---|---|
| `GET` | `/prestadores` | Lista prestadores |
| `POST` | `/prestadores` | Cria prestador |
| `POST` | `/prestadores/{prestadorId}` | Atualiza prestador |
| `POST` | `/prestadores/{prestadorId}/inativar` | Inativa prestador |

Rotas API:

| Método | Rota | Função |
|---|---|---|
| `GET` | `/api/prestadores` | Lista prestadores ativos |
| `POST` | `/api/prestadores` | Cria prestador |
| `PUT` | `/api/prestadores/{prestadorId}` | Atualiza prestador |
| `POST` | `/api/prestadores/{prestadorId}/inativar` | Inativa prestador |

Regras:

- Prestadores são isolados por condomínio.
- Exclusão é lógica via `ativo = false`.

### 6.10 Moradores e Unidades

Pacote: `br.com.sindico.app.morador`

Arquivos principais:

- `MoradorController`
- `MoradorApiController`
- `MoradorGestaoService`
- `MoradorRepository`
- `UnidadeRepository`
- `Morador`
- `Unidade`
- `MoradorRequest`
- `UnidadeRequest`
- `MoradorResponse`
- `UnidadeResponse`
- `MoradorPapel`

Rotas web:

| Método | Rota | Função |
|---|---|---|
| `GET` | `/moradores` | Tela de moradores |
| `POST` | `/moradores/unidades` | Cria unidade |
| `POST` | `/moradores` | Cria morador |

Rotas API:

| Método | Rota | Função |
|---|---|---|
| `GET` | `/api/unidades` | Lista unidades |
| `POST` | `/api/unidades` | Cria unidade |
| `GET` | `/api/moradores` | Lista moradores ativos |
| `POST` | `/api/moradores` | Cria morador |
| `PUT` | `/api/moradores/{id}` | Atualiza morador |
| `POST` | `/api/moradores/{id}/inativar` | Inativa morador |

Regras:

- Unidade sempre pertence ao condomínio atual.
- Morador só pode ser vinculado a unidade do condomínio atual.
- Atualização e inativação validam tenant.
- Unidade duplicada no mesmo bloco/número gera erro de negócio.

### 6.11 Manutenções

Pacote: `br.com.sindico.app.manutencao`

Arquivos principais:

- `Manutencao`
- `ManutencaoApiController`
- `ManutencaoService`
- `ManutencaoRepository`
- `ManutencaoRequest`
- `ManutencaoResponse`
- `ManutencaoStatus`
- `ManutencaoTipo`

Rotas API:

| Método | Rota | Função |
|---|---|---|
| `GET` | `/api/manutencoes` | Lista manutenções do condomínio atual |
| `POST` | `/api/manutencoes` | Cria manutenção |
| `PUT` | `/api/manutencoes/{id}` | Atualiza manutenção |
| `DELETE` | `/api/manutencoes/{id}` | Remove manutenção |

Campos principais:

- título;
- descrição;
- tipo;
- categoria;
- local;
- ativo relacionado;
- fornecedor relacionado;
- responsável interno;
- data de ocorrência;
- data de execução;
- custo previsto;
- custo realizado;
- status;
- observações.

Regras:

- Criação define `condominioId` com tenant atual.
- Criação define `criadoPor` com usuário autenticado.
- Atualização e exclusão validam que registro pertence ao condomínio atual.

Ponto de atenção:

- `ativoId` e `fornecedorId` são aceitos no request, mas a validação de pertencimento desses IDs ao condomínio atual deve ser reforçada antes de produção.

### 6.12 Reuniões

Pacote: `br.com.sindico.app.reuniao`

Arquivos principais:

- `Reuniao`
- `ReuniaoApiController`
- `ReuniaoService`
- `ReuniaoRepository`
- `ParticipanteReuniao`
- `ParticipanteReuniaoRepository`
- `ReuniaoRequest`
- `ReuniaoResponse`
- `ReuniaoTipo`

Rotas API:

| Método | Rota | Função |
|---|---|---|
| `GET` | `/api/reunioes` | Lista reuniões do condomínio atual |
| `POST` | `/api/reunioes` | Cria reunião |
| `PUT` | `/api/reunioes/{id}` | Atualiza reunião |
| `DELETE` | `/api/reunioes/{id}` | Remove reunião |

Campos principais:

- título;
- tipo;
- data e hora;
- local;
- link;
- pauta;
- resumo;
- decisões;
- pendências geradas;
- participantes.

Regras:

- Criação define `condominioId` com tenant atual.
- Criação define `criadoPor` com usuário autenticado.
- Atualização e exclusão validam tenant.
- Participantes são substituídos a cada atualização.

Ponto de atenção:

- Listagem atual carrega participantes reunião por reunião. Para volume maior, pode virar N+1; otimizar com query dedicada ou fetch planejado.

### 6.13 Anexos

Pacote: `br.com.sindico.app.anexo`

Arquivos principais:

- `Anexo`
- `AnexoApiController`
- `AnexoService`
- `AnexoRepository`
- `AnexoResponse`

Rotas API:

| Método | Rota | Função |
|---|---|---|
| `GET` | `/api/anexos?entidadeTipo=...&entidadeId=...` | Lista anexos |
| `POST` | `/api/anexos` | Upload de arquivo |
| `GET` | `/api/anexos/{anexoId}/download` | Download |

Entidades permitidas:

- `MANUTENCAO`
- `REUNIAO`

Tipos de arquivo permitidos:

- PDF;
- JPEG;
- PNG;
- WebP;
- DOC;
- DOCX.

Regras de segurança já presentes:

- Valida tipo de entidade.
- Valida se entidade pertence ao condomínio atual.
- Valida tamanho máximo.
- Valida content-type.
- Sanitiza nome original do arquivo.
- Usa nome armazenado com UUID.
- Valida path para evitar escrita/leitura fora do diretório de upload.
- Download valida `anexoId` + `condominioId`.

Pontos de atenção:

- `Content-Type` enviado pelo cliente não é prova forte do tipo real do arquivo.
- Para produção, ideal validar magic bytes/assinatura do arquivo.
- Storage local funciona para MVP; produção deve migrar para S3 ou compatível.

### 6.14 Senha e Perfil

Pacote: `br.com.sindico.app.senha`

Arquivos principais:

- `SenhaResetController`
- `SenhaResetService`
- `PerfilController`
- `PerfilService`

Rotas:

| Método | Rota | Função |
|---|---|---|
| `GET` | `/esqueci-senha` | Form de recuperação |
| `POST` | `/esqueci-senha` | Solicita reset |
| `GET` | `/redefinir-senha` | Form de nova senha |
| `POST` | `/redefinir-senha` | Redefine senha |
| `GET` | `/perfil` | Tela de perfil |
| `POST` | `/perfil/dados` | Atualiza dados |
| `POST` | `/perfil/senha` | Troca senha |

Regras:

- Solicitação de reset não revela se e-mail existe.
- Token tem 256 bits e expira em 60 minutos.
- Senha nova exige mínimo 8 caracteres, letra e número.
- Senhas são armazenadas com BCrypt.

Pontos de atenção:

- Implementação de e-mail atual é `NoOpEmailService`, que registra o link no log.
- Token de reset é armazenado em texto no banco.
- Base URL do link vem da requisição HTTP.

### 6.15 E-mail

Pacote: `br.com.sindico.app.email`

Arquivos:

- `EmailService`
- `NoOpEmailService`

Estado atual:

- Serviço de e-mail é fake/no-op.
- Loga link de reset.

Próximo passo técnico:

- Implementar serviço real com SMTP, SES, SendGrid, Mailgun ou outro provedor.
- Remover token dos logs.

---

## 7. Frontend React

Diretório: `frontend`

### Ponto de entrada

- `frontend/src/App.jsx`

Páginas:

- `CompromissosPage`
- `ManutencoesPage`
- `ReunioesPage`
- `AnotacoesPage`
- `MoradoresPage`
- `PrestadoresPage`
- `LoginPage`

### Navegação

O estado de página atual fica em `sessionStorage` usando a chave `appPage`.

Páginas disponíveis no menu:

- Compromissos;
- Manutenções;
- Reuniões;
- Anotações;
- Moradores;
- Prestadores.

### Autenticação no frontend

Arquivo: `frontend/src/api.js`

Regras:

- Token JWT fica em `localStorage`, chave `authToken`.
- Todas as chamadas passam por `apiFetch`.
- `apiFetch` adiciona `Content-Type: application/json`.
- Se existir token, adiciona header `Authorization: Bearer ...`.
- Base URL vem de `VITE_API_BASE_URL`; se vazio, usa mesma origem.

Funções principais:

- `getMe()`
- `login(email, senha)`
- `register(payload)`
- `logout()`
- `apiFetch(path, options)`
- `parseJson(response)`

Ponto de atenção:

- JWT em `localStorage` é simples para MVP, mas aumenta impacto de XSS. Para produção, avaliar cookie HttpOnly ou access token curto + hardening forte de CSP.

---

## 8. Banco de Dados

### Controle de schema

Flyway controla migrations em:

```text
src/main/resources/db/migration
```

Configuração principal:

- `spring.jpa.hibernate.ddl-auto: validate`
- `spring.flyway.enabled: true`
- `spring.flyway.locations: classpath:db/migration`

### Migrations relevantes

| Migration | Função |
|---|---|
| `V1__init.sql` | Cria modelo inicial: condomínios, usuários, fornecedores, ativos, manutenções, reuniões, participantes, pendências, anexos e índices |
| `V2__compromissos_google_calendar.sql` | Evolução de compromissos/agenda |
| `V5__anotacoes.sql` | Cria condomínio piloto e tabela de anotações |
| `V7__prestadores_servico.sql` | Cria tabela de prestadores de serviço |
| `V9__usuarios_login_compromissos_tenant.sql` | Adiciona senha hash, tenant em compromissos e usuário demo |
| `V11__mvp_manutencoes_reunioes_campos.sql` | Adiciona campos atuais de manutenção e reunião |

### Entidades principais

- `condominios`
- `usuarios`
- `usuarios_condominios`
- `fornecedores`
- `ativos`
- `manutencoes`
- `reunioes`
- `participantes_reuniao`
- `pendencias`
- `anexos`
- `anotacoes`
- `prestadores_servico`
- `compromissos`
- `senha_reset_tokens`
- `moradores`
- `unidades`

### Modelo multi-tenant

O tenant principal é o `condominio_id`.

Tabelas de negócio devem sempre ter `condominio_id` ou relacionamento que chegue ao condomínio.

Regra prática:

- listagens sempre filtram por `condominioId`;
- criação sempre seta `condominioId` pelo `TenantAccessor`;
- update/delete sempre validam pertencimento ao condomínio atual;
- nunca aceitar `condominioId` do frontend como fonte de verdade.

---

## 9. Configuração de Ambientes

Arquivo: `src/main/resources/application.yml`

### Perfil padrão

```yaml
spring:
  profiles:
    default: supabase
```

Ou seja, se nenhum perfil for informado, a aplicação tenta subir com configuração Supabase.

### Variáveis importantes

| Variável | Uso |
|---|---|
| `DB_URL` | URL JDBC do PostgreSQL |
| `DB_USERNAME` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |
| `APP_CORS_ORIGINS` | Origens permitidas no CORS |
| `APP_JWT_SECRET` | Segredo de assinatura JWT |
| `APP_JWT_EXPIRATION_MINUTES` | Expiração do JWT |
| `APP_MAX_FILE_SIZE_BYTES` | Tamanho máximo de upload |
| `APP_FLYWAY_REPAIR` | Repair Flyway em dev |
| `VITE_API_BASE_URL` | Base URL do backend no frontend |

### Supabase

Perfil `supabase` usa:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- baseline Flyway em versão 10;
- pool Hikari pequeno.

Observação:

- Comentários do arquivo recomendam conexão direta Supabase porta 5432, não pooler transacional 6543 para Flyway/Hibernate.

### Produção

Perfil `prod`:

- Thymeleaf cache ligado.
- Hikari com pool maior.
- `sslMode: verify-full` no datasource.
- CORS sem origem padrão.

Ponto crítico:

- `APP_JWT_SECRET` ainda possui fallback dev. Em produção isso deve virar obrigatório.

---

## 10. Segurança

### Pontos positivos já existentes

- Senhas com BCrypt.
- APIs protegidas por Spring Security.
- JWT validado por filtro próprio.
- Multi-tenant centralizado por `TenantAccessor`.
- Atualizações e exclusões relevantes validam tenant.
- Reset de senha não permite enumeração por mensagem.
- Headers básicos de segurança configurados.
- Upload valida entidade, tenant, tamanho e content-type.
- Paths de upload/download são normalizados e validados.
- JPQL parametrizado em consultas customizadas.

### Riscos atuais

| Severidade | Risco | Impacto |
|---|---|---|
| Crítico | `APP_JWT_SECRET` tem fallback fixo | Token pode ser forjado se deploy esquecer segredo |
| Crítico | `NoOpEmailService` loga link de reset | Quem acessa logs pode tomar conta da conta |
| Alto | Link de reset usa base URL da request | Risco com proxy/Host header mal configurado |
| Alto | Sem autorização por papel | Todo autenticado pode usar APIs do condomínio |
| Médio | Sem rate limiting | Brute force, credential stuffing e abuso de reset/cadastro |
| Médio | Logout JWT não revoga token | Token continua válido até expirar |
| Médio | Token de reset salvo em texto | Vazamento de banco permite uso direto |
| Médio | Upload confia no `Content-Type` do cliente | Pode aceitar arquivo mascarado |
| Baixo | Docker roda sem usuário não-root explícito | Hardening de container incompleto |

### Recomendações de segurança

Prioridade alta:

1. Remover fallback de `APP_JWT_SECRET` em produção.
2. Substituir `NoOpEmailService` por serviço real.
3. Nunca logar token de reset.
4. Usar `APP_PUBLIC_BASE_URL` para links de reset.
5. Adicionar rate limiting em login, cadastro e reset.
6. Criar autorização por papel.
7. Salvar hash do token de reset, não token puro.

Prioridade média:

1. Validar magic bytes em upload.
2. Reduzir duração do JWT ou implementar refresh token.
3. Implementar revogação de token ou `tokenVersion`.
4. Melhorar CSP para frontend em produção.
5. Rodar container como usuário não-root.

---

## 11. APIs REST Consolidadas

### Autenticação

| Método | Rota |
|---|---|
| `POST` | `/api/auth/login` |
| `POST` | `/api/auth/register` |
| `GET` | `/api/auth/me` |
| `POST` | `/api/auth/logout` |

### Compromissos

| Método | Rota |
|---|---|
| `GET` | `/api/compromissos` |
| `POST` | `/api/compromissos` |

### Anotações

| Método | Rota |
|---|---|
| `GET` | `/api/anotacoes` |
| `POST` | `/api/anotacoes` |
| `PUT` | `/api/anotacoes/{id}` |
| `DELETE` | `/api/anotacoes/{id}` |

### Prestadores

| Método | Rota |
|---|---|
| `GET` | `/api/prestadores` |
| `POST` | `/api/prestadores` |
| `PUT` | `/api/prestadores/{prestadorId}` |
| `POST` | `/api/prestadores/{prestadorId}/inativar` |

### Unidades e Moradores

| Método | Rota |
|---|---|
| `GET` | `/api/unidades` |
| `POST` | `/api/unidades` |
| `GET` | `/api/moradores` |
| `POST` | `/api/moradores` |
| `PUT` | `/api/moradores/{id}` |
| `POST` | `/api/moradores/{id}/inativar` |

### Manutenções

| Método | Rota |
|---|---|
| `GET` | `/api/manutencoes` |
| `POST` | `/api/manutencoes` |
| `PUT` | `/api/manutencoes/{id}` |
| `DELETE` | `/api/manutencoes/{id}` |

### Reuniões

| Método | Rota |
|---|---|
| `GET` | `/api/reunioes` |
| `POST` | `/api/reunioes` |
| `PUT` | `/api/reunioes/{id}` |
| `DELETE` | `/api/reunioes/{id}` |

### Anexos

| Método | Rota |
|---|---|
| `GET` | `/api/anexos` |
| `POST` | `/api/anexos` |
| `GET` | `/api/anexos/{anexoId}/download` |

---

## 12. Telas Web Thymeleaf

| Método | Rota | Tela/Função |
|---|---|---|
| `GET` | `/login` | Login |
| `GET` | `/cadastro` | Cadastro |
| `POST` | `/cadastro` | Criação de conta |
| `GET` | `/` | Dashboard |
| `POST` | `/compromissos` | Criação de compromisso |
| `GET` | `/condominio` | Dados do condomínio |
| `POST` | `/condominio` | Atualização do condomínio |
| `GET` | `/condominios/selecionar` | Seleção de condomínio |
| `POST` | `/condominios/selecionar` | Aplica condomínio selecionado |
| `GET` | `/anotacoes` | Anotações |
| `POST` | `/anotacoes` | Cria anotação |
| `GET` | `/moradores` | Moradores/unidades |
| `POST` | `/moradores/unidades` | Cria unidade |
| `POST` | `/moradores` | Cria morador |
| `GET` | `/prestadores` | Prestadores |
| `POST` | `/prestadores` | Cria prestador |
| `POST` | `/prestadores/{prestadorId}` | Atualiza prestador |
| `POST` | `/prestadores/{prestadorId}/inativar` | Inativa prestador |
| `GET` | `/perfil` | Perfil |
| `POST` | `/perfil/dados` | Atualiza perfil |
| `POST` | `/perfil/senha` | Troca senha |
| `GET` | `/esqueci-senha` | Recuperação de senha |
| `POST` | `/esqueci-senha` | Solicita reset |
| `GET` | `/redefinir-senha` | Nova senha |
| `POST` | `/redefinir-senha` | Redefine senha |

---

## 13. Testes

Diretório:

```text
src/test/java/br/com/sindico/app
```

Testes existentes:

- `SindicoApplicationTests`
- `CadastroControllerTest`
- `CondominioControllerWebMvcTest`
- `DashboardControllerWebMvcTest`
- `AnotacaoControllerWebMvcTest`
- `AnotacaoApiControllerTest`
- `MoradorControllerWebMvcTest`
- `MoradorApiControllerTest`
- `PrestadorServicoControllerWebMvcTest`
- `SenhaResetControllerTest`
- `PerfilControllerTest`

Cobertura atual:

- Context load.
- Controllers MVC.
- Alguns controllers API.
- Segurança básica via Spring Security Test.

Lacunas:

- Testes para `ManutencaoApiController`.
- Testes para `ReuniaoApiController`.
- Testes para `AnexoApiController`.
- Testes de tenant/IDOR para manutenção, reunião e anexos.
- Testes de upload inválido.
- Testes de JWT.
- Testes de autorização por papel quando papéis forem refinados.
- Testes E2E do frontend.

Comando backend:

```bash
mvn test
```

Comandos frontend:

```bash
cd frontend
npm run lint
npm run build
```

---

## 14. Como Rodar o Projeto

### Backend

Pré-requisitos:

- Java 21.
- Maven.
- PostgreSQL acessível.
- Variáveis de ambiente do banco.

Variáveis mínimas:

```bash
export DB_URL="jdbc:postgresql://host:5432/database?sslmode=require"
export DB_USERNAME="postgres"
export DB_PASSWORD="..."
export APP_JWT_SECRET="segredo-forte-com-mais-de-32-bytes"
export APP_CORS_ORIGINS="http://localhost:5173"
```

Rodar:

```bash
mvn spring-boot:run
```

Compilar:

```bash
mvn compile
```

Empacotar:

```bash
mvn package
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Se backend estiver em outra origem:

```bash
export VITE_API_BASE_URL="http://localhost:8080"
```

### Docker

Build:

```bash
docker build -t sindico-app .
```

Run exemplo:

```bash
docker run --rm -p 8080:8080 \
  -e DB_URL="jdbc:postgresql://host:5432/database?sslmode=require" \
  -e DB_USERNAME="postgres" \
  -e DB_PASSWORD="..." \
  -e APP_JWT_SECRET="segredo-forte-com-mais-de-32-bytes" \
  sindico-app
```

---

## 15. Fluxos Principais

### Cadastro inicial

1. Usuário acessa cadastro.
2. Informa nome, e-mail, nome do condomínio e senha.
3. Backend valida senha.
4. Backend verifica e-mail duplicado.
5. Cria usuário ativo.
6. Cria condomínio.
7. Cria vínculo usuário-condomínio com perfil `SINDICO`.

### Login SPA

1. React envia e-mail/senha para `/api/auth/login`.
2. Backend autentica.
3. Backend gera JWT.
4. React salva token em `localStorage`.
5. React chama `/api/auth/me`.
6. App libera navegação.

### Operação multi-tenant

1. Usuário autentica.
2. Principal contém lista de condomínios permitidos.
3. `TenantAccessor` escolhe condomínio atual.
4. Services usam o condomínio atual nas consultas.
5. Update/delete só acontece se registro pertence ao condomínio atual.

### Upload de anexo

1. Frontend envia `multipart/form-data` para `/api/anexos`.
2. Backend normaliza tipo da entidade.
3. Backend valida se manutenção/reunião pertence ao condomínio atual.
4. Backend valida arquivo.
5. Backend grava arquivo no disco em pasta por condomínio/tipo/entidade.
6. Backend salva metadados na tabela `anexos`.

---

## 16. Qualidade e Padrões do Projeto

### Padrões bons já presentes

- Monolito modular por domínio.
- Services concentrando regra de negócio.
- Repositories com métodos claros.
- DTOs/records para API.
- `@Transactional` em serviços.
- Bean Validation em controllers.
- Tratamento local de erros nos controllers.
- Tenant centralizado.
- Migrations versionadas.
- `open-in-view: false`.

### Padrões a manter

- Nunca colocar regra de negócio pesada no controller.
- Nunca aceitar `condominioId` vindo do frontend como verdade.
- Todo novo módulo deve filtrar por tenant.
- Update/delete devem validar tenant antes de alterar.
- Toda nova tabela de negócio deve ter índice por `condominio_id`.
- Todo endpoint mutável deve ter teste.
- Todo upload deve validar tamanho, tipo e path.

### Dívidas técnicas

- Algumas regras ainda estão duplicadas entre MVC e API.
- Faltam testes dos módulos novos.
- Ainda existem telas Thymeleaf e React convivendo; precisa decidir estratégia de longo prazo.
- Logout JWT não revoga token.
- Autorização por papel ainda não existe.
- Storage local precisa estratégia para produção.
- Docs antigas falam de módulos planejados e podem ficar divergentes se não forem atualizadas.

---

## 17. Roadmap Recomendado

### Próximo passo imediato

1. Criar testes para manutenção.
2. Criar testes para reunião.
3. Criar testes para anexos.
4. Validar tenant em `ativoId` e `fornecedorId` na manutenção.
5. Integrar anexos nas telas React de manutenção/reunião.
6. Corrigir segurança crítica do JWT e reset de senha.

### MVP mínimo para uso real

1. Login/cadastro estável.
2. CRUD manutenção completo.
3. CRUD reunião completo.
4. Upload/download de anexos.
5. Prestadores.
6. Moradores/unidades.
7. Dashboard básico.
8. Filtros principais.
9. Backup do banco.
10. Deploy com variáveis seguras.

### Pós-MVP

1. Pendências como módulo próprio.
2. Timeline consolidada.
3. Busca global.
4. Convite de usuários.
5. Permissões por perfil.
6. Notificações por e-mail.
7. Exportação PDF.
8. Auditoria de alterações.
9. Storage S3.
10. Observabilidade.

---

## 18. Checklist de Produção

Antes de colocar em produção:

- [ ] Definir domínio final.
- [ ] Configurar HTTPS.
- [ ] Definir `APP_JWT_SECRET` forte e sem fallback.
- [ ] Definir `APP_CORS_ORIGINS` somente com domínio real.
- [ ] Remover logs com tokens de reset.
- [ ] Implementar serviço real de e-mail.
- [ ] Validar backup automático do PostgreSQL.
- [ ] Configurar storage persistente para uploads.
- [ ] Rodar migrations em ambiente limpo.
- [ ] Rodar `mvn test`.
- [ ] Rodar `npm run lint`.
- [ ] Rodar `npm run build`.
- [ ] Criar usuário não-root no Dockerfile.
- [ ] Configurar logs estruturados.
- [ ] Definir rate limiting.
- [ ] Criar monitoramento básico.
- [ ] Criar política de rotação de segredo JWT.

---

## 19. Convenções para Novas Features

Ao criar novo módulo:

1. Criar pacote próprio em `br.com.sindico.app.<modulo>`.
2. Criar entity JPA.
3. Criar migration Flyway.
4. Criar repository.
5. Criar service com tenant.
6. Criar request/response.
7. Criar controller API.
8. Criar testes.
9. Atualizar esta documentação.

Template mínimo de regra multi-tenant:

```java
registroRepository.findById(id)
    .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
    .orElseThrow(() -> new EntityNotFoundException("Registro nao encontrado."));
```

---

## 20. Decisões Técnicas Atuais

### Monolito modular

Escolha adequada para MVP. Mantém deploy simples e permite evoluir rápido sem custo inicial de microsserviços.

### React + Thymeleaf coexistindo

Estado atual de transição. Thymeleaf mantém telas existentes; React recebe telas novas. No médio prazo, escolher:

- manter híbrido;
- migrar tudo para React;
- ou voltar para server-side se simplicidade for prioridade.

Para produto com UX mais rica, recomendação: migrar gradualmente para React e manter Spring como API.

### PostgreSQL + Flyway

Boa escolha para histórico, auditoria, filtros e rastreabilidade.

### JWT para SPA

Funciona para frontend separado. Precisa hardening antes de produção.

### Storage local

Serve para piloto. Em produção, preferir S3/MinIO/compatível com URL assinada e antivírus se necessário.

---

## 21. Pendências Técnicas por Prioridade

### Alta

- Corrigir segredo JWT obrigatório.
- Remover token de reset dos logs.
- Implementar e-mail real.
- Adicionar testes dos módulos novos.
- Validar tenant de `ativoId` e `fornecedorId`.
- Adicionar rate limiting.

### Média

- Criar módulo de pendências.
- Criar filtros de manutenção/reunião.
- Otimizar listagem de reuniões com participantes.
- Migrar upload para storage externo.
- Criar permissões por papel.
- Adicionar CI.

### Baixa

- Melhorar design visual.
- Criar exportação PDF.
- Criar timeline consolidada.
- Melhorar docs antigas.
- Criar guia de contribuição.

---

## 22. Onde Procurar Cada Coisa

| Quero entender | Olhar em |
|---|---|
| Configuração geral | `src/main/resources/application.yml` |
| Segurança | `src/main/java/br/com/sindico/app/config/SecurityConfig.java` |
| JWT | `src/main/java/br/com/sindico/app/security/JwtService.java` |
| Tenant | `src/main/java/br/com/sindico/app/security/SecurityTenantAccessor.java` |
| Login API | `src/main/java/br/com/sindico/app/auth/AuthApiController.java` |
| Cadastro | `src/main/java/br/com/sindico/app/cadastro` |
| Manutenção | `src/main/java/br/com/sindico/app/manutencao` |
| Reunião | `src/main/java/br/com/sindico/app/reuniao` |
| Anexos | `src/main/java/br/com/sindico/app/anexo` |
| Frontend | `frontend/src` |
| Banco | `src/main/resources/db/migration` |
| Testes | `src/test/java/br/com/sindico/app` |
| Deploy container | `Dockerfile` |
| Dependências backend | `pom.xml` |
| Dependências frontend | `frontend/package.json` |

---

## 23. Como Manter Esta Documentação Atualizada

Atualize este arquivo quando:

- criar ou remover rota;
- criar nova tabela;
- alterar autenticação/autorização;
- mudar regra multi-tenant;
- adicionar módulo novo;
- mudar deploy;
- resolver uma pendência relevante;
- descobrir risco de segurança;
- decidir migrar Thymeleaf para React ou vice-versa.

Sugestão de rotina:

- Antes de começar nova feature: ler seções 17, 19 e 21.
- Ao terminar feature: atualizar seções 2, 6, 8, 11, 13 e 21.
- Antes de deploy: revisar seção 18.

---

## 24. Resumo Executivo Atual

O projeto já saiu da fase de ideia e possui base funcional sólida: autenticação, multi-tenant, módulos de cadastro, compromissos, anotações, prestadores, moradores, manutenção, reunião e anexos. A arquitetura é adequada para MVP, com Spring Boot modular, PostgreSQL e React.

O maior risco atual não é arquitetura; é **finalização de qualidade para produção**: segurança de JWT/reset, testes dos módulos novos, autorização por papel, rate limiting, validação extra em uploads e hardening de deploy.

Próximo foco recomendado: **fechar manutenção + reunião + anexos com testes e segurança**, depois evoluir pendências e timeline.

