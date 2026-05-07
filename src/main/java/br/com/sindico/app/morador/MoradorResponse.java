package br.com.sindico.app.morador;

import java.time.LocalDateTime;
import java.util.UUID;

public record MoradorResponse(
        UUID id,
        UUID unidadeId,
        String unidadeRotulo,
        String nome,
        String email,
        String telefone,
        MoradorPapel papel,
        String observacoes,
        boolean ativo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MoradorResponse from(Morador m) {
        return new MoradorResponse(
                m.getId(),
                m.getUnidade().getId(),
                m.getUnidade().getRotulo(),
                m.getNome(),
                m.getEmail(),
                m.getTelefone(),
                m.getPapel(),
                m.getObservacoes(),
                m.isAtivo(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
