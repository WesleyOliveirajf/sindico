-- Data opcional informada pelo sindico para a anotacao (ex.: quando ocorreu o fato)
ALTER TABLE anotacoes
    ADD COLUMN data_referencia DATE;
