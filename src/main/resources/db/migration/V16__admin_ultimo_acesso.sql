-- Adiciona coluna para rastrear ultimo acesso do usuario (usado pelo painel admin)
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ultimo_acesso TIMESTAMP WITH TIME ZONE;
