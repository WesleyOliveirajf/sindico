package br.com.sindico.app.prestador;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PrestadorServicoResponse(
        UUID id,
        String nome,
        String telefone,
        String historicoServicos,
        List<PrestadorHistoricoItem> historicoItens,
        boolean ativo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PrestadorServicoResponse from(PrestadorServico p, List<PrestadorHistoricoItem> historicoItens) {
        return new PrestadorServicoResponse(
                p.getId(),
                p.getNome(),
                p.getTelefone(),
                p.getHistoricoServicos(),
                historicoItens,
                p.isAtivo(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
