ALTER TABLE manutencoes
    ADD COLUMN IF NOT EXISTS local VARCHAR(150),
    ADD COLUMN IF NOT EXISTS responsavel_interno VARCHAR(150);

ALTER TABLE reunioes
    ADD COLUMN IF NOT EXISTS link VARCHAR(500),
    ADD COLUMN IF NOT EXISTS pendencias_geradas TEXT;

CREATE INDEX IF NOT EXISTS idx_manutencoes_condominio_created_at ON manutencoes(condominio_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reunioes_condominio_created_at ON reunioes(condominio_id, created_at DESC);
