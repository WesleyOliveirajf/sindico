package br.com.sindico.app.gasto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record GastoResponse(
        UUID id,
        String descricao,
        GastoTipo tipo,
        BigDecimal valor,
        LocalDate dataGasto,
        boolean fixo,
        String observacoes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static GastoResponse from(Gasto g) {
        return new GastoResponse(
                g.getId(),
                g.getDescricao(),
                g.getTipo(),
                g.getValor(),
                g.getDataGasto(),
                g.isFixo(),
                g.getObservacoes(),
                g.getCreatedAt(),
                g.getUpdatedAt());
    }
}
