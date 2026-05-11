package br.com.sindico.app.prestador;

import java.math.BigDecimal;

public record PrestadorHistoricoItem(
        String servico,
        BigDecimal valor
) {
}
