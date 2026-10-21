CREATE TABLE saldo_ajuste_periodo (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    condominio_id   UUID         NOT NULL REFERENCES condominios (id) ON DELETE CASCADE,
    ano             INT          NOT NULL CHECK (ano >= 2000 AND ano <= 2100),
    mes             INT          NOT NULL CHECK (mes >= 1 AND mes <= 12),
    valor           NUMERIC(15, 2) NOT NULL,
    observacao      TEXT,
    atualizado_por  UUID,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_saldo_ajuste_periodo_condominio_ano_mes UNIQUE (condominio_id, ano, mes)
);

CREATE INDEX idx_saldo_ajuste_periodo_condominio ON saldo_ajuste_periodo (condominio_id);
