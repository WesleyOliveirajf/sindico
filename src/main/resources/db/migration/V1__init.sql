CREATE TABLE condominios (
    id UUID PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    cnpj VARCHAR(18),
    endereco TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    telefone VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'ativo',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuarios_condominios (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    condominio_id UUID NOT NULL REFERENCES condominios(id),
    perfil VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE fornecedores (
    id UUID PRIMARY KEY,
    condominio_id UUID NOT NULL REFERENCES condominios(id),
    nome VARCHAR(150) NOT NULL,
    documento VARCHAR(30),
    telefone VARCHAR(30),
    email VARCHAR(150),
    especialidade VARCHAR(100)
);

CREATE TABLE ativos (
    id UUID PRIMARY KEY,
    condominio_id UUID NOT NULL REFERENCES condominios(id),
    nome VARCHAR(150) NOT NULL,
    tipo VARCHAR(100),
    local VARCHAR(150),
    fabricante VARCHAR(100),
    modelo VARCHAR(100),
    numero_serie VARCHAR(100),
    observacoes TEXT
);

CREATE TABLE manutencoes (
    id UUID PRIMARY KEY,
    condominio_id UUID NOT NULL REFERENCES condominios(id),
    ativo_id UUID REFERENCES ativos(id),
    fornecedor_id UUID REFERENCES fornecedores(id),
    criado_por UUID NOT NULL REFERENCES usuarios(id),
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT,
    tipo VARCHAR(30) NOT NULL,
    categoria VARCHAR(50),
    prioridade VARCHAR(30),
    status VARCHAR(30) NOT NULL,
    data_ocorrencia DATE,
    data_execucao DATE,
    custo_previsto NUMERIC(12,2),
    custo_realizado NUMERIC(12,2),
    observacoes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reunioes (
    id UUID PRIMARY KEY,
    condominio_id UUID NOT NULL REFERENCES condominios(id),
    criado_por UUID NOT NULL REFERENCES usuarios(id),
    titulo VARCHAR(150) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    local VARCHAR(150),
    pauta TEXT,
    resumo TEXT,
    decisoes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE participantes_reuniao (
    id UUID PRIMARY KEY,
    reuniao_id UUID NOT NULL REFERENCES reunioes(id),
    nome VARCHAR(150) NOT NULL,
    cargo VARCHAR(100),
    presente BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE pendencias (
    id UUID PRIMARY KEY,
    condominio_id UUID NOT NULL REFERENCES condominios(id),
    reuniao_id UUID REFERENCES reunioes(id),
    manutencao_id UUID REFERENCES manutencoes(id),
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT,
    responsavel VARCHAR(150),
    prazo DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'aberta'
);

CREATE TABLE anexos (
    id UUID PRIMARY KEY,
    condominio_id UUID NOT NULL REFERENCES condominios(id),
    entidade_tipo VARCHAR(30) NOT NULL,
    entidade_id UUID NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    url_arquivo TEXT NOT NULL,
    mime_type VARCHAR(100),
    tamanho_bytes BIGINT,
    enviado_por UUID REFERENCES usuarios(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_manutencoes_condominio_status ON manutencoes(condominio_id, status);
CREATE INDEX idx_manutencoes_data_ocorrencia ON manutencoes(data_ocorrencia);
CREATE INDEX idx_reunioes_condominio_data ON reunioes(condominio_id, data_hora);
CREATE INDEX idx_pendencias_condominio_status ON pendencias(condominio_id, status);
CREATE INDEX idx_anexos_entidade ON anexos(entidade_tipo, entidade_id);