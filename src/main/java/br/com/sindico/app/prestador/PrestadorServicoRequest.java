package br.com.sindico.app.prestador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;

public record PrestadorServicoRequest(
        @NotBlank(message = "Informe nome do prestador")
        @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
        String nome,

        @NotBlank(message = "Informe telefone ou WhatsApp")
        @Size(max = 30, message = "Telefone deve ter no maximo 30 caracteres")
        String telefone,

        @Size(max = 4000, message = "Historico deve ter no maximo 4000 caracteres")
        String historicoServicos,

        @Valid
        List<HistoricoItemRequest> historicoItens
) {

    public record HistoricoItemRequest(
            @NotBlank(message = "Informe o servico realizado")
            @Size(max = 200, message = "Servico deve ter no maximo 200 caracteres")
            String servico,

            @DecimalMin(value = "0.0", inclusive = false, message = "Valor deve ser maior que zero")
            BigDecimal valor
    ) {
    }
}
