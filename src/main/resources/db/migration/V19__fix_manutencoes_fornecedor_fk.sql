-- Corrige FK: manutencoes.fornecedor_id apontava para fornecedores (tabela legada do V1).
-- O sistema migrou para prestadores_servico (V7). A FK errada causava Internal Server Error ao criar manutencao.
ALTER TABLE manutencoes DROP CONSTRAINT IF EXISTS manutencoes_fornecedor_id_fkey;
ALTER TABLE manutencoes
    ADD CONSTRAINT manutencoes_fornecedor_id_fkey
    FOREIGN KEY (fornecedor_id) REFERENCES prestadores_servico(id);