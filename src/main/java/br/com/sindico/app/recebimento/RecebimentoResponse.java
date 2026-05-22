package br.com.sindico.app.recebimento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecebimentoResponse(
        UUID id,
        String descricao,
        RecebimentoTipo tipo,
        BigDecimal valor,
        LocalDate dataRecebimento,
        String observacoes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RecebimentoResponse from(Recebimento r) {
        return new RecebimentoResponse(
                r.getId(),
                r.getDescricao(),
                r.getTipo(),
                r.getValor(),
                r.getDataRecebimento(),
                r.getObservacoes(),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
