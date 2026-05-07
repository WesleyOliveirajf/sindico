-- updated_at para usuarios (auditoria de troca de senha)
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Tokens de reset de senha: UUID single-use, expira em 1h
CREATE TABLE senha_reset_tokens (
    id          UUID PRIMARY KEY,
    usuario_id  UUID        NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    token       VARCHAR(64) NOT NULL UNIQUE,
    expira_em   TIMESTAMP   NOT NULL,
    usado       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_senha_reset_tokens_token     ON senha_reset_tokens (token);
CREATE INDEX idx_senha_reset_tokens_usuario   ON senha_reset_tokens (usuario_id);
CREATE INDEX idx_senha_reset_tokens_expira_em ON senha_reset_tokens (expira_em);
