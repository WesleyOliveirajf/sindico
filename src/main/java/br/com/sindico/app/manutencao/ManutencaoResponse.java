package br.com.sindico.app.manutencao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ManutencaoResponse(
        UUID id,
        String titulo,
        String descricao,
        ManutencaoTipo tipo,
        String categoria,
        String local,
        UUID ativoId,
        UUID fornecedorId,
        String responsavelInterno,
        LocalDate dataOcorrencia,
        LocalDate dataExecucao,
        BigDecimal custoPrevisto,
        BigDecimal custoRealizado,
        ManutencaoStatus status,
        String observacoes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ManutencaoResponse from(Manutencao m) {
        return new ManutencaoResponse(
                m.getId(),
                m.getTitulo(),
                m.getDescricao(),
                m.getTipo(),
                m.getCategoria(),
                m.getLocal(),
                m.getAtivoId(),
                m.getFornecedorId(),
                m.getResponsavelInterno(),
                m.getDataOcorrencia(),
                m.getDataExecucao(),
                m.getCustoPrevisto(),
                m.getCustoRealizado(),
                m.getStatus(),
                m.getObservacoes(),
                m.getCreatedAt(),
                m.getUpdatedAt());
    }
}
