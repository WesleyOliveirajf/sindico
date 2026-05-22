-- =============================================================
-- V18: Tabela de recebimentos + colunas de parcela em gastos
-- =============================================================

-- 1. Nova tabela de recebimentos (entradas financeiras do condomínio)
CREATE TABLE recebimentos (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    condominio_id     UUID           NOT NULL REFERENCES condominios (id),
    criado_por        UUID           NOT NULL,
    descricao         VARCHAR(255)   NOT NULL,
    tipo              VARCHAR(30)    NOT NULL,
    valor             NUMERIC(12, 2) NOT NULL,
    data_recebimento  DATE           NOT NULL,
    observacoes       TEXT,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_recebimentos_condominio_data ON recebimentos (condominio_id, data_recebimento DESC);
CREATE INDEX idx_recebimentos_condominio_tipo ON recebimentos (condominio_id, tipo);

-- 2. Adicionar suporte a parcelas na tabela de gastos
ALTER TABLE gastos ADD COLUMN parcelado     BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE gastos ADD COLUMN parcela_atual INTEGER;
ALTER TABLE gastos ADD COLUMN parcela_total INTEGER;
