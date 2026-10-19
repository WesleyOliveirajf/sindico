package br.com.sindico.app.gasto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ParcelaResponse(Integer numero, BigDecimal valor, boolean paga, LocalDateTime pagaEm) {
    public static ParcelaResponse from(GastoParcela parcela) {
        return new ParcelaResponse(parcela.getNumero(), parcela.getValor(), parcela.isPaga(), parcela.getPagaEm());
    }
}
