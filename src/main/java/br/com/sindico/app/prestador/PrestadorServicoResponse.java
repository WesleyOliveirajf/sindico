package br.com.sindico.app.prestador;

import java.time.LocalDateTime;
import java.util.UUID;

public record PrestadorServicoResponse(
        UUID id,
        String nome,
        String telefone,
        String historicoServicos,
        boolean ativo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PrestadorServicoResponse from(PrestadorServico p) {
        return new PrestadorServicoResponse(
                p.getId(),
                p.getNome(),
                p.getTelefone(),
                p.getHistoricoServicos(),
                p.isAtivo(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
