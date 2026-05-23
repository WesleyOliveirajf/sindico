# Spec-Driven Development: LiveSindIA Mobile

**Versao:** 2.0 — Proposta Completa de Implantacao
**Data:** 2026-05-23
**Autor:** Assistente de Arquitetura
**Status:** Proposta para Aprovacao

---

## Indice

1. [Resumo Executivo](#1-resumo-executivo)
2. [Stack Tecnologica Completa](#2-stack-tecnologica-completa)
3. [Arquitetura Geral e Sincronizacao](#3-arquitetura-geral-e-sincronizacao)
4. [Mapa Completo de Telas e Campos](#4-mapa-completo-de-telas-e-campos)
5. [Mapeamento de API: Tela por Tela](#5-mapeamento-de-api-tela-por-tela)
6. [Endpoints Backend Novos](#6-endpoints-backend-novos)
7. [Autenticacao e Seguranca Mobile](#7-autenticacao-e-seguranca-mobile)
8. [Estrategia Offline Completa](#8-estrategia-offline-completa)
9. [Push Notifications](#9-push-notifications)
10. [Features Exclusivas Mobile](#10-features-exclusivas-mobile)
11. [Design System e UX Mobile](#11-design-system-e-ux-mobile)
12. [Estrutura de Pastas do Projeto](#12-estrutura-de-pastas-do-projeto)
13. [Codigo de Referencia: API Client](#13-codigo-de-referencia-api-client)
14. [Codigo de Referencia: Hooks](#14-codigo-de-referencia-hooks)
15. [Codigo de Referencia: Stores](#15-codigo-de-referencia-stores)
16. [Codigo de Referencia: Componentes](#16-codigo-de-referencia-componentes)
17. [Configuracao Expo Completa](#17-configuracao-expo-completa)
18. [CI/CD e Build Pipeline](#18-cicd-e-build-pipeline)
19. [Testes](#19-testes)
20. [Alteracoes no Backend](#20-alteracoes-no-backend)
21. [Garantia de Consistencia Cross-Platform](#21-garantia-de-consistencia-cross-platform)
22. [Plano de Sprints Detalhado](#22-plano-de-sprints-detalhado)
23. [Custos e Infraestrutura](#23-custos-e-infraestrutura)
24. [Metricas de Sucesso](#24-metricas-de-sucesso)
25. [Riscos e Mitigacoes](#25-riscos-e-mitigacoes)
26. [Checklist Pre-Lancamento](#26-checklist-pre-lancamento)
27. [Glossario](#27-glossario)

---

## 1. Resumo Executivo

### 1.1 O que e

Aplicativo mobile LiveSindIA para Android e iOS, permitindo que sindicos gerenciem condominios de qualquer dispositivo com dados sincronizados em tempo real.

### 1.2 Viabilidade: SIM

| Fator | Status |
|---|---|
| API REST completa (`/api/**`) | Existe, 90% dos endpoints prontos |
| Autenticacao JWT | Existe, funciona identico no mobile |
| PostgreSQL centralizado (Supabase) | Existe, verdade unica de dados |
| Multi-tenant por condominioId | Existe, isolamento ja funciona |
| Google OAuth | Existe, portavel pro mobile |
| Upload de anexos via API | Existe |
| Dados locais no frontend | Nenhum — tudo vem do servidor |

**Zero mudancas destrutivas no backend. O app mobile consome a mesma API que o frontend React web.**

### 1.3 Numeros-Chave

- **29 endpoints existentes** reutilizados sem alteracao
- **4 endpoints novos** necessarios
- **1 migration Flyway** nova (push_tokens)
- **10 telas principais** + sub-telas
- **3 fases** de implantacao (~13 sprints)

---

## 2. Stack Tecnologica Completa

### 2.1 Comparativo de Frameworks

| Criterio | React Native (Expo) | Flutter | Kotlin/Swift Nativo |
|---|---|---|---|
| Reuso de conhecimento React 19 | Alto | Zero | Zero |
| Codebase unica Android + iOS | Sim | Sim | Nao (2 codebases) |
| Reuso de logica web (~40%) | Sim (api.js, validators) | Nao | Nao |
| Curva de aprendizado | Baixa | Media | Alta |
| Performance nativa | Excelente (New Arch) | Excelente | Maxima |
| Hot reload | Sim | Sim | Limitado |
| Push notifications | Expo Push (unificado) | Firebase (manual) | APNs/FCM (manual) |
| OTA updates (sem store) | Sim (expo-updates) | Nao | Nao |
| Custo de manutencao | Baixo (1 dev) | Medio (1 dev) | Alto (2 devs) |
| Tamanho do app | ~25MB | ~15MB | ~10MB |

**Decisao: React Native com Expo SDK 53+**

### 2.2 Stack Completa com Versoes

```
RUNTIME
├── React Native 0.79+
├── Expo SDK 53+
├── TypeScript 5.5+
└── Node.js 20+ (build)

NAVEGACAO
├── expo-router 4+ (file-based routing)
└── @react-navigation/native 7+

UI / DESIGN
├── react-native-paper 5+ (Material Design 3)
│   OU tamagui 1.100+ (design system universal)
├── @expo/vector-icons (icones)
├── react-native-reanimated 3+ (animacoes)
├── react-native-gesture-handler 2+ (gestos)
└── react-native-safe-area-context 4+

ESTADO E DADOS
├── @tanstack/react-query 5+ (cache + sync)
├── @tanstack/query-async-storage-persister (cache offline)
├── zustand 5+ (estado global leve)
└── @react-native-async-storage/async-storage 2+

REDE E API
├── fetch nativo (wrapper TypeScript)
└── @react-native-community/netinfo 11+ (detectar offline)

AUTENTICACAO
├── expo-secure-store 14+ (Keychain iOS / Keystore Android)
├── expo-auth-session 6+ (Google OAuth)
├── expo-web-browser 14+ (abrir OAuth)
└── expo-local-authentication 15+ (biometria)

CAMERA E ARQUIVOS
├── expo-camera 16+ (fotos de manutencao)
├── expo-image-picker 16+ (galeria)
├── expo-document-picker 13+ (upload de documentos)
└── expo-file-system 18+ (cache de anexos)

NOTIFICACOES
├── expo-notifications 0.30+ (push + local)
└── expo-device 7+ (info do dispositivo)

UTILIDADES
├── expo-clipboard 7+ (copiar textos)
├── expo-sharing 13+ (compartilhar)
├── expo-linking 7+ (deep links)
├── expo-haptics 14+ (feedback tatil)
├── expo-splash-screen 0.29+
├── expo-updates 0.27+ (OTA)
├── expo-constants 17+
├── date-fns 4+ (formatacao de datas pt-BR)
└── zod 3+ (validacao de formularios)

BUILD E DEPLOY
├── EAS Build (Expo Application Services)
├── EAS Submit (publicacao nas stores)
└── EAS Update (OTA)

TESTES
├── jest 29+ (unitario)
├── @testing-library/react-native 12+ (componentes)
├── msw 2+ (mock de API)
└── maestro 1.38+ (E2E)

QUALIDADE
├── eslint 9+ (flat config)
├── prettier 3+
├── typescript strict mode
└── husky + lint-staged (pre-commit)
```

### 2.3 Pacotes npm (package.json)

```json
{
  "name": "livesind-mobile",
  "version": "1.0.0",
  "main": "expo-router/entry",
  "scripts": {
    "start": "expo start",
    "android": "expo run:android",
    "ios": "expo run:ios",
    "build:dev": "eas build --profile development",
    "build:preview": "eas build --profile preview",
    "build:prod": "eas build --profile production",
    "submit": "eas submit",
    "update": "eas update",
    "lint": "eslint .",
    "typecheck": "tsc --noEmit",
    "test": "jest",
    "test:e2e": "maestro test e2e/"
  },
  "dependencies": {
    "expo": "~53.0.0",
    "expo-router": "~4.0.0",
    "expo-secure-store": "~14.0.0",
    "expo-auth-session": "~6.0.0",
    "expo-web-browser": "~14.0.0",
    "expo-local-authentication": "~15.0.0",
    "expo-camera": "~16.0.0",
    "expo-image-picker": "~16.0.0",
    "expo-document-picker": "~13.0.0",
    "expo-file-system": "~18.0.0",
    "expo-notifications": "~0.30.0",
    "expo-device": "~7.0.0",
    "expo-clipboard": "~7.0.0",
    "expo-sharing": "~13.0.0",
    "expo-linking": "~7.0.0",
    "expo-haptics": "~14.0.0",
    "expo-splash-screen": "~0.29.0",
    "expo-updates": "~0.27.0",
    "expo-constants": "~17.0.0",
    "react": "19.0.0",
    "react-native": "0.79.0",
    "react-native-paper": "~5.13.0",
    "react-native-reanimated": "~3.17.0",
    "react-native-gesture-handler": "~2.24.0",
    "react-native-safe-area-context": "~4.15.0",
    "react-native-screens": "~4.8.0",
    "@react-navigation/native": "~7.0.0",
    "@tanstack/react-query": "~5.60.0",
    "@tanstack/query-async-storage-persister": "~5.60.0",
    "@react-native-async-storage/async-storage": "~2.1.0",
    "@react-native-community/netinfo": "~11.4.0",
    "@expo/vector-icons": "~14.0.0",
    "zustand": "~5.0.0",
    "zod": "~3.24.0",
    "date-fns": "~4.1.0",
    "date-fns-tz": "~3.2.0"
  },
  "devDependencies": {
    "@types/react": "~19.0.0",
    "typescript": "~5.5.0",
    "jest": "~29.7.0",
    "@testing-library/react-native": "~12.9.0",
    "msw": "~2.7.0",
    "eslint": "~9.15.0",
    "prettier": "~3.4.0"
  }
}
```

---

## 3. Arquitetura Geral e Sincronizacao

### 3.1 Diagrama de Arquitetura

```
┌──────────────────────────────────────────────────────────┐
│                    CLIENTES                               │
│                                                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │  Web React   │  │  Android     │  │  iOS         │      │
│  │  (Vite SPA)  │  │  (Expo)      │  │  (Expo)      │      │
│  │  port:5173   │  │              │  │              │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                 │                 │              │
│         │  HTTPS          │  HTTPS          │  HTTPS       │
│         │  Bearer JWT     │  Bearer JWT     │  Bearer JWT  │
│         └────────┬────────┴────────┬────────┘              │
│                  │                 │                       │
└──────────────────┼─────────────────┼───────────────────────┘
                   │                 │
          ┌────────▼─────────────────▼────────┐
          │         SPRING BOOT 3.5            │
          │         Java 25 / Monolito Modular │
          │                                    │
          │  ┌────────────┐  ┌──────────────┐  │
          │  │  /api/**    │  │  Thymeleaf   │  │
          │  │  REST+JWT   │  │  Sessao web  │  │
          │  └──────┬──────┘  └──────────────┘  │
          │         │                           │
          │  ┌──────▼──────┐                    │
          │  │  Services   │                    │
          │  │  (tenant-   │                    │
          │  │   isolated) │                    │
          │  └──────┬──────┘                    │
          │         │                           │
          │  ┌──────▼──────┐                    │
          │  │ Spring Data │                    │
          │  │ JPA/Hibern. │                    │
          │  └──────┬──────┘                    │
          └─────────┼──────────────────────────┘
                    │
           ┌────────▼────────┐
           │   PostgreSQL     │
           │   (Supabase)     │
           │                  │
           │  ┌────────────┐  │
           │  │ usuarios    │  │
           │  │ condominios │  │
           │  │ compromissos│  │
           │  │ manutencoes │  │
           │  │ reunioes    │  │
           │  │ anotacoes   │  │
           │  │ gastos      │  │
           │  │ recebimentos│  │
           │  │ moradores   │  │
           │  │ unidades    │  │
           │  │ prestadores │  │
           │  │ anexos      │  │
           │  │ push_tokens │ ← NOVO
           │  └────────────┘  │
           └──────────────────┘
```

### 3.2 Principio: Server-First

- **Toda escrita e leitura passa pelo servidor**
- Nao existe banco local com dados de negocio
- Cache local (React Query) e descartavel — se perder, rebusca
- Zero risco de conflito de dados entre plataformas
- Usuario faz acao no celular → abre desktop → dado ja esta la

### 3.3 Fluxo de Dados

```
┌─────────────────────────────────────────────────────────────┐
│  MOBILE APP                                                  │
│                                                              │
│  ┌──────────────┐     ┌──────────────────────────┐           │
│  │  Tela/Screen  │◄───┤  React Query Cache        │           │
│  │  (UI)         │    │  staleTime: 30s            │           │
│  └──────┬────────┘    │  gcTime: 5min              │           │
│         │             │  refetchOnFocus: true       │           │
│         │ mutation    │  refetchOnReconnect: true   │           │
│         ▼             └──────────┬─────────────────┘           │
│  ┌──────────────┐               │ query                      │
│  │  Optimistic   │               ▼                            │
│  │  Update       │        ┌──────────────┐                    │
│  └──────┬────────┘        │  API Client   │                    │
│         │                 │  (api.ts)     │                    │
│         │ online?         └──────┬───────┘                    │
│         ▼                        │                            │
│  ┌──────────────┐         ┌──────▼───────┐                    │
│  │  Offline      │         │  fetch()     │                    │
│  │  Queue        │◄───────►│  + Bearer    │                    │
│  │  (AsyncStore) │ offline │  JWT header  │                    │
│  └──────────────┘         └──────┬───────┘                    │
│                                  │                            │
└──────────────────────────────────┼────────────────────────────┘
                                   │ HTTPS
                            ┌──────▼───────┐
                            │ Spring Boot  │
                            │ /api/**      │
                            └──────┬───────┘
                                   │
                            ┌──────▼───────┐
                            │ PostgreSQL   │
                            └──────────────┘
```

---

## 4. Mapa Completo de Telas e Campos

### 4.1 Arvore de Navegacao

```
App (Root Layout)
│
├── (auth)/ ──────────────────── Grupo nao-autenticado
│   ├── login.tsx                LoginScreen
│   ├── register.tsx             RegisterScreen
│   └── forgot-password.tsx      ForgotPasswordScreen
│
├── (tabs)/ ──────────────────── Tab Navigator (autenticado)
│   ├── _layout.tsx              TabBar: Home | Financeiro | Manutencoes | Mais | Perfil
│   ├── index.tsx                HomeTab (Dashboard)
│   ├── financeiro.tsx           FinanceiroTab
│   ├── manutencoes.tsx          ManutencaoTab
│   ├── mais.tsx                 MaisTab (menu expandido)
│   └── perfil.tsx               PerfilTab
│
├── compromissos/
│   ├── index.tsx                CompromissosList
│   └── novo.tsx                 CompromissoForm
│
├── reunioes/
│   ├── index.tsx                ReunioesList
│   ├── novo.tsx                 ReuniaoForm
│   └── [id].tsx                 ReuniaoDetail
│
├── moradores/
│   ├── index.tsx                MoradoresList + UnidadesList
│   ├── novo-morador.tsx         MoradorForm
│   └── nova-unidade.tsx         UnidadeForm
│
├── prestadores/
│   ├── index.tsx                PrestadoresList
│   └── novo.tsx                 PrestadorForm
│
├── anotacoes/
│   ├── index.tsx                AnotacoesList
│   └── nova.tsx                 AnotacaoForm
│
├── assistente/
│   └── index.tsx                Chat IA
│
├── gastos/
│   ├── novo.tsx                 GastoForm
│   └── [id].tsx                 GastoEdit
│
├── recebimentos/
│   ├── novo.tsx                 RecebimentoForm
│   └── [id].tsx                 RecebimentoEdit
│
└── admin/
    ├── index.tsx                AdminDashboard
    ├── usuarios.tsx             GerenciarUsuarios
    └── ia-config.tsx            ConfiguracaoIA
```

### 4.2 Detalhamento de Cada Tela

#### 4.2.1 LoginScreen

**Campos:**
- Email (TextInput, keyboard: email-address, autoComplete: email)
- Senha (TextInput, secureTextEntry: true)
- Checkbox: "Aceito Termos de Uso e Politica de Privacidade" (obrigatorio para Google)
- Checkbox: "Aceito comunicacoes comerciais" (opcional)

**Acoes:**
- Botao "Entrar com Email" → `POST /api/auth/login`
- Botao "Continuar com Google" → expo-auth-session → `POST /api/auth/google`
- Link "Esqueci minha senha" → ForgotPasswordScreen
- Link "Criar conta gratis" → RegisterScreen
- Botao biometria (se configurado) → auto-login

**Logica:**
- Token salvo em expo-secure-store
- Redireciona pra (tabs) apos login
- 401 no /api/auth/me → mostra login

#### 4.2.2 RegisterScreen

**Campos:**
- Nome do sindico (string, max 150, obrigatorio)
- Nome do condominio (string, max 150, obrigatorio)
- Email (email, obrigatorio)
- Senha (min 8 chars, letras + numeros, obrigatorio)
- Confirmar senha (deve ser igual, obrigatorio)
- Checkbox: aceitar termos (obrigatorio)
- Checkbox: aceitar marketing (opcional)

**Acoes:**
- Botao "Cadastrar" → `POST /api/auth/register` → auto-login

#### 4.2.3 ForgotPasswordScreen

**Campos:**
- Email

**Acoes:**
- Botao "Enviar link" → `POST /api/auth/esqueci-senha` (precisa criar endpoint API)
- Ou: abrir WebView da pagina `/esqueci-senha` existente

#### 4.2.4 HomeTab (Dashboard)

**Dados exibidos:**
- Card: Gastos do mes (valor, qtd registros) — cor vermelha
- Card: Recebimentos do mes (valor, qtd registros) — cor verde
- Card: Saldo real (recebimentos - gastos) — verde se positivo, vermelho se negativo
- Lista: Compromissos a fazer (top 5, com situacao: Hoje/Agendado/Vencido)
- Lista: Manutencoes abertas (top 5, com status e local)
- Lista: Atividade financeira recente (top 5, com tipo e valor)
- Lista: Reunioes recentes (top 3, com tipo e data)
- Lista: Anotacoes recentes (top 3, com importancia e categoria)
- Atalhos rapidos: Novo gasto, Novo recebimento, Nova manutencao, Nova reuniao, Novo compromisso

**APIs chamadas (paralelo):**
- `GET /api/gastos?mes={atual}&ano={atual}`
- `GET /api/recebimentos?mes={atual}&ano={atual}`
- `GET /api/compromissos`
- `GET /api/manutencoes`
- `GET /api/reunioes`
- `GET /api/anotacoes`

**UX mobile:**
- Pull-to-refresh em toda tela
- Skeleton loading
- Cards com sombra e bordas arredondadas
- FAB (Floating Action Button) com menu de acoes rapidas

#### 4.2.5 FinanceiroTab (Gastos + Recebimentos)

**Segmented control:** Gastos | Recebimentos

**Filtros (compartilhados):**
- Mes (picker: Janeiro..Dezembro + "Todos")
- Ano (picker: ultimos 7 anos + "Todos")
- Tipo de gasto (apenas na aba gastos): AGUA, LUZ, GAS, SEGURO, LIMPEZA, MANUTENCAO, ADMINISTRACAO, SALARIOS, IMPOSTOS, OUTROS

**Cards de resumo:**
- Total de gastos (vermelho)
- Total de recebimentos (verde)
- Saldo real (verde/vermelho)

**Aba Gastos — Lista:**
- Cada item: descricao, tipo (label), fixo/variavel, parcela X/Y (se parcelado), data, valor
- Acoes: Editar, Remover (com confirmacao)
- Botao IA "Analisar gastos com IA" → `GET /api/ia/gastos/analise?mes=X&ano=Y`

**Aba Gastos — Form (tela separada):**
- Descricao (string, max 255, obrigatorio)
- Tipo (picker: AGUA, LUZ, GAS, SEGURO, LIMPEZA, MANUTENCAO, ADMINISTRACAO, SALARIOS, IMPOSTOS, OUTROS)
- Valor R$ (number, step 0.01, min 0.01, obrigatorio)
- Data do gasto (date picker, obrigatorio)
- Switch: Gasto fixo (recorrente)
- Switch: Gasto parcelado
  - Se sim: Parcela atual (number, min 1), Total parcelas (number, min 1)
- Observacoes (textarea)

**Aba Recebimentos — Lista:**
- Cada item: descricao, tipo (label), data, valor (verde)
- Acoes: Remover (com confirmacao)

**Aba Recebimentos — Form:**
- Descricao (string, max 255, obrigatorio)
- Tipo (picker: TAXA_CONDOMINIO, ALUGUEL_AREA, MULTA, RESERVA_FUNDO, OUTROS)
- Valor R$ (number, obrigatorio)
- Data do recebimento (date picker, obrigatorio)
- Observacoes (textarea)

**APIs:**
- `GET /api/gastos?mes=X&ano=Y&tipo=Z`
- `POST /api/gastos` — criar
- `PUT /api/gastos/{id}` — editar
- `DELETE /api/gastos/{id}` — remover
- `GET /api/recebimentos?mes=X&ano=Y`
- `POST /api/recebimentos` — criar
- `DELETE /api/recebimentos/{id}` — remover
- `GET /api/ia/gastos/analise?mes=X&ano=Y` — analise IA

#### 4.2.6 ManutencaoTab

**Lista:**
- Cada item: titulo, tipo (PREVENTIVA/CORRETIVA), status (ABERTA/AGENDADA/EM_ANDAMENTO/CONCLUIDA/CANCELADA), categoria, local, prestador (nome + tel com link WhatsApp), responsavel, custos previsto/realizado, datas ocorrencia/execucao, descricao, observacoes

**Form (tela separada):**
- Titulo (string, max 150, obrigatorio)
- Tipo (picker: PREVENTIVA, CORRETIVA)
- Categoria (string, max 50)
- Local (string, max 150)
- Prestador (picker carregado de /api/prestadores, obrigatorio)
- Responsavel interno (string, max 150)
- Status (picker: ABERTA, AGENDADA, EM_ANDAMENTO, CONCLUIDA, CANCELADA)
- Data da ocorrencia (date picker)
- Data da execucao (date picker)
- Custo previsto (currency input R$)
- Custo realizado (currency input R$)
- Descricao (textarea — usada pela triagem IA)
- Botao "Triar com IA" (requer descricao preenchida) → `POST /api/ia/manutencao/triar`
  - Preenche automaticamente: tipo, categoria, titulo, observacoes
- Observacoes (textarea)
- Botao camera (expo-camera) → tira foto → upload como anexo
- Botao galeria (expo-image-picker) → seleciona imagem → upload como anexo

**APIs:**
- `GET /api/manutencoes`
- `POST /api/manutencoes` — criar
- `PUT /api/manutencoes/{id}` — editar
- `DELETE /api/manutencoes/{id}` — remover
- `GET /api/prestadores` — preencher picker
- `POST /api/ia/manutencao/triar` — triagem IA
- `POST /api/anexos` — upload de foto (multipart, entidadeTipo=MANUTENCAO)
- `GET /api/anexos?entidadeTipo=MANUTENCAO&entidadeId={id}` — listar anexos
- `GET /api/anexos/{id}/download` — download/visualizar

#### 4.2.7 CompromissosList + CompromissoForm

**Segmented control:** A Fazer (count) | Concluidos (count)

**Lista:**
- Cada item: checkbox de conclusao, titulo (riscado se concluido), tags (status + tipo), descricao, data inicio, data conclusao, local
- Acoes: Concluir (PATCH), Excluir (DELETE com confirmacao)
- Cor lateral: amarelo (aberto), verde (concluido)

**Form:**
- Titulo (string, max 150, obrigatorio)
- Data de inicio (date picker, obrigatorio)
- Tipo (picker: OUTROS, MANUTENCAO, REUNIAO)
- Local (string, max 150)
- Descricao (textarea)

**APIs:**
- `GET /api/compromissos`
- `POST /api/compromissos` — criar
- `PATCH /api/compromissos/{id}/concluir` — marcar concluido
- `DELETE /api/compromissos/{id}` — excluir

#### 4.2.8 ReunioesList + ReuniaoForm + ReuniaoDetail

**Lista:**
- Cada item: titulo, tipo (ORDINARIA/EXTRAORDINARIA/CONSELHO/ASSEMBLEIA), data/hora, local, pauta, decisoes, pendencias, participantes
- Botao "Gerar Ata com IA" por reuniao

**Form:**
- Titulo (string, max 150, obrigatorio)
- Tipo (picker: ORDINARIA, EXTRAORDINARIA, CONSELHO, ASSEMBLEIA)
- Data e horario (datetime picker, obrigatorio)
- Local (string, max 150)
- Link (string, max 500)
- Pauta (textarea)
- Resumo (textarea)
- Decisoes (textarea)
- Pendencias geradas (textarea)
- Participantes (lista editavel, 1 nome por linha, cada um com flag `presente: true`)

**APIs:**
- `GET /api/reunioes`
- `POST /api/reunioes` — criar
- `PUT /api/reunioes/{id}` — editar
- `DELETE /api/reunioes/{id}` — remover
- `POST /api/ia/reuniao/{id}/ata` — gerar ata IA
- `POST /api/anexos` — upload para reuniao (entidadeTipo=REUNIAO)
- `GET /api/anexos?entidadeTipo=REUNIAO&entidadeId={id}`

#### 4.2.9 MoradoresList + Forms

**Secao Unidades:**
- Lista: bloco, numero, complemento
- Form: Bloco (string), Numero (string, obrigatorio), Complemento (string)

**Secao Moradores:**
- Lista: nome, email, telefone, papel, unidade vinculada, observacoes
- Acoes inline: editar (cada campo), inativar
- Form: unidadeId (picker), nome (obrigatorio), email, telefone, papel (picker: PROPRIETARIO, INQUILINO, DEPENDENTE, ZELADOR, OUTRO), observacoes

**APIs:**
- `GET /api/unidades`
- `POST /api/unidades` — criar unidade
- `GET /api/moradores`
- `POST /api/moradores` — criar morador
- `PUT /api/moradores/{id}` — editar morador
- `POST /api/moradores/{id}/inativar` — inativar

#### 4.2.10 PrestadoresList + PrestadorForm

**Lista:**
- Cada item: nome, telefone (com link WhatsApp), area de atuacao
- Acoes inline: editar, inativar

**Form:**
- Nome (string, obrigatorio)
- Telefone (string)
- Area de atuacao (string) — mapeado para `historicoServicos` na API

**APIs:**
- `GET /api/prestadores`
- `POST /api/prestadores` — criar
- `PUT /api/prestadores/{id}` — editar
- `POST /api/prestadores/{id}/inativar` — inativar

#### 4.2.11 AnotacoesList + AnotacaoForm

**Filtros:**
- Texto (busca livre)
- Data inicio (date picker)
- Data fim (date picker)

**Lista:**
- Cada item: titulo, categoria, importancia (NORMAL/IMPORTANTE/CRITICO), data referencia, descricao
- Acoes: editar, excluir (com confirmacao)

**Form:**
- Titulo (string, obrigatorio)
- Categoria (string)
- Descricao (textarea)
- Referencia (string)
- Importancia (picker: NORMAL, IMPORTANTE, CRITICO)
- Data de referencia (date picker)

**APIs:**
- `GET /api/anotacoes?texto=X&dataInicio=Y&dataFim=Z`
- `POST /api/anotacoes` — criar
- `PUT /api/anotacoes/{id}` — editar
- `DELETE /api/anotacoes/{id}` — excluir

#### 4.2.12 Assistente IA (Chat)

**UI:** Chat bubble interface (similar ao web)

**Elementos:**
- Lista de mensagens (user/assistant/error)
- Input de texto com botao "Enviar"
- Sugestoes iniciais (botoes):
  - "Quais manutencoes estao pendentes no condominio?"
  - "Qual o resumo dos gastos cadastrados recentemente?"
  - "Quem sao os moradores ativos cadastrados e em quais unidades?"
  - "Quais foram as decisoes e pendencias da ultima reuniao?"
  - "Quais sao os proximos compromissos na nossa agenda?"
- Animacao de "pensando" (3 dots)

**API:**
- `POST /api/ia/chat` — { mensagem: string } → { resposta: string }

#### 4.2.13 PerfilTab

**Secoes:**
- Dados pessoais (nome, email, telefone)
- Trocar senha
- Selecionar condominio (se usuario tem mais de 1)
- Configuracoes de notificacao (on/off por tipo)
- Biometria (ativar/desativar)
- Sobre o app (versao)
- Termos de uso / Privacidade (abrir WebView)
- Sair (logout)

#### 4.2.14 Admin (condicional ROLE_ADMIN)

**Abas:** Usuarios | Configuracao IA

**Usuarios:**
- Stats: total usuarios, ativos, pendentes, condominios
- Lista de usuarios: nome, email, status (ativo/pendente/inativo), ultimo acesso
- Acoes: aprovar, rejeitar, reativar

**Configuracao IA:**
- Provider (picker: OPENAI, ANTHROPIC, GEMINI, OLLAMA, CUSTOM)
- API Key (campo senha)
- Model (string)
- Base URL (string, para OLLAMA/CUSTOM)
- Switch: ativo
- Botao testar conexao

**APIs:**
- `GET /api/admin/stats`
- `GET /api/admin/usuarios`
- `POST /api/admin/usuarios/{id}/aprovar`
- `POST /api/admin/usuarios/{id}/rejeitar`
- `POST /api/admin/usuarios/{id}/reativar`
- `GET /api/ia/config`
- `POST /api/ia/config`
- `POST /api/ia/config/testar`

---

## 5. Mapeamento de API: Tela por Tela

### 5.1 Todos os Endpoints Utilizados

| # | Metodo | Endpoint | Tela Mobile | Existe? |
|---|---|---|---|---|
| 1 | POST | `/api/auth/login` | Login | SIM |
| 2 | POST | `/api/auth/google` | Login (Google) | SIM |
| 3 | POST | `/api/auth/register` | Cadastro | SIM |
| 4 | GET | `/api/auth/me` | App (verificar sessao) | SIM |
| 5 | POST | `/api/auth/logout` | Perfil (sair) | SIM |
| 6 | GET | `/api/compromissos` | Dashboard, Compromissos | SIM |
| 7 | POST | `/api/compromissos` | CompromissoForm | SIM |
| 8 | PATCH | `/api/compromissos/{id}/concluir` | CompromissosList | SIM |
| 9 | DELETE | `/api/compromissos/{id}` | CompromissosList | SIM |
| 10 | GET | `/api/manutencoes` | Dashboard, Manutencoes | SIM |
| 11 | POST | `/api/manutencoes` | ManutencaoForm | SIM |
| 12 | PUT | `/api/manutencoes/{id}` | ManutencaoEdit | SIM |
| 13 | DELETE | `/api/manutencoes/{id}` | ManutencaosList | SIM |
| 14 | GET | `/api/reunioes` | Dashboard, Reunioes | SIM |
| 15 | POST | `/api/reunioes` | ReuniaoForm | SIM |
| 16 | PUT | `/api/reunioes/{id}` | ReuniaoEdit | SIM |
| 17 | DELETE | `/api/reunioes/{id}` | ReunioesList | SIM |
| 18 | GET | `/api/anotacoes` | Dashboard, Anotacoes | SIM |
| 19 | POST | `/api/anotacoes` | AnotacaoForm | SIM |
| 20 | PUT | `/api/anotacoes/{id}` | AnotacaoEdit | SIM |
| 21 | DELETE | `/api/anotacoes/{id}` | AnotacoesList | SIM |
| 22 | GET | `/api/prestadores` | Prestadores, ManutencaoForm | SIM |
| 23 | POST | `/api/prestadores` | PrestadorForm | SIM |
| 24 | PUT | `/api/prestadores/{id}` | PrestadorEdit | SIM |
| 25 | POST | `/api/prestadores/{id}/inativar` | PrestadoresList | SIM |
| 26 | GET | `/api/moradores` | Moradores | SIM |
| 27 | POST | `/api/moradores` | MoradorForm | SIM |
| 28 | PUT | `/api/moradores/{id}` | MoradorEdit | SIM |
| 29 | POST | `/api/moradores/{id}/inativar` | MoradoresList | SIM |
| 30 | GET | `/api/unidades` | Moradores | SIM |
| 31 | POST | `/api/unidades` | UnidadeForm | SIM |
| 32 | GET | `/api/gastos` | Dashboard, Financeiro | SIM |
| 33 | POST | `/api/gastos` | GastoForm | SIM |
| 34 | PUT | `/api/gastos/{id}` | GastoEdit | SIM |
| 35 | DELETE | `/api/gastos/{id}` | GastosList | SIM |
| 36 | GET | `/api/recebimentos` | Dashboard, Financeiro | SIM |
| 37 | POST | `/api/recebimentos` | RecebimentoForm | SIM |
| 38 | DELETE | `/api/recebimentos/{id}` | RecebimentosList | SIM |
| 39 | POST | `/api/anexos` | Manutencao, Reuniao | SIM |
| 40 | GET | `/api/anexos` | ManutencaoDetail, ReuniaoDetail | SIM |
| 41 | GET | `/api/anexos/{id}/download` | AnexoViewer | SIM |
| 42 | POST | `/api/ia/chat` | Assistente IA | SIM |
| 43 | POST | `/api/ia/manutencao/triar` | ManutencaoForm | SIM |
| 44 | GET | `/api/ia/gastos/analise` | Financeiro | SIM |
| 45 | POST | `/api/ia/reuniao/{id}/ata` | ReuniaoDetail | SIM |
| 46 | GET | `/api/admin/stats` | Admin | SIM |
| 47 | GET | `/api/admin/usuarios` | Admin | SIM |
| 48 | POST | `/api/admin/usuarios/{id}/aprovar` | Admin | SIM |
| 49 | POST | `/api/admin/usuarios/{id}/rejeitar` | Admin | SIM |
| 50 | POST | `/api/admin/usuarios/{id}/reativar` | Admin | SIM |
| 51 | GET | `/api/ia/config` | Admin IA Config | SIM |
| 52 | POST | `/api/ia/config` | Admin IA Config | SIM |
| 53 | POST | `/api/ia/config/testar` | Admin IA Config | SIM |
| 54 | POST | `/api/auth/refresh` | App (refresh token) | **NOVO** |
| 55 | POST | `/api/push/register` | App (on mount) | **NOVO** |
| 56 | POST | `/api/push/unregister` | Perfil (logout) | **NOVO** |
| 57 | GET | `/api/notificacoes` | Notificacoes | **NOVO** |

**Total: 53 existentes + 4 novos = 57 endpoints**

---

## 6. Endpoints Backend Novos

### 6.1 Refresh Token

```java
// POST /api/auth/refresh
// Headers: Authorization: Bearer <refresh_token>
// Response: { "token": "novo_access_token", "refreshToken": "novo_refresh_token" }
```

**Implementacao:**
- Access token: validade 15 min
- Refresh token: validade 30 dias, salvo em `refresh_tokens` table
- Mobile envia refresh token quando access token expira (401)
- Revogacao ao logout

### 6.2 Push Token Registration

```java
// POST /api/push/register
// Body: { "token": "ExponentPushToken[xxx]", "platform": "android|ios" }
// Response: 200 OK

// POST /api/push/unregister
// Body: { "token": "ExponentPushToken[xxx]" }
// Response: 200 OK
```

### 6.3 Notificacoes

```java
// GET /api/notificacoes?page=0&size=20
// Response:
{
  "content": [
    {
      "id": "uuid",
      "tipo": "COMPROMISSO_PROXIMO",
      "titulo": "Compromisso em 1 hora",
      "mensagem": "Vistoria da bomba d'agua as 14:00",
      "lida": false,
      "createdAt": "2026-05-23T13:00:00"
    }
  ],
  "totalElements": 42,
  "totalPages": 3
}
```

### 6.4 Migration Flyway

```sql
-- V19__push_tokens_e_notificacoes.sql

CREATE TABLE push_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    token VARCHAR(255) NOT NULL,
    platform VARCHAR(10) NOT NULL CHECK (platform IN ('android', 'ios')),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (usuario_id, token)
);

CREATE TABLE notificacoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    condominio_id UUID NOT NULL REFERENCES condominios(id),
    tipo VARCHAR(50) NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    mensagem TEXT,
    lida BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_push_tokens_usuario ON push_tokens(usuario_id) WHERE ativo = TRUE;
CREATE INDEX idx_notificacoes_usuario ON notificacoes(usuario_id, lida, created_at DESC);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash) WHERE revoked = FALSE;
```

---

## 7. Autenticacao e Seguranca Mobile

### 7.1 Fluxo de Login Email/Senha

```
1. Usuario digita email + senha
2. POST /api/auth/login → { token, refreshToken, email, condominioId, nome, roles }
3. Access token → expo-secure-store key "accessToken"
4. Refresh token → expo-secure-store key "refreshToken"
5. Toda request: Authorization: Bearer <accessToken>
6. 401 → tenta refresh → se falhar → redireciona pro login
```

### 7.2 Fluxo Google OAuth Mobile

```
1. expo-auth-session abre browser nativo do Google
2. Usuario autoriza
3. Recebe credential token (id_token do Google)
4. POST /api/auth/google { credentialToken, aceitouTermos, aceitouMarketing }
5. Backend valida com Google API Client → retorna JWT
6. Mesmo fluxo de armazenamento
```

### 7.3 Fluxo de Refresh Token

```
1. Request retorna 401
2. API client intercepta
3. POST /api/auth/refresh com refresh token
4. Se 200: salva novos tokens, repete request original
5. Se 401: limpa tokens, redireciona pro login
6. Lock mutex para evitar refresh concorrente
```

### 7.4 Biometria

```
1. Apos primeiro login, pergunta: "Ativar login com biometria?"
2. Se sim:
   a. expo-local-authentication verifica suporte
   b. Salva credenciais criptografadas no Keychain/Keystore
   c. Flag "biometryEnabled" em expo-secure-store
3. Proximo acesso:
   a. Verifica flag
   b. Solicita biometria (FaceID / fingerprint)
   c. Se sucesso: recupera credenciais → auto-login
   d. Se falha: mostra form de login normal
```

### 7.5 Comparativo de Seguranca

| Aspecto | Web Atual | Mobile Proposto |
|---|---|---|
| Armazenamento token | sessionStorage (perde ao fechar aba) | expo-secure-store (Keychain/Keystore, criptografado pelo SO) |
| Vulnerabilidade | XSS pode ler sessionStorage | Isolado por app, criptografia nativa |
| Persistencia | Nao persiste | Persiste entre sessoes |
| Refresh token | Nao implementado | Sim, 30 dias |
| Biometria | N/A | FaceID / Fingerprint |
| Certificate pinning | Nao | Recomendado para producao |

---

## 8. Estrategia Offline Completa

### 8.1 Niveis de Suporte

| Nivel | Funcionalidade | Implementacao |
|---|---|---|
| **Leitura offline** | Ver dados cacheados sem internet | React Query persisted cache |
| **Escrita offline** | Criar/editar sem internet, sync depois | Mutation queue (AsyncStorage) |
| **Indicador visual** | Banner "Voce esta offline" | NetInfo + OfflineBanner component |
| **Conflito** | Resolver dados conflitantes | Server-wins (Last Write Wins) |

### 8.2 Codigo de Referencia

```typescript
// src/lib/queryClient.ts

import { QueryClient } from '@tanstack/react-query';
import { createAsyncStoragePersister } from '@tanstack/query-async-storage-persister';
import AsyncStorage from '@react-native-async-storage/async-storage';
import NetInfo from '@react-native-community/netinfo';
import { onlineManager } from '@tanstack/react-query';

// Sync online state with device connectivity
NetInfo.addEventListener(state => {
  onlineManager.setOnline(!!state.isConnected);
});

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,           // 30s antes de revalidar
      gcTime: 10 * 60_000,         // 10min no cache
      refetchOnReconnect: true,     // revalida ao reconectar
      retry: 3,
      retryDelay: attemptIndex => Math.min(1000 * 2 ** attemptIndex, 30000),
    },
    mutations: {
      retry: 3,
      retryDelay: attemptIndex => Math.min(1000 * 2 ** attemptIndex, 30000),
    },
  },
});

export const asyncStoragePersister = createAsyncStoragePersister({
  storage: AsyncStorage,
  key: 'LIVESIND_QUERY_CACHE',
  throttleTime: 1000,
});
```

### 8.3 Fila de Mutacoes Offline

```typescript
// Quando offline:
// 1. React Query pausa mutations automaticamente
// 2. Ao reconectar, processa em ordem FIFO
// 3. Se mutation falhar apos 3 retries: mostra toast com erro

// Para garantir que mutations pendentes nao se perdem ao fechar o app:
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client';

// No root layout:
<PersistQueryClientProvider
  client={queryClient}
  persistOptions={{ persister: asyncStoragePersister }}
  onSuccess={() => {
    // Mutations pausadas sao retomadas automaticamente
    queryClient.resumePausedMutations().then(() => {
      queryClient.invalidateQueries();
    });
  }}
>
  {children}
</PersistQueryClientProvider>
```

---

## 9. Push Notifications

### 9.1 Eventos

| Evento | Destinatario | Quando |
|---|---|---|
| Compromisso proximo | Sindico | 1 hora antes |
| Manutencao criada | Todos sindicos do condominio | Imediato |
| Manutencao status alterado | Criador | Imediato |
| Reuniao agendada | Todos do condominio | Imediato |
| Reuniao em 24h | Participantes | 24h antes |
| Novo morador | Sindico | Imediato |
| Gasto acima de R$ 5.000 | Sindico | Imediato |

### 9.2 Fluxo Completo

```
Mobile (Expo)                    Backend                     Expo Push API
     │                              │                            │
     ├── expo-notifications         │                            │
     │   .getExpoPushTokenAsync()   │                            │
     │   → "ExponentPushToken[xx]"  │                            │
     │                              │                            │
     ├── POST /api/push/register ──►│                            │
     │   { token, platform }        │── salva em push_tokens     │
     │                              │                            │
     │                              │                            │
     │      (evento ocorre)         │                            │
     │                              ├── busca tokens do usuario  │
     │                              ├── POST https://exp.host/ ──►│
     │                              │   /--/api/v2/push/send     │
     │                              │   { to, title, body }      │
     │                              │                            │
     │   ◄──────────────────────────┼────────────────────────────┤
     │   notificacao aparece        │                            │
     │   no celular                 │                            │
```

### 9.3 Entidades Backend

```java
@Entity
@Table(name = "push_tokens")
public class PushToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false, length = 10)
    private String platform;

    @Column(nullable = false)
    private boolean ativo = true;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

@Entity
@Table(name = "notificacoes")
public class Notificacao {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "condominio_id", nullable = false)
    private UUID condominioId;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false, length = 200)
    private String titulo;

    private String mensagem;

    @Column(nullable = false)
    private boolean lida = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

---

## 10. Features Exclusivas Mobile

| Feature | Lib Expo | Descricao |
|---|---|---|
| Camera | expo-camera | Tirar foto de manutencao direto do app |
| Galeria | expo-image-picker | Selecionar imagem existente |
| Biometria | expo-local-authentication | FaceID / Fingerprint login |
| Push | expo-notifications | Notificacoes nativas |
| Compartilhar | expo-sharing | Share sheet nativo (WhatsApp, email) |
| Haptics | expo-haptics | Feedback tatil ao concluir compromisso |
| Deep links | expo-linking | Abrir tela especifica por URL |
| Clipboard | expo-clipboard | Copiar dados (ata, relatorio) |
| Document picker | expo-document-picker | Upload de PDF/documentos |
| OTA updates | expo-updates | Atualizar JS sem ir pra store |
| Pull-to-refresh | RefreshControl nativo | Em todas as listas |
| Swipe actions | react-native-gesture-handler | Deslizar pra editar/excluir |
| Skeleton loading | react-native-paper | Placeholder animado enquanto carrega |

---

## 11. Design System e UX Mobile

### 11.1 Tema (Cores)

```typescript
export const theme = {
  colors: {
    primary: '#6366f1',       // Indigo (brand)
    primaryDark: '#4f46e5',
    accent: '#2dd4bf',        // Teal (IA)
    success: '#16a34a',       // Verde (recebimentos)
    danger: '#dc2626',        // Vermelho (gastos)
    warning: '#f59e0b',       // Amarelo (pendente)
    background: '#0a0a0f',    // Fundo escuro (dark mode padrao)
    surface: '#1a1a2e',       // Card background
    surfaceVariant: '#16213e',
    text: '#e4e4e7',
    textSecondary: '#a1a1aa',
    border: '#27273a',
    inputBg: '#0f0f1a',
  },
  spacing: {
    xs: 4, sm: 8, md: 16, lg: 24, xl: 32,
  },
  borderRadius: {
    sm: 6, md: 10, lg: 16, full: 9999,
  },
  fontSize: {
    xs: 12, sm: 14, md: 16, lg: 18, xl: 24, xxl: 32,
  },
};
```

### 11.2 Componentes Base

| Componente | Uso | Lib Base |
|---|---|---|
| Button | Acoes primarias/secundarias/danger | Paper Button + custom |
| Card | Container com sombra | Paper Card + custom |
| Input | Campos de texto | Paper TextInput |
| Select | Pickers nativos | @react-native-picker/picker |
| DatePicker | Selecao de data | @react-native-community/datetimepicker |
| Switch | Toggles (fixo, parcelado) | React Native Switch |
| Badge | Tags de status | Paper Badge |
| FAB | Floating Action Button | Paper FAB |
| BottomSheet | Formularios mobile | @gorhom/bottom-sheet |
| ConfirmDialog | Confirmacao de exclusao | Paper Dialog |
| Toast | Mensagens de sucesso/erro | react-native-toast-message |
| Skeleton | Loading placeholder | Paper ActivityIndicator |
| EmptyState | Lista vazia | Custom component |
| OfflineBanner | Indicador offline | Custom + NetInfo |
| CurrencyInput | Input monetario R$ | Custom com formatacao |

### 11.3 Padroes de UX

- **Pull-to-refresh** em todas as listas
- **Skeleton loading** em vez de spinner
- **Optimistic updates** para acoes rapidas (concluir compromisso)
- **Swipe-to-action** em listas (deslizar = editar/excluir)
- **Bottom sheet** para formularios curtos (novo gasto)
- **Full-screen modal** para formularios complexos (manutencao)
- **Haptic feedback** ao completar acoes
- **Toast notifications** para sucesso/erro
- **Infinite scroll** para listas longas
- **Search bar** com debounce em anotacoes
- **Dark mode** como padrao (consistente com web)
- **Safe area** respeitada em todos os dispositivos

---

## 12. Estrutura de Pastas do Projeto

```
mobile/
├── app/                              # Expo Router (file-based routing)
│   ├── _layout.tsx                   # Root: providers, auth guard, theme
│   ├── (auth)/
│   │   ├── _layout.tsx               # Auth layout (sem tabs)
│   │   ├── login.tsx
│   │   ├── register.tsx
│   │   └── forgot-password.tsx
│   ├── (tabs)/
│   │   ├── _layout.tsx               # Tab navigator config
│   │   ├── index.tsx                 # Dashboard
│   │   ├── financeiro.tsx            # Gastos + Recebimentos
│   │   ├── manutencoes.tsx           # Lista manutencoes
│   │   ├── mais.tsx                  # Menu de modulos
│   │   └── perfil.tsx                # Perfil + config
│   ├── gastos/
│   │   ├── novo.tsx
│   │   └── [id].tsx
│   ├── recebimentos/
│   │   ├── novo.tsx
│   │   └── [id].tsx
│   ├── manutencoes/
│   │   ├── novo.tsx
│   │   └── [id].tsx
│   ├── compromissos/
│   │   ├── index.tsx
│   │   └── novo.tsx
│   ├── reunioes/
│   │   ├── index.tsx
│   │   ├── novo.tsx
│   │   └── [id].tsx
│   ├── moradores/
│   │   ├── index.tsx
│   │   ├── novo-morador.tsx
│   │   └── nova-unidade.tsx
│   ├── prestadores/
│   │   ├── index.tsx
│   │   └── novo.tsx
│   ├── anotacoes/
│   │   ├── index.tsx
│   │   └── nova.tsx
│   ├── assistente/
│   │   └── index.tsx
│   └── admin/
│       ├── index.tsx
│       ├── usuarios.tsx
│       └── ia-config.tsx
│
├── src/
│   ├── api/
│   │   ├── client.ts                 # Fetch wrapper + interceptors + refresh
│   │   ├── auth.ts                   # login, register, me, logout, google, refresh
│   │   ├── compromissos.ts           # CRUD compromissos
│   │   ├── manutencoes.ts            # CRUD manutencoes
│   │   ├── reunioes.ts               # CRUD reunioes
│   │   ├── anotacoes.ts              # CRUD anotacoes
│   │   ├── moradores.ts              # CRUD moradores + unidades
│   │   ├── prestadores.ts            # CRUD prestadores
│   │   ├── gastos.ts                 # CRUD gastos
│   │   ├── recebimentos.ts           # CRUD recebimentos
│   │   ├── anexos.ts                 # Upload, list, download
│   │   ├── ia.ts                     # Chat, triagem, analise, ata
│   │   ├── admin.ts                  # Stats, usuarios, IA config
│   │   └── push.ts                   # Register/unregister push token
│   │
│   ├── hooks/
│   │   ├── useAuth.ts                # Login state, biometry, auto-refresh
│   │   ├── useCompromissos.ts         # useQuery + useMutation
│   │   ├── useManutencoes.ts
│   │   ├── useReunioes.ts
│   │   ├── useAnotacoes.ts
│   │   ├── useMoradores.ts
│   │   ├── usePrestadores.ts
│   │   ├── useGastos.ts
│   │   ├── useRecebimentos.ts
│   │   ├── useAnexos.ts
│   │   ├── useIA.ts
│   │   ├── useAdmin.ts
│   │   ├── usePushNotifications.ts
│   │   └── useOnlineStatus.ts
│   │
│   ├── components/
│   │   ├── ui/
│   │   │   ├── Button.tsx
│   │   │   ├── Card.tsx
│   │   │   ├── Input.tsx
│   │   │   ├── CurrencyInput.tsx
│   │   │   ├── Select.tsx
│   │   │   ├── DatePicker.tsx
│   │   │   ├── Switch.tsx
│   │   │   ├── Badge.tsx
│   │   │   ├── FAB.tsx
│   │   │   ├── ConfirmDialog.tsx
│   │   │   ├── Toast.tsx
│   │   │   ├── EmptyState.tsx
│   │   │   ├── ErrorState.tsx
│   │   │   ├── LoadingSkeleton.tsx
│   │   │   └── OfflineBanner.tsx
│   │   ├── forms/
│   │   │   ├── GastoForm.tsx
│   │   │   ├── RecebimentoForm.tsx
│   │   │   ├── ManutencaoForm.tsx
│   │   │   ├── CompromissoForm.tsx
│   │   │   ├── ReuniaoForm.tsx
│   │   │   ├── MoradorForm.tsx
│   │   │   ├── UnidadeForm.tsx
│   │   │   ├── PrestadorForm.tsx
│   │   │   └── AnotacaoForm.tsx
│   │   ├── lists/
│   │   │   ├── GastoCard.tsx
│   │   │   ├── CompromissoCard.tsx
│   │   │   ├── ManutencaoCard.tsx
│   │   │   ├── ReuniaoCard.tsx
│   │   │   └── AnotacaoCard.tsx
│   │   └── shared/
│   │       ├── MetricCard.tsx
│   │       ├── SectionHeader.tsx
│   │       ├── QuickActions.tsx
│   │       ├── ChatBubble.tsx
│   │       └── AnexosList.tsx
│   │
│   ├── store/
│   │   ├── authStore.ts              # Zustand: user, tokens, biometry
│   │   └── settingsStore.ts          # Zustand: theme, notifications prefs
│   │
│   ├── lib/
│   │   ├── queryClient.ts            # React Query + persister config
│   │   ├── secureStorage.ts          # expo-secure-store wrapper
│   │   └── notifications.ts          # Push registration helper
│   │
│   ├── utils/
│   │   ├── formatters.ts             # formatCurrency, formatDate, formatDateTime
│   │   ├── validators.ts             # Zod schemas for forms
│   │   └── constants.ts              # TIPOS_GASTO, PAPEIS, IMPORTANCIAS, etc.
│   │
│   └── types/
│       ├── auth.ts
│       ├── compromisso.ts
│       ├── manutencao.ts
│       ├── reuniao.ts
│       ├── anotacao.ts
│       ├── morador.ts
│       ├── prestador.ts
│       ├── gasto.ts
│       ├── recebimento.ts
│       ├── anexo.ts
│       └── ia.ts
│
├── assets/
│   ├── icon.png                      # 1024x1024
│   ├── adaptive-icon.png             # 1024x1024 (Android)
│   ├── splash-icon.png               # Splash screen icon
│   └── favicon.png                   # 48x48 (web)
│
├── e2e/                              # Maestro E2E tests
│   ├── login.yaml
│   ├── criar-gasto.yaml
│   └── criar-manutencao.yaml
│
├── app.json                          # Expo config
├── eas.json                          # EAS Build profiles
├── tsconfig.json
├── package.json
├── .env.example
└── .eslintrc.js
```

---

## 13. Codigo de Referencia: API Client

```typescript
// src/api/client.ts

import * as SecureStore from 'expo-secure-store';

const API_BASE = process.env.EXPO_PUBLIC_API_BASE_URL || '';
const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb);
}

function onTokenRefreshed(token: string) {
  refreshSubscribers.forEach(cb => cb(token));
  refreshSubscribers = [];
}

export async function getAccessToken(): Promise<string | null> {
  return SecureStore.getItemAsync(ACCESS_TOKEN_KEY);
}

export async function setTokens(access: string, refresh: string) {
  await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, access);
  await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, refresh);
}

export async function clearTokens() {
  await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY);
  await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
}

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = await SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
  if (!refreshToken) return null;

  const res = await fetch(`${API_BASE}/api/auth/refresh`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${refreshToken}`,
    },
  });

  if (!res.ok) {
    await clearTokens();
    return null;
  }

  const data = await res.json();
  await setTokens(data.token, data.refreshToken);
  return data.token;
}

export async function apiFetch(
  path: string,
  options: RequestInit = {},
): Promise<Response> {
  const token = await getAccessToken();
  const headers: Record<string, string> = {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers as Record<string, string>),
  };

  if (options.body && !(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  let response = await fetch(`${API_BASE}${path}`, { ...options, headers });

  // Auto-refresh on 401
  if (response.status === 401 && !path.includes('/auth/login') && !path.includes('/auth/refresh')) {
    if (!isRefreshing) {
      isRefreshing = true;
      const newToken = await refreshAccessToken();
      isRefreshing = false;

      if (newToken) {
        onTokenRefreshed(newToken);
        headers.Authorization = `Bearer ${newToken}`;
        response = await fetch(`${API_BASE}${path}`, { ...options, headers });
      }
    } else {
      // Wait for refresh to complete
      const newToken = await new Promise<string>(resolve => {
        subscribeTokenRefresh(resolve);
      });
      headers.Authorization = `Bearer ${newToken}`;
      response = await fetch(`${API_BASE}${path}`, { ...options, headers });
    }
  }

  return response;
}

export async function parseJson<T = unknown>(response: Response): Promise<T> {
  const contentType = response.headers.get('content-type') || '';
  if (!contentType.includes('application/json')) {
    throw new Error(
      `Resposta inesperada do servidor (${response.status}).`
    );
  }
  return response.json();
}

export async function parseError(response: Response, fallback: string): Promise<string> {
  try {
    const data = await parseJson<{ message?: string; error?: string }>(response);
    return data?.message || data?.error || fallback;
  } catch {
    return fallback;
  }
}
```

---

## 14. Codigo de Referencia: Hooks

```typescript
// src/hooks/useGastos.ts

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiFetch, parseJson, parseError } from '../api/client';
import type { Gasto, GastoRequest } from '../types/gasto';

export function useGastos(mes?: string, ano?: string, tipo?: string) {
  return useQuery({
    queryKey: ['gastos', { mes, ano, tipo }],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (mes) params.set('mes', mes);
      if (ano) params.set('ano', ano);
      if (tipo) params.set('tipo', tipo);
      const qs = params.toString();
      const path = qs ? `/api/gastos?${qs}` : '/api/gastos';
      const res = await apiFetch(path);
      if (!res.ok) throw new Error(await parseError(res, 'Falha ao carregar gastos.'));
      return parseJson<Gasto[]>(res);
    },
  });
}

export function useCreateGasto() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: GastoRequest) => {
      const res = await apiFetch('/api/gastos', {
        method: 'POST',
        body: JSON.stringify(payload),
      });
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao registrar gasto.'));
      return parseJson<Gasto>(res);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['gastos'] });
    },
  });
}

export function useUpdateGasto() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...payload }: GastoRequest & { id: string }) => {
      const res = await apiFetch(`/api/gastos/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      });
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao atualizar gasto.'));
      return parseJson<Gasto>(res);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['gastos'] });
    },
  });
}

export function useDeleteGasto() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      const res = await apiFetch(`/api/gastos/${id}`, { method: 'DELETE' });
      if (!res.ok) throw new Error(await parseError(res, 'Erro ao remover gasto.'));
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['gastos'] });
    },
  });
}
```

---

## 15. Codigo de Referencia: Stores

```typescript
// src/store/authStore.ts

import { create } from 'zustand';

interface User {
  email: string;
  nome: string;
  condominioId: string;
  nomeCondominio: string;
  roles: string[];
}

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  biometryEnabled: boolean;
  setUser: (user: User | null) => void;
  setLoading: (loading: boolean) => void;
  setBiometryEnabled: (enabled: boolean) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,
  biometryEnabled: false,
  setUser: (user) => set({ user, isAuthenticated: !!user }),
  setLoading: (isLoading) => set({ isLoading }),
  setBiometryEnabled: (biometryEnabled) => set({ biometryEnabled }),
  logout: () => set({ user: null, isAuthenticated: false }),
}));
```

---

## 16. Codigo de Referencia: Componentes

```typescript
// src/components/ui/CurrencyInput.tsx

import { useState } from 'react';
import { TextInput, type TextInputProps } from 'react-native-paper';

interface CurrencyInputProps extends Omit<TextInputProps, 'value' | 'onChangeText'> {
  value: string;
  onChangeValue: (value: string) => void;
}

export function CurrencyInput({ value, onChangeValue, ...props }: CurrencyInputProps) {
  const formatCurrency = (raw: string): string => {
    const digits = raw.replace(/\D/g, '');
    if (!digits) return '';
    const num = parseInt(digits, 10) / 100;
    return num.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  };

  return (
    <TextInput
      {...props}
      value={value}
      keyboardType="numeric"
      onChangeText={(text) => {
        const digits = text.replace(/\D/g, '');
        const num = parseInt(digits || '0', 10) / 100;
        onChangeValue(String(num));
      }}
      onBlur={() => {
        if (value) {
          // Format on blur
        }
      }}
    />
  );
}
```

---

## 17. Configuracao Expo Completa

### 17.1 app.json

```json
{
  "expo": {
    "name": "LiveSindIA",
    "slug": "livesind-mobile",
    "version": "1.0.0",
    "orientation": "portrait",
    "icon": "./assets/icon.png",
    "userInterfaceStyle": "dark",
    "newArchEnabled": true,
    "scheme": "livesind",
    "splash": {
      "image": "./assets/splash-icon.png",
      "resizeMode": "contain",
      "backgroundColor": "#0a0a0f"
    },
    "ios": {
      "supportsTablet": true,
      "bundleIdentifier": "br.com.livesind.app",
      "buildNumber": "1",
      "infoPlist": {
        "NSCameraUsageDescription": "Permitir camera para fotografar manutencoes",
        "NSPhotoLibraryUsageDescription": "Permitir acesso a galeria para upload de fotos",
        "NSFaceIDUsageDescription": "Usar Face ID para login rapido"
      },
      "config": {
        "googleSignIn": {
          "reservedClientId": "com.googleusercontent.apps.7569526-tfrrghq40es98m92r09f4m8i97e930u8"
        }
      }
    },
    "android": {
      "adaptiveIcon": {
        "foregroundImage": "./assets/adaptive-icon.png",
        "backgroundColor": "#0a0a0f"
      },
      "package": "br.com.livesind.app",
      "versionCode": 1,
      "permissions": [
        "CAMERA",
        "READ_EXTERNAL_STORAGE",
        "WRITE_EXTERNAL_STORAGE",
        "RECEIVE_BOOT_COMPLETED",
        "VIBRATE",
        "USE_BIOMETRIC",
        "USE_FINGERPRINT"
      ],
      "googleServicesFile": "./google-services.json"
    },
    "plugins": [
      "expo-router",
      "expo-secure-store",
      "expo-camera",
      "expo-image-picker",
      "expo-document-picker",
      "expo-notifications",
      "expo-local-authentication",
      [
        "expo-updates",
        { "username": "livesind" }
      ]
    ],
    "extra": {
      "eas": {
        "projectId": "your-eas-project-id"
      }
    },
    "updates": {
      "url": "https://u.expo.dev/your-eas-project-id"
    },
    "runtimeVersion": {
      "policy": "appVersion"
    }
  }
}
```

### 17.2 eas.json

```json
{
  "cli": { "version": ">= 14.0.0" },
  "build": {
    "development": {
      "developmentClient": true,
      "distribution": "internal",
      "env": {
        "EXPO_PUBLIC_API_BASE_URL": "http://192.168.1.X:8080"
      }
    },
    "preview": {
      "distribution": "internal",
      "android": { "buildType": "apk" },
      "ios": { "simulator": true },
      "env": {
        "EXPO_PUBLIC_API_BASE_URL": "https://staging.livesind.com.br"
      }
    },
    "production": {
      "android": { "buildType": "app-bundle" },
      "ios": { "autoIncrement": true },
      "env": {
        "EXPO_PUBLIC_API_BASE_URL": "https://api.livesind.com.br"
      }
    }
  },
  "submit": {
    "production": {
      "android": {
        "serviceAccountKeyPath": "./play-store-key.json",
        "track": "production"
      },
      "ios": {
        "appleId": "your@apple.id",
        "ascAppId": "1234567890"
      }
    }
  }
}
```

### 17.3 .env.example

```
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080
EXPO_PUBLIC_GOOGLE_CLIENT_ID=7569526-tfrrghq40es98m92r09f4m8i97e930u8.apps.googleusercontent.com
```

---

## 18. CI/CD e Build Pipeline

### 18.1 GitHub Actions

```yaml
# .github/workflows/mobile-ci.yml
name: Mobile CI

on:
  push:
    branches: [main, develop]
    paths: ['mobile/**']
  pull_request:
    paths: ['mobile/**']

jobs:
  lint-and-test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: mobile
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: mobile/package-lock.json
      - run: npm ci
      - run: npm run typecheck
      - run: npm run lint
      - run: npm test -- --coverage

  build-preview:
    needs: lint-and-test
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: mobile
    steps:
      - uses: actions/checkout@v4
      - uses: expo/expo-github-action@v8
        with:
          eas-version: latest
          token: ${{ secrets.EXPO_TOKEN }}
      - run: npm ci
      - run: eas build --profile preview --platform all --non-interactive

  build-production:
    needs: lint-and-test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: mobile
    steps:
      - uses: actions/checkout@v4
      - uses: expo/expo-github-action@v8
        with:
          eas-version: latest
          token: ${{ secrets.EXPO_TOKEN }}
      - run: npm ci
      - run: eas build --profile production --platform all --non-interactive
```

### 18.2 OTA Updates

```bash
# Deploy hotfix sem ir pra store
cd mobile
eas update --branch production --message "fix: corrige formatacao de moeda"
```

---

## 19. Testes

### 19.1 Estrategia

| Tipo | Ferramenta | O que testa | Cobertura minima |
|---|---|---|---|
| Unitario | Jest | formatters, validators, utils | 90% |
| Componente | Testing Library RN | Forms, cards, listas | 70% |
| Hook | Testing Library RN | useGastos, useAuth, etc. | 80% |
| API mock | MSW | Client, interceptors, refresh | 90% |
| E2E | Maestro | Fluxos completos | Top 5 fluxos |
| Manual | TestFlight + Internal Track | UX, performance real | Cada sprint |

### 19.2 Testes E2E Maestro

```yaml
# e2e/login.yaml
appId: br.com.livesind.app
---
- launchApp
- assertVisible: "Entrar com Email"
- tapOn: "E-mail"
- inputText: "sindico@demo.local"
- tapOn: "Senha"
- inputText: "senha123"
- tapOn: "Entrar com Email"
- assertVisible: "Dashboard"

# e2e/criar-gasto.yaml
appId: br.com.livesind.app
---
- launchApp
- tapOn: "Financeiro"
- tapOn: "Novo gasto"
- tapOn: "Descricao"
- inputText: "Conta de agua maio"
- tapOn: "Valor"
- inputText: "35000"
- tapOn: "Registrar gasto"
- assertVisible: "Gasto registrado com sucesso"
```

### 19.3 Testes de Sincronizacao Cross-Platform

| # | Cenario | Passos | Resultado esperado |
|---|---|---|---|
| 1 | Criar no mobile | Criar gasto no celular → abrir web | Gasto aparece na web |
| 2 | Criar no web | Criar manutencao no web → abrir mobile | Manutencao aparece no mobile |
| 3 | Offline → sync | Criar gasto offline → reconectar | Gasto sincronizado no servidor |
| 4 | Login simultaneo | Login no celular + desktop | Ambos funcionam, mesmos dados |
| 5 | Trocar condominio | Trocar no mobile | Nao afeta sessao do desktop |

---

## 20. Alteracoes no Backend

### 20.1 Fase 1 (Obrigatorias)

| # | Tipo | O que | Detalhes |
|---|---|---|---|
| 1 | AJUSTE | CORS | Aceitar requisicoes de apps mobile (origin pode ser null em mobile nativo; validar via JWT) |
| 2 | VERIFICAR | `PUT/DELETE /api/compromissos/{id}` | Verificar se ja existe, implementar se nao |

### 20.2 Fase 2 (Novas features)

| # | Tipo | O que | Detalhes |
|---|---|---|---|
| 3 | NOVO | `POST /api/auth/refresh` | Refresh token endpoint |
| 4 | NOVO | Tabela `refresh_tokens` | Migration Flyway |
| 5 | NOVO | `POST /api/push/register` | Registrar push token |
| 6 | NOVO | `POST /api/push/unregister` | Remover push token |
| 7 | NOVO | Tabela `push_tokens` | Migration Flyway |
| 8 | NOVO | `GET /api/notificacoes` | Listar notificacoes |
| 9 | NOVO | Tabela `notificacoes` | Migration Flyway |
| 10 | NOVO | PushService | Service que envia push via Expo Push API |
| 11 | NOVO | Event listeners | Disparar push em eventos (compromisso, manutencao, etc.) |

### 20.3 Fase 3 (Melhorias)

| # | Tipo | O que |
|---|---|---|
| 12 | MELHORIA | `@Version` em entidades JPA (optimistic locking) |
| 13 | MELHORIA | WebSocket/SSE para sync real-time |
| 14 | MELHORIA | Paginacao em todas as listas (Page<T>) |

---

## 21. Garantia de Consistencia Cross-Platform

### 21.1 Principios

1. **Verdade unica:** PostgreSQL. Nenhum dado vive apenas no cliente.
2. **Cache descartavel:** React Query cache pode ser limpo sem perda.
3. **Fila persistente:** Mutations offline salvas em AsyncStorage (write-ahead log).
4. **Server-wins:** Em conflito, versao do servidor prevalece.
5. **Multi-sessao:** Desktop e mobile logados simultaneamente sem conflito.
6. **Tenant por token:** Cada JWT carrega condominioId. Trocar no mobile nao afeta desktop.

### 21.2 Diagrama de Consistencia

```
Celular (offline)          Servidor              Desktop
     │                        │                     │
     ├── Cria gasto (local)   │                     │
     │   [queue: gasto_1]     │                     │
     │                        │                     │
     ├── Edita anotacao       │                     │
     │   [queue: anot_1]      │                     │
     │                        │                     │
     │~~~ RECONECTA ~~~~~~~~~~│                     │
     │                        │                     │
     ├── POST /api/gastos ───►│                     │
     │                        ├── salva no PG       │
     │                        │                     │
     ├── PUT /api/anotacoes ─►│                     │
     │                        ├── salva no PG       │
     │                        │                     │
     │   ◄── 200 OK ──────────┤                     │
     │                        │                     │
     │                        │    GET /api/gastos ──┤
     │                        ├── retorna atualizado►│
     │                        │                     │
     ▼                        ▼                     ▼
  Todos os dados consistentes em todas as plataformas
```

---

## 22. Plano de Sprints Detalhado

### Fase 1: MVP (Sprints 1-6, ~6 semanas)

| Sprint | Duracoes | Entregas | Backend |
|---|---|---|---|
| S1 | 1 semana | Projeto Expo, theme, navigation structure, login email/senha, JWT storage, auth guard, `/api/auth/me` check | Ajustar CORS |
| S2 | 1 semana | Dashboard (6 chamadas paralelas, cards metricas, listas resumo, quick actions, pull-to-refresh) | — |
| S3 | 1 semana | Financeiro completo: Gastos CRUD + Recebimentos CRUD, filtros mes/ano/tipo, resumo financeiro | — |
| S4 | 1 semana | Manutencoes CRUD, picker de prestadores, camera/galeria upload de anexos, triagem IA | — |
| S5 | 1 semana | Compromissos (criar, concluir, excluir), Reunioes (CRUD + participantes + gerar ata IA) | Verificar PUT/DELETE compromissos |
| S6 | 1 semana | Moradores + Unidades CRUD, Prestadores CRUD, Anotacoes CRUD com filtros, UX polish, bug fixes | — |

**Entregavel Fase 1:** App funcional publicavel nas stores.

### Fase 2: Diferenciadores (Sprints 7-10, ~4 semanas)

| Sprint | Entregas | Backend |
|---|---|---|
| S7 | Google OAuth (expo-auth-session), Biometria (FaceID/fingerprint) | — |
| S8 | Refresh token (auto-refresh no 401), offline cache persistente | Refresh token endpoint + tabela |
| S9 | Push notifications (registro, recebimento, navegacao ao tocar) | Push service + tabelas + Expo Push API |
| S10 | Assistente IA (chat completo com sugestoes), Analise financeira IA | — |

### Fase 3: Polish e Avancado (Sprints 11-13, ~3 semanas)

| Sprint | Entregas |
|---|---|
| S11 | Selecao de condominio (multi-tenant mobile), Admin panel (usuarios + IA config) |
| S12 | Share sheet (compartilhar ata/relatorio), deep links, OTA updates |
| S13 | Offline mutation queue, optimistic updates, haptic feedback, testes E2E, pre-lancamento |

---

## 23. Custos e Infraestrutura

### 23.1 Contas Necessarias

| Servico | Custo | Uso |
|---|---|---|
| Apple Developer Program | US$ 99/ano | Publicar na App Store |
| Google Play Console | US$ 25 (unico) | Publicar na Play Store |
| Expo (EAS) | Gratis (tier free: 30 builds/mes) | Build na nuvem |
| Expo (EAS Pro) | US$ 99/mes (opcional) | Builds ilimitados + priority |
| Supabase (existente) | Ja pago | PostgreSQL |
| VPS/Railway (existente) | Ja pago | Spring Boot |

### 23.2 Custo Mensal Estimado (Producao)

| Item | Custo/mes |
|---|---|
| Apple Developer | ~US$ 8 |
| Expo EAS Free | US$ 0 |
| Backend (existente) | US$ 0 adicional |
| Push notifications (Expo) | Gratis ate 10k/mes |
| **Total mensal** | **~US$ 8** |

---

## 24. Metricas de Sucesso

| Metrica | Meta | Como medir |
|---|---|---|
| Tempo de login | < 2s | Expo Performance |
| Tempo carga lista (cache) | < 500ms | React Query devtools |
| Tempo carga lista (rede) | < 3s | Network inspector |
| Consistencia cross-platform | 100% | Testes E2E |
| Crash rate | < 1% | EAS Crash Reports |
| Tamanho app (Android) | < 30MB | EAS Build output |
| Tamanho app (iOS) | < 50MB | TestFlight |
| Offline sync success rate | > 99% | Logs de mutation queue |
| Push delivery rate | > 95% | Expo Push receipts |
| App Store rating | > 4.5 | Store reviews |

---

## 25. Riscos e Mitigacoes

| # | Risco | Impacto | Probabilidade | Mitigacao |
|---|---|---|---|---|
| 1 | Token JWT expira no mobile | Re-login frequente | Alta | Refresh token (Fase 2) |
| 2 | Upload grande em rede lenta | Timeout, perda de foto | Media | Chunk upload + progress bar + retry |
| 3 | Cache desatualizado | Usuario ve dado antigo | Media | refetchOnFocus + pull-to-refresh |
| 4 | Conflito de edicao | Dado sobrescrito | Baixa | Server-wins + @Version futuro |
| 5 | App rejeitado na App Store | Atraso no lancamento | Media | Seguir guidelines desde Sprint 1 |
| 6 | Performance em Android antigo | UX ruim, crash | Media | Testar em Android Go, FlashList |
| 7 | Google OAuth rejeitar redirect | Login quebrado | Baixa | Testar com conta real em Sprint 7 |
| 8 | Expo SDK breaking change | Retrabalho | Baixa | Fixar versoes no package.json |
| 9 | Push nao chegar | Usuario perde lembrete | Media | Notificacao local como fallback |
| 10 | LGPD mobile | Nao conformidade | Media | Mesmo fluxo de consentimento da web |

---

## 26. Checklist Pre-Lancamento

### 26.1 App Store (iOS)

- [ ] Icone 1024x1024
- [ ] Screenshots (iPhone 6.7", 6.5", 5.5"; iPad 12.9")
- [ ] Descricao em portugues
- [ ] Privacy policy URL
- [ ] Terms of service URL
- [ ] App Review Information (conta demo)
- [ ] Age rating
- [ ] App Store categories (Utilities / Business)
- [ ] In-app purchases? (nao)

### 26.2 Google Play (Android)

- [ ] Feature graphic 1024x500
- [ ] Screenshots (phone + tablet)
- [ ] Descricao curta (80 chars) + longa
- [ ] Privacy policy URL
- [ ] App signing by Google Play
- [ ] Content rating questionnaire
- [ ] Target audience
- [ ] Data safety section

### 26.3 Tecnico

- [ ] All API endpoints tested on mobile
- [ ] Login email/senha funcional
- [ ] Login Google funcional
- [ ] CRUD gastos funcional
- [ ] CRUD manutencoes funcional
- [ ] Upload foto funcional
- [ ] Cache offline funcional
- [ ] Pull-to-refresh funcional
- [ ] Dark mode correto
- [ ] Safe area em iPhone com notch
- [ ] Keyboard avoiding em todos os forms
- [ ] Performance ok em Android 10+
- [ ] Performance ok em iOS 15+
- [ ] Sem crashes em 24h de uso
- [ ] Tamanho do bundle ok

---

## 27. Glossario

| Termo | Significado |
|---|---|
| SDD | Spec-Driven Development |
| JWT | JSON Web Token |
| OTA | Over-The-Air (atualizacao sem store) |
| EAS | Expo Application Services |
| FIFO | First In, First Out |
| Server-Wins | Em conflito, versao do servidor prevalece |
| Optimistic Update | UI atualiza antes da confirmacao do servidor |
| Stale | Dado em cache que pode estar desatualizado |
| Tenant | Condominio (isolamento de dados) |
| FAB | Floating Action Button |
| SecureStore | Armazenamento criptografado nativo (Keychain/Keystore) |
| Maestro | Framework de testes E2E para mobile |
| MSW | Mock Service Worker (mock de API em testes) |
| FCM | Firebase Cloud Messaging (push Android) |
| APNs | Apple Push Notification service (push iOS) |
| Bundle | Pacote JavaScript compilado do app |
| Deep Link | URL que abre tela especifica do app |
| Skeleton | Placeholder animado que simula conteudo carregando |
| Bottom Sheet | Modal que sobe da parte inferior da tela |
