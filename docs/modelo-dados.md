# Modelo de Dados

## Entidades principais

### Condominio

- id
- nome
- cnpj
- endereco
- created_at
- updated_at

### Usuario

- id
- nome
- email
- telefone
- status
- created_at

### UsuarioCondominio

- id
- usuario_id
- condominio_id
- perfil
- created_at

### Fornecedor

- id
- condominio_id
- nome
- documento
- telefone
- email
- especialidade

### Ativo

Representa equipamento, sistema ou area que pode exigir manutencao.

- id
- condominio_id
- nome
- tipo
- local
- fabricante
- modelo
- numero_serie
- observacoes

### Manutencao

- id
- condominio_id
- ativo_id
- fornecedor_id
- criado_por
- titulo
- descricao
- tipo
- categoria
- prioridade
- status
- data_ocorrencia
- data_execucao
- custo_previsto
- custo_realizado
- observacoes
- created_at
- updated_at

### Reuniao

- id
- condominio_id
- criado_por
- titulo
- tipo
- data_hora
- local
- pauta
- resumo
- decisoes
- created_at
- updated_at

### ParticipanteReuniao

- id
- reuniao_id
- nome
- cargo
- presente

### Pendencia

- id
- condominio_id
- reuniao_id
- manutencao_id
- titulo
- descricao
- responsavel
- prazo
- status

### Anexo

- id
- condominio_id
- entidade_tipo
- entidade_id
- nome_arquivo
- url_arquivo
- mime_type
- tamanho_bytes
- enviado_por
- created_at

## Relacionamentos principais

- condominio 1:N usuario_condominio
- usuario 1:N usuario_condominio
- condominio 1:N fornecedor
- condominio 1:N ativo
- condominio 1:N manutencao
- condominio 1:N reuniao
- reuniao 1:N participante_reuniao
- reuniao 1:N pendencia
- manutencao 1:N pendencia
- manutencao 1:N anexo
- reuniao 1:N anexo

## Observacoes de modelagem

- anexos foram modelados de forma polimorfica para simplificar o MVP
- pendencia conecta a camada administrativa da reuniao com a camada operacional da manutencao
- ativo e opcional no cadastro de manutencao, mas vale muito a pena para gerar historico tecnico consistente
