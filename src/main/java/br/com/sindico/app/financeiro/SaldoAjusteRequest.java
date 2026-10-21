package br.com.sindico.app.financeiro;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SaldoAjusteRequest(
        @NotNull @Min(1) @Max(12) Integer mes,
        @NotNull @Min(2000) @Max(2100) Integer ano,
        @NotNull BigDecimal saldoRealDesejado,
        String observacao
) {}
