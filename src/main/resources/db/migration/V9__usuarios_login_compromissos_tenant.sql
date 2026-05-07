-- Login por cliente: senha BCrypt armazenada no usuario que vincula ao condominio
ALTER TABLE usuarios
    ADD COLUMN senha_hash VARCHAR(255);

-- Compromissos por condominio (isolamento tenant)
ALTER TABLE compromissos
    ADD COLUMN condominio_id UUID;

UPDATE compromissos
SET condominio_id = '00000000-0000-0000-0000-000000000001'
WHERE condominio_id IS NULL;

ALTER TABLE compromissos
    ALTER COLUMN condominio_id SET NOT NULL;

ALTER TABLE compromissos
    ADD CONSTRAINT fk_compromissos_condominio FOREIGN KEY (condominio_id) REFERENCES condominios (id);

CREATE INDEX idx_compromissos_condominio_inicio_em ON compromissos (condominio_id, inicio_em);

-- Usuario inicial (senha: MudarSenha123!) associado ao condominio padrao do MVP
INSERT INTO usuarios (id, nome, email, telefone, status, senha_hash, created_at)
SELECT 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
       'Sindico Demonstracao',
       'sindico@demo.local',
       NULL,
       'ativo',
       '$2b$10$4mJGtVOR97pw2nLBqNeJE.lKoU/7wB2Cx4mbuDCpXHxkxjNESmal2',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'sindico@demo.local');

INSERT INTO usuarios_condominios (id, usuario_id, condominio_id, perfil, created_at)
SELECT 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaab1',
       'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
       '00000000-0000-0000-0000-000000000001',
       'SINDICO',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM usuarios_condominios WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaab1');
