CREATE TABLE compromissos (
    id UUID PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT,
    tipo VARCHAR(30) NOT NULL,
    inicio_em TIMESTAMP NOT NULL,
    fim_em TIMESTAMP NOT NULL,
    local VARCHAR(150),
    status VARCHAR(30) NOT NULL,
    google_event_id VARCHAR(255),
    google_sync_status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    google_sync_erro TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_compromissos_inicio_em ON compromissos(inicio_em);
CREATE INDEX idx_compromissos_tipo_status ON compromissos(tipo, status);
