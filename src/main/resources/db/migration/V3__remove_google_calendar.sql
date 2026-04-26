-- Remove colunas e indice relacionados a integracao com Google Calendar
ALTER TABLE compromissos DROP COLUMN IF EXISTS google_event_id;
ALTER TABLE compromissos DROP COLUMN IF EXISTS google_sync_status;
ALTER TABLE compromissos DROP COLUMN IF EXISTS google_sync_erro;
DROP INDEX IF EXISTS idx_compromissos_tipo_status;

-- Recria indice de tipo sem dependencia de status de sync
CREATE INDEX IF NOT EXISTS idx_compromissos_tipo ON compromissos (tipo);
