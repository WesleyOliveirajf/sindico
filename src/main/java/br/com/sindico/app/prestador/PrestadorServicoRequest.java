package br.com.sindico.app.prestador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrestadorServicoRequest(
        @NotBlank(message = "Informe nome do prestador")
        @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
        String nome,

        @NotBlank(message = "Informe telefone ou WhatsApp")
        @Size(max = 30, message = "Telefone deve ter no maximo 30 caracteres")
        String telefone,

        @Size(max = 4000, message = "Historico deve ter no maximo 4000 caracteres")
        String historicoServicos
) {
}
