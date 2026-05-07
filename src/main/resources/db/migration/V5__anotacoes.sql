-- Condominio padrao para MVP (id fixo alinhado a app.condominio.default-id)
INSERT INTO condominios (id, nome, cnpj, endereco, created_at, updated_at)
SELECT '00000000-0000-0000-0000-000000000001', 'Condominio Piloto', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM condominios WHERE id = '00000000-0000-0000-0000-000000000001'
);

CREATE TABLE anotacoes (
    id UUID PRIMARY KEY,
    condominio_id UUID NOT NULL REFERENCES condominios (id),
    titulo VARCHAR(150) NOT NULL,
    categoria VARCHAR(50),
    descricao TEXT,
    referencia VARCHAR(200),
    importancia VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    criado_por UUID REFERENCES usuarios (id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_anotacoes_condominio_criado ON anotacoes (condominio_id, created_at DESC);
