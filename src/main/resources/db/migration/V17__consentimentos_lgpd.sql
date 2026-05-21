-- Migração para suporte a LGPD: Registro técnico de consentimentos de usuários
CREATE TABLE usuarios_consentimentos (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    terms_version VARCHAR(20) NOT NULL,
    privacy_policy_version VARCHAR(20) NOT NULL,
    accepted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    origem VARCHAR(50) NOT NULL
);

CREATE INDEX idx_consentimentos_usuario ON usuarios_consentimentos(usuario_id);
