# Arquitetura Sugerida

## Stack recomendada para MVP

### Opcao recomendada

- Java 21 + Spring Boot 3
- Spring Web para a API e controladores MVC
- Spring Data JPA + Hibernate para persistencia
- PostgreSQL como banco principal
- Flyway para migracoes de banco
- Spring Security para autenticacao e autorizacao
- Thymeleaf para telas server-side no piloto ou React depois
- Storage local para anexos no piloto e S3 quando entrar em producao
- Docker para empacotamento e deploy

## Motivos da recomendacao

- Spring Boot escala melhor como base de projeto corporativo
- monolito modular permite comecar simples sem perder organizacao
- banco relacional atende muito bem historico, filtros e auditoria
- Flyway evita caos em alteracoes de schema
- a troca de storage local para S3 e natural quando o volume crescer

## Estrategia para piloto e escalabilidade

### Fase de piloto

- um unico usuario autenticado
- interface simples no proprio backend com Thymeleaf
- upload de arquivos em pasta local controlada
- deploy unico com aplicacao + banco

### Fase de crescimento

- liberar multiusuario por perfis
- separar frontend se houver necessidade real
- mover anexos para S3 ou compativel
- adicionar cache e observabilidade
- escalar horizontalmente a aplicacao se o volume justificar

## Modulos da aplicacao

### Autenticacao e autorizacao

- login
- usuario administrador no piloto
- multiusuario e perfis na segunda fase
- vinculo com condominio
- controle por perfil

### Cadastro base

- condominio
- usuarios
- fornecedores
- ativos

### Historico de manutencao

- listagem
- cadastro
- detalhes
- anexos
- filtros

### Historico de reunioes

- listagem
- cadastro
- detalhes
- participantes
- pendencias
- anexos

### Consulta e indicadores

- timeline consolidada
- manutencoes por status
- pendencias em aberto
- custo por periodo

## Estrutura sugerida de telas

- login
- dashboard
- condominios
- manutencoes
- reunioes
- pendencias
- fornecedores
- ativos
- configuracoes

## Estrategia de implantacao

### Fase 1

- autenticacao
- cadastro de condominio
- CRUD de manutencao
- CRUD de reuniao
- upload de anexos
- piloto com usuario unico
- interface server-side simples

### Fase 2

- pendencias
- timeline unificada
- dashboard com indicadores
- permissao mais refinada
- multiusuario

### Fase 3

- notificacoes
- renovacao automatica de manutencoes preventivas
- exportacao PDF
- assinatura ou validacao de atas

## Estrutura de pacotes sugerida no backend

- br.com.sindico.app.config
- br.com.sindico.app.auth
- br.com.sindico.app.condominio
- br.com.sindico.app.manutencao
- br.com.sindico.app.reuniao
- br.com.sindico.app.pendencia
- br.com.sindico.app.anexo
- br.com.sindico.app.shared
