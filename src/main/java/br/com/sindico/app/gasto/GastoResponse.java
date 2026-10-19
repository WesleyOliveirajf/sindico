package br.com.sindico.app.gasto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public record GastoResponse(
        UUID id,
        String descricao,
        GastoTipo tipo,
        BigDecimal valor,
        BigDecimal valorParcela,
        LocalDate dataGasto,
        boolean fixo,
        boolean parcelado,
        Integer parcelaAtual,
        Integer parcelaTotal,
        List<ParcelaResponse> parcelas,
        BigDecimal valorPago,
        BigDecimal saldoDevedor,
        String observacoes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static GastoResponse from(Gasto g) {
        return from(g, List.of());
    }

    public static GastoResponse from(Gasto g, List<GastoParcela> parcelas) {
        var respostas = parcelas.stream().map(ParcelaResponse::from).toList();
        var pago = parcelas.stream().filter(GastoParcela::isPaga).map(GastoParcela::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new GastoResponse(
                g.getId(),
                g.getDescricao(),
                g.getTipo(),
                g.getValor(),
                g.getValorParcela(),
                g.getDataGasto(),
                g.isFixo(),
                g.isParcelado(),
                g.getParcelaAtual(),
                g.getParcelaTotal(),
                respostas,
                pago,
                g.getValor().subtract(pago).max(BigDecimal.ZERO),
                g.getObservacoes(),
                g.getCreatedAt(),
                g.getUpdatedAt());
    }
}
