package br.com.sindico.app.anexo;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnexoResponse(
        UUID id,
        String nomeArquivo,
        String mimeType,
        Long tamanhoBytes,
        LocalDateTime createdAt
) {
    public static AnexoResponse from(Anexo a) {
        return new AnexoResponse(a.getId(), a.getNomeArquivo(), a.getMimeType(), a.getTamanhoBytes(), a.getCreatedAt());
    }
}
