# Plano Base de Tasks - Melhorias de Frontend

## Objetivo
Este documento organiza as melhorias do frontend com prioridade e status para execucao paralela por agentes.

## Como usar (agentes)
1. Antes de iniciar, marque seu nome em `Responsavel`.
2. Ao iniciar uma task, altere `Status` para `EM ANDAMENTO`.
3. Ao finalizar, altere `Status` para `CONCLUIDA` e preencha `Evidencia`.
4. Nao altere task de outro agente sem alinhamento.
5. Se houver bloqueio, marque `BLOQUEADA` e descreva em `Observacoes`.

## Legenda
- Prioridade:
  - `P0` = critico (alto impacto no usuario final)
  - `P1` = importante
  - `P2` = melhoria evolutiva
- Status:
  - `PENDENTE`
  - `EM ANDAMENTO`
  - `BLOQUEADA`
  - `CONCLUIDA`

## Quadro de Tasks

| ID | Task | Prioridade | Status | Responsavel | Dependencias | Evidencia | Observacoes |
|---|---|---|---|---|---|---|---|
| FE-001 | Implementar roteamento com `react-router` e URLs por modulo (`/compromissos`, `/gastos`, etc.) | P0 | CONCLUIDA | Codex |  | `frontend/src/main.jsx`, `frontend/src/App.jsx`, `frontend/package.json` | Rotas ativas com redirecionamento padrao e fallback |
| FE-002 | Criar layout base com navegacao consistente (header/sidebar), mantendo responsividade mobile | P0 | CONCLUIDA | Codex | FE-001 | `frontend/src/App.jsx`, `frontend/src/App.css` | Sidebar fixa no desktop e drawer no mobile com overlay e toggle |
| FE-003 | Unificar cliente HTTP (`api.js`) e estrategia de autenticacao (JWT/cookie) com tratamento padrao de 401 | P0 | CONCLUIDA | Codex |  | `frontend/src/api.js`, `frontend/src/App.jsx` | `apiFetch` unificado com credentials include, evento global para 401 e limpeza de token |
| FE-004 | Padronizar estados de pagina: loading, empty state, erro com retry e feedback de sucesso | P0 | CONCLUIDA | Codex | FE-002 | `frontend/src/components/PageFeedback.jsx`, `frontend/src/App.css`, `frontend/src/CompromissosPage.jsx`, `frontend/src/AnotacoesPage.jsx`, `frontend/src/GastosPage.jsx`, `frontend/src/MoradoresPage.jsx`, `frontend/src/PrestadoresPage.jsx`, `frontend/src/ManutencoesPage.jsx`, `frontend/src/ReunioesPage.jsx` | Padrao de estados aplicado nas telas principais com retry e feedback de sucesso |
| FE-005 | Substituir `confirm()` nativo por modal de confirmacao acessivel reutilizavel | P1 | CONCLUIDA | Codex | FE-002 | `frontend/src/components/ConfirmDialog.jsx`, `frontend/src/App.css`, `frontend/src/CompromissosPage.jsx`, `frontend/src/AnotacoesPage.jsx`, `frontend/src/GastosPage.jsx` | Modal reutilizavel com ESC, foco inicial e fechamento por overlay aplicado em todos os pontos com `confirm()` |
| FE-006 | Criar componentes base de UI (`Button`, `Input`, `Card`, `Modal`, `Alert`) para reduzir duplicacao | P1 | CONCLUIDA | Codex | FE-002 | `frontend/src/components/ui/Button.jsx`, `frontend/src/components/ui/Input.jsx`, `frontend/src/components/ui/Card.jsx`, `frontend/src/components/ui/Modal.jsx`, `frontend/src/components/ui/Alert.jsx`, `frontend/src/components/ConfirmDialog.jsx`, `frontend/src/components/PageFeedback.jsx`, `frontend/src/CompromissosPage.jsx`, `frontend/src/App.css` | Componentes base criados e adotados em fluxos de confirmacao, feedback e formulario de compromissos |
| FE-007 | Remover estilos inline mais repetidos e consolidar em classes CSS reutilizaveis | P1 | PENDENTE |  | FE-006 |  |  |
| FE-008 | Melhorar formularios com validacoes de UX (telefone, moeda BRL, datas, mensagens por campo) | P1 | PENDENTE |  | FE-006 |  |  |
| FE-009 | Adicionar filtros persistentes por URL e busca nas telas de maior volume (Moradores, Prestadores, Gastos) | P1 | PENDENTE |  | FE-001 |  |  |
| FE-010 | Adicionar resumo com KPIs nas telas principais (pendencias, gastos do mes, reunioes proximas) | P2 | PENDENTE |  | FE-004 |  |  |
| FE-011 | Melhorias de acessibilidade: foco visivel, navegacao por teclado, `aria-live` para alertas | P1 | PENDENTE |  | FE-006 |  |  |
| FE-012 | Revisar copy/rotulos para linguagem mais clara ao sindico (acoes, erros, vazios) | P2 | PENDENTE |  | FE-004 |  |  |

## Checklist de Qualidade por Task (marcar ao concluir)
- [ ] Testado em desktop
- [ ] Testado em mobile
- [ ] Sem regressao de autenticacao
- [ ] Sem regressao de navegacao
- [ ] Mensagens de erro claras para usuario final
- [ ] Build frontend (`npm run build`) sem erros

## Registro de Execucao
Use este bloco para historico rapido de entregas:

| Data | Agente | Task ID | Acao |
|---|---|---|---|
| 2026-05-18 | Codex | FE-001 | Implementacao de roteamento com URLs por modulo concluida |
| 2026-05-18 | Codex | FE-002 | Implementacao de layout base com header/sidebar responsivo concluida |
| 2026-05-18 | Codex | FE-003 | Unificacao do cliente HTTP e tratamento global de 401 concluido |
| 2026-05-18 | Codex | FE-004 | Estados padrao finalizados e aplicados em todas as telas principais |
| 2026-05-18 | Codex | FE-005 | Substituicao de `confirm()` nativo por modal acessivel reutilizavel concluida |
| 2026-05-18 | Codex | FE-006 | Componentes base de UI criados e aplicados nos fluxos principais |
