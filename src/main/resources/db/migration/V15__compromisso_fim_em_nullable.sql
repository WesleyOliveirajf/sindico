-- V15: permite fim_em nulo em compromissos
-- fim_em passa a ser preenchido automaticamente ao concluir; nao obrigatorio no cadastro.
ALTER TABLE compromissos ALTER COLUMN fim_em DROP NOT NULL;

