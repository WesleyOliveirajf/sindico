CREATE TABLE prestadores_servico (
    id UUID PRIMARY KEY,
    condominio_id UUID NOT NULL REFERENCES condominios (id),
    nome VARCHAR(150) NOT NULL,
    telefone VARCHAR(30) NOT NULL,
    historico_servicos TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prestadores_servico_condominio_nome ON prestadores_servico (condominio_id, nome);
