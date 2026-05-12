CREATE TABLE gastos (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    condominio_id UUID         NOT NULL REFERENCES condominios (id),
    criado_por    UUID         NOT NULL,
    descricao     VARCHAR(255) NOT NULL,
    tipo          VARCHAR(30)  NOT NULL,
    valor         NUMERIC(12, 2) NOT NULL,
    data_gasto    DATE         NOT NULL,
    fixo          BOOLEAN      NOT NULL DEFAULT FALSE,
    observacoes   TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gastos_condominio_data ON gastos (condominio_id, data_gasto DESC);
CREATE INDEX idx_gastos_condominio_tipo ON gastos (condominio_id, tipo);
