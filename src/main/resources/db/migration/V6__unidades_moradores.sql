CREATE TABLE unidades (
    id UUID PRIMARY KEY,
    condominio_id UUID NOT NULL REFERENCES condominios (id),
    bloco VARCHAR(30) NOT NULL DEFAULT '',
    numero VARCHAR(30) NOT NULL,
    complemento VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_unidade_condominio_bloco_numero UNIQUE (condominio_id, bloco, numero)
);

CREATE INDEX idx_unidades_condominio ON unidades (condominio_id);

CREATE TABLE moradores (
    id UUID PRIMARY KEY,
    unidade_id UUID NOT NULL REFERENCES unidades (id),
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    telefone VARCHAR(30),
    papel VARCHAR(30) NOT NULL,
    observacoes TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_moradores_unidade ON moradores (unidade_id);
