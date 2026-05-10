package br.com.sindico.app.reuniao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record ReuniaoRequest(
        @NotBlank(message = "Informe um titulo")
        @Size(max = 150, message = "Titulo deve ter no maximo 150 caracteres")
        String titulo,

        @NotNull(message = "Selecione o tipo")
        ReuniaoTipo tipo,

        @NotNull(message = "Informe data e horario")
        LocalDateTime dataHora,

        @Size(max = 150, message = "Local deve ter no maximo 150 caracteres")
        String local,

        @Size(max = 500, message = "Link deve ter no maximo 500 caracteres")
        String link,

        String pauta,
        String resumo,
        String decisoes,
        String pendenciasGeradas,

        @Valid
        List<ParticipanteRequest> participantes
) {
    public record ParticipanteRequest(
            @NotBlank(message = "Nome do participante e obrigatorio")
            @Size(max = 150, message = "Nome do participante deve ter no maximo 150 caracteres")
            String nome,

            @Size(max = 100, message = "Cargo deve ter no maximo 100 caracteres")
            String cargo,

            Boolean presente
    ) {}
}
