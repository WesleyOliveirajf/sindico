package br.com.sindico.app.compromisso.api;

import br.com.sindico.app.compromisso.Compromisso;
import br.com.sindico.app.compromisso.CompromissoStatus;
import br.com.sindico.app.compromisso.CompromissoTipo;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    /** Retorna true se status == CONCLUIDO. Usado pelo frontend React. */
    public boolean concluido() {
        return status == CompromissoStatus.CONCLUIDO;
    }

    /** Data de início formatada como dd/MM/yyyy — sem hora. */
    public String inicioEmFormatado() {
        if (inicioEm == null) return "";
        return inicioEm.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /** Data/hora da conclusão formatada. */
    public String fimEmFormatado() {
        if (fimEm == null) return "";
        return fimEm.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public static CompromissoResponse from(Compromisso c) {
        return new CompromissoResponse(
                c.getId(),
                c.getTitulo(),
                c.getDescricao(),
                c.getTipo(),
                c.getInicioEm(),
                c.getFimEm(),
                c.getLocal(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
