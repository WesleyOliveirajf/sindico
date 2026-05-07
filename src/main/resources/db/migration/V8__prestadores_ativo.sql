ALTER TABLE prestadores_servico
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_prestadores_servico_condominio_ativo_nome
    ON prestadores_servico (condominio_id, ativo, nome);
