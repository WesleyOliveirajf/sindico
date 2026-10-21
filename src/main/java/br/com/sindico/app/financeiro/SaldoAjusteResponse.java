package br.com.sindico.app.financeiro;

import java.math.BigDecimal;

public record SaldoAjusteResponse(
        BigDecimal totalGastos,
        BigDecimal totalRecebimentos,
        BigDecimal saldoCalculado,
        BigDecimal ajusteSaldo,
        BigDecimal saldoReal,
        String observacao
) {}
