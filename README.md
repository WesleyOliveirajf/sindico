# Sistema para Historico de Manutencao e Reunioes de Condominios

Este workspace agora contem a definicao inicial de um sistema para ajudar sindicos a registrar, consultar e acompanhar:

- historico de manutencoes
- historico de reunioes
- anexos e evidencias
- pendencias e acompanhamentos

## Objetivo do produto

Centralizar o historico operacional do condominio em um unico lugar, reduzindo perda de informacao, facilitando auditoria e dando rastreabilidade para decisoes e servicos executados.

## MVP proposto

O MVP deve atender quatro frentes:

1. Cadastro do condominio e usuarios responsaveis.
2. Registro de manutencoes preventivas e corretivas.
3. Registro de reunioes ordinarias e extraordinarias.
4. Consulta do historico com filtros, anexos e status.

## Documentos criados

- [docs/visao-produto.md](docs/visao-produto.md)
- [docs/requisitos-mvp.md](docs/requisitos-mvp.md)
- [docs/modelo-dados.md](docs/modelo-dados.md)
- [docs/arquitetura-sugerida.md](docs/arquitetura-sugerida.md)
- [docs/backlog-inicial.md](docs/backlog-inicial.md)
- [database/schema.sql](database/schema.sql)

## Recomendacao tecnica para iniciar rapido

- Frontend: Thymeleaf no proprio Spring Boot para o piloto ou React em uma segunda etapa
- Backend: Java 21 + Spring Boot
- Banco: PostgreSQL em producao e H2 para desenvolvimento rapido
- Persistencia: Spring Data JPA + Hibernate
- Seguranca: Spring Security
- Armazenamento de arquivos: disco local no piloto e S3 em producao
- Hospedagem: Docker + VPS, Railway ou AWS

## Fluxo principal do usuario

1. O sindico acessa o sistema.
2. Cadastra o condominio.
3. Registra uma manutencao ou uma reuniao.
4. Anexa ata, notas fiscais, orcamentos, fotos ou contratos.
5. Consulta o historico por periodo, categoria, fornecedor ou status.

## Proximo passo recomendado

Transformar essa analise em um projeto funcional com:

- autenticacao
- dashboard inicial
- CRUD de manutencoes
- CRUD de reunioes
- upload de anexos
- filtros e busca

## Estrategia recomendada para comecar pequeno e crescer certo

- iniciar com um monolito modular em Spring Boot
- suportar apenas um usuario no piloto para validar fluxo e modelo de dados
- manter separacao por camadas desde o inicio: controller, service, repository e domain
- usar PostgreSQL e migracoes para evitar retrabalho quando o sistema crescer
- deixar preparado para multiusuario e multiconominio, mesmo que o piloto use um unico login
