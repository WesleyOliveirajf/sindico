package br.com.sindico.app.morador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MoradorRequest(
        @NotNull UUID unidadeId,
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 150) String email,
        @Size(max = 30) String telefone,
        @NotNull MoradorPapel papel,
        String observacoes
) {}
