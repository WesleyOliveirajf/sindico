package br.com.sindico.app.morador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnidadeRequest(
        @Size(max = 30) String bloco,
        @NotBlank @Size(max = 30) String numero,
        @Size(max = 100) String complemento
) {}
