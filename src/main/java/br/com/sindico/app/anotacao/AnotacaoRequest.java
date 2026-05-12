package br.com.sindico.app.anotacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AnotacaoRequest(
        @NotBlank @Size(max = 150) String titulo,
        @Size(max = 50) String categoria,
        String descricao,
        @Size(max = 200) String referencia,
        LocalDate dataReferencia,
        @NotNull AnotacaoImportancia importancia
) {}
