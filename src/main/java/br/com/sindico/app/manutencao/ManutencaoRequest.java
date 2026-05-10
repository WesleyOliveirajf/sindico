package br.com.sindico.app.manutencao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ManutencaoRequest(
        @NotBlank(message = "Informe um titulo")
        @Size(max = 150, message = "Titulo deve ter no maximo 150 caracteres")
        String titulo,

        @Size(max = 5000, message = "Descricao deve ter no maximo 5000 caracteres")
        String descricao,

        @NotNull(message = "Selecione o tipo")
        ManutencaoTipo tipo,

        @Size(max = 50, message = "Categoria deve ter no maximo 50 caracteres")
        String categoria,

        @Size(max = 150, message = "Local deve ter no maximo 150 caracteres")
        String local,

        UUID ativoId,
        UUID fornecedorId,

        @Size(max = 150, message = "Responsavel interno deve ter no maximo 150 caracteres")
        String responsavelInterno,

        LocalDate dataOcorrencia,
        LocalDate dataExecucao,
        BigDecimal custoPrevisto,
        BigDecimal custoRealizado,

        @NotNull(message = "Selecione o status")
        ManutencaoStatus status,

        @Size(max = 5000, message = "Observacoes deve ter no maximo 5000 caracteres")
        String observacoes
) {
}
