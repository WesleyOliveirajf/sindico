package br.com.sindico.app.gasto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoRequest(

        @NotBlank(message = "Informe a descricao do gasto")
        @Size(max = 255, message = "Descricao deve ter no maximo 255 caracteres")
        String descricao,

        @NotNull(message = "Selecione o tipo do gasto")
        GastoTipo tipo,

        @NotNull(message = "Informe o valor do gasto")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "Informe a data do gasto")
        LocalDate dataGasto,

        boolean fixo,

        @Size(max = 5000, message = "Observacoes deve ter no maximo 5000 caracteres")
        String observacoes
) {}
