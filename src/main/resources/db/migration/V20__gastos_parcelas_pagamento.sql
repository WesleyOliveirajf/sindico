CREATE TABLE gasto_parcelas (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gasto_id       UUID NOT NULL REFERENCES gastos (id) ON DELETE CASCADE,
    numero         INTEGER NOT NULL,
    valor          NUMERIC(12, 2) NOT NULL,
    paga           BOOLEAN NOT NULL DEFAULT FALSE,
    paga_em        TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_gasto_parcela_numero UNIQUE (gasto_id, numero),
    CONSTRAINT ck_gasto_parcela_numero_positivo CHECK (numero > 0),
    CONSTRAINT ck_gasto_parcela_valor_positivo CHECK (valor > 0)
);

ALTER TABLE gastos ADD COLUMN valor_parcela NUMERIC(12, 2);

CREATE INDEX idx_gasto_parcelas_gasto ON gasto_parcelas (gasto_id, numero);
