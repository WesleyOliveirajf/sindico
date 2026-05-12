package br.com.sindico.app.anotacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AnotacaoResponse(
        UUID id,
        String titulo,
        String categoria,
        String descricao,
        String referencia,
        LocalDate dataReferencia,
        AnotacaoImportancia importancia,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AnotacaoResponse from(Anotacao a) {
        return new AnotacaoResponse(
                a.getId(),
                a.getTitulo(),
                a.getCategoria(),
                a.getDescricao(),
                a.getReferencia(),
                a.getDataReferencia(),
                a.getImportancia(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
