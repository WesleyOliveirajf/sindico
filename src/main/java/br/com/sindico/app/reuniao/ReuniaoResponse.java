package br.com.sindico.app.reuniao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReuniaoResponse(
        UUID id,
        String titulo,
        ReuniaoTipo tipo,
        LocalDateTime dataHora,
        String local,
        String link,
        String pauta,
        String resumo,
        String decisoes,
        String pendenciasGeradas,
        List<ParticipanteResponse> participantes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record ParticipanteResponse(String nome, String cargo, boolean presente) {}

    public static ReuniaoResponse from(Reuniao reuniao, List<ParticipanteReuniao> participantes) {
        return new ReuniaoResponse(
                reuniao.getId(),
                reuniao.getTitulo(),
                reuniao.getTipo(),
                reuniao.getDataHora(),
                reuniao.getLocal(),
                reuniao.getLink(),
                reuniao.getPauta(),
                reuniao.getResumo(),
                reuniao.getDecisoes(),
                reuniao.getPendenciasGeradas(),
                participantes.stream().map(p -> new ParticipanteResponse(p.getNome(), p.getCargo(), p.isPresente())).toList(),
                reuniao.getCreatedAt(),
                reuniao.getUpdatedAt());
    }
}
