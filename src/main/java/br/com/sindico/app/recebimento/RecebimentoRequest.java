package br.com.sindico.app.recebimento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecebimentoRequest(

        @NotBlank(message = "Informe a descricao do recebimento")
        @Size(max = 255, message = "Descricao deve ter no maximo 255 caracteres")
        String descricao,

        @NotNull(message = "Selecione o tipo do recebimento")
        RecebimentoTipo tipo,

        @NotNull(message = "Informe o valor do recebimento")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "Informe a data do recebimento")
        LocalDate dataRecebimento,

        @Size(max = 5000, message = "Observacoes deve ter no maximo 5000 caracteres")
        String observacoes
) {}
