package br.com.sindico.app.morador;

import java.util.UUID;

public record UnidadeResponse(
        UUID id,
        String bloco,
        String numero,
        String complemento,
        String rotulo
) {
    public static UnidadeResponse from(Unidade u) {
        return new UnidadeResponse(u.getId(), u.getBloco(), u.getNumero(), u.getComplemento(), u.getRotulo());
    }
}
