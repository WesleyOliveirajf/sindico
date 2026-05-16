CREATE TABLE configuracao_ia (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    condominio_id UUID        NOT NULL UNIQUE REFERENCES condominios(id),
    provider      VARCHAR(30) NOT NULL,
    api_key_enc   TEXT,
    model         VARCHAR(100),
    base_url      TEXT,
    ativo         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
