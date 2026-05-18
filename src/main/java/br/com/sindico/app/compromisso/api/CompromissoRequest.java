package br.com.sindico.app.compromisso.api;

import br.com.sindico.app.compromisso.CompromissoTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Payload para criar compromisso via API REST.
 * Recebe apenas a data de início (sem hora); fim é preenchido automaticamente ao concluir.
 */
public record CompromissoRequest(
        @NotBlank(message = "Informe um titulo")
        @Size(max = 150, message = "Titulo deve ter no maximo 150 caracteres")
        String titulo,

        @Size(max = 2000, message = "Descricao deve ter no maximo 2000 caracteres")
        String descricao,

        CompromissoTipo tipo,

        @NotNull(message = "Informe a data de inicio")
        LocalDate inicioEm,

        @Size(max = 150, message = "Local deve ter no maximo 150 caracteres")
        String local
) {
}
