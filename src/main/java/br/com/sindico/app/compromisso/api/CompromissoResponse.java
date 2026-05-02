package br.com.sindico.app.compromisso.api;

import br.com.sindico.app.compromisso.Compromisso;
import br.com.sindico.app.compromisso.CompromissoStatus;
import br.com.sindico.app.compromisso.CompromissoTipo;
import java.time.LocalDateTime;
import java.util.UUID;

public record CompromissoResponse(
        UUID id,
        String titulo,
        String descricao,
        CompromissoTipo tipo,
        LocalDateTime inicioEm,
        LocalDateTime fimEm,
        String local,
        CompromissoStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CompromissoResponse from(Compromisso compromisso) {
        return new CompromissoResponse(
                compromisso.getId(),
                compromisso.getTitulo(),
                compromisso.getDescricao(),
                compromisso.getTipo(),
                compromisso.getInicioEm(),
                compromisso.getFimEm(),
                compromisso.getLocal(),
                compromisso.getStatus(),
                compromisso.getCreatedAt(),
                compromisso.getUpdatedAt()
        );
    }
}
