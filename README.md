# LiveSindIA — Gestão Condominial

Aplicação web para centralizar rotinas de condomínios, com histórico, rastreabilidade e organização de informações operacionais.

## Demonstração

[https://sindico-seven.vercel.app](https://sindico-seven.vercel.app)

A demonstração utiliza autenticação. Para conhecer as telas antes de criar uma conta, consulte a documentação do projeto.

## Funcionalidades

- Cadastro e gestão de condomínios
- Autenticação por e-mail, JWT e login social
- Controle de moradores e prestadores de serviço
- Registro de manutenções e atualização de status
- Gestão de reuniões, anotações e compromissos
- Controle de gastos e recebimentos
- Upload e organização de anexos
- Dashboard e consultas por contexto
- Política centralizada de senha e controles de autorização

## Stack

| Camada | Tecnologias |
|---|---|
| Backend | Java 25, Spring Boot 3.5, Spring Web e Validation |
| Segurança | Spring Security, JWT e Google OAuth |
| Persistência | Spring Data JPA, PostgreSQL e Flyway |
| Frontend | React 19, React Router e Vite |
| Operação | Docker e Spring Boot Actuator |
| Testes | Spring Boot Test e Spring Security Test |

## Arquitetura

```text
React/Vite
    │
    ▼
API Spring Boot
    │
    ├── Controllers
    ├── Services
    ├── Repositories
    ├── Segurança/JWT
    └── Migrações Flyway
            │
            ▼
       PostgreSQL
```

O backend mantém separação por responsabilidades e o frontend consome a API por módulos de domínio.

## Estrutura principal

```text
sindico/
├── src/main/          # aplicação Spring Boot
├── src/test/          # testes de serviços, controllers e segurança
├── frontend/          # aplicação React
├── docs/              # requisitos, arquitetura e relatórios de QA
├── scripts/           # utilitários operacionais
├── Dockerfile
└── pom.xml
```

## Executar o backend

Pré-requisitos:

- Java 25
- Maven
- PostgreSQL

Configure as variáveis descritas em `.env.example` e execute:

```bash
mvn spring-boot:run
```

## Executar o frontend

```bash
cd frontend
npm install
npm run dev
```

## Validação

Backend:

```bash
mvn test
```

Frontend:

```bash
cd frontend
npm run lint
npm run build
```

O repositório contém testes para autenticação, autorização, cadastro, manutenção, reuniões, moradores, prestadores, gastos, recebimentos, anexos e outros fluxos principais.

## Documentação

- [Documentação completa](DOCUMENTACAO_DO_SISTEMA.md)
- [Visão do produto](docs/visao-produto.md)
- [Requisitos do MVP](docs/requisitos-mvp.md)
- [Modelo de dados](docs/modelo-dados.md)
- [Relatório de QA](docs/QA-REPORT.md)

## Status

Projeto funcional e em evolução contínua. O README descreve o estado atual do código disponível neste repositório.

## Autor

[Wesley Oliveira](https://github.com/WesleyOliveirajf)
