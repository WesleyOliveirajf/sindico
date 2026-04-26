# Requisitos do MVP

## Perfis de acesso

### Sindico

- acesso total ao condominio
- cria, edita, exclui e consulta registros
- gerencia usuarios do condominio

### Administradora

- pode cadastrar e consultar registros
- permissao configuravel para edicao

### Conselho

- consulta historico
- pode comentar ou validar futuramente

## Requisitos funcionais

### RF01 - Cadastro de condominio

O sistema deve permitir cadastrar condominio com nome, CNPJ opcional, endereco e dados basicos.

### RF02 - Cadastro de usuarios

O sistema deve permitir convidar usuarios e associar perfil de acesso por condominio.

### RF03 - Registro de manutencao

O sistema deve permitir registrar manutencao com:

- titulo
- descricao
- tipo: preventiva ou corretiva
- categoria
- local
- equipamento ou ativo relacionado
- fornecedor
- responsavel interno
- data da ocorrencia
- data da execucao
- custo previsto
- custo realizado
- status
- observacoes

### RF04 - Anexos em manutencao

O sistema deve permitir anexar imagens, PDF, orcamentos, notas fiscais e contratos em registros de manutencao.

### RF05 - Registro de reuniao

O sistema deve permitir registrar reuniao com:

- titulo
- tipo de reuniao
- data e horario
- local ou link
- pauta
- resumo
- decisoes
- participantes
- pendencias geradas

### RF06 - Anexos em reuniao

O sistema deve permitir anexar ata, lista de presenca e documentos complementares.

### RF07 - Consulta historica

O sistema deve permitir consultar registros com filtro por:

- periodo
- tipo
- status
- categoria
- fornecedor
- palavra-chave

### RF08 - Linha do tempo

O sistema deve exibir uma linha do tempo consolidada do condominio com eventos de manutencao e reunioes.

### RF09 - Vinculo entre reuniao e manutencao

O sistema deve permitir relacionar uma decisao de reuniao a uma manutencao futura ou executada.

### RF10 - Auditoria

O sistema deve registrar quem criou e quem alterou cada registro, com data e hora.

## Requisitos nao funcionais

### RNF01 - Seguranca

Cada usuario deve visualizar apenas os condominios aos quais possui acesso.

### RNF02 - Usabilidade

O sistema deve funcionar bem em desktop e mobile.

### RNF03 - Performance

Consultas historicas com filtros comuns devem responder rapidamente mesmo com muitos registros.

### RNF04 - Backup

Anexos e banco devem ter politica clara de backup.

### RNF05 - Rastreabilidade

Exclusoes sensiveis devem ser evitadas; preferir inativacao ou soft delete.

## Regras de negocio

- cada registro pertence a um condominio
- um usuario pode participar de varios condominios
- manutencao pode estar vinculada a um fornecedor
- reuniao pode gerar varias pendencias
- uma pendencia pode originar uma manutencao
- anexos devem ficar ligados ao registro de origem
