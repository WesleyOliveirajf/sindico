package br.com.sindico.app.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record TriagemResponse(
        String tipo,
        String categoria,
        String urgencia,
        String tituloSugerido,
        String observacoes,
        String rawJson
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static TriagemResponse parseJson(String json) {
        try {
            String clean = json.startsWith("```") ? stripMarkdown(json) : json;
            JsonNode node = MAPPER.readTree(clean);
            return new TriagemResponse(
                    node.path("tipo").asText("CORRETIVA"),
                    node.path("categoria").asText(""),
                    node.path("urgencia").asText("MEDIA"),
                    node.path("titulaSugerido").asText(""),
                    node.path("observacoes").asText(""),
                    clean
            );
        } catch (Exception e) {
            return new TriagemResponse("CORRETIVA", "", "MEDIA", "", json, json);
        }
    }

    private static String stripMarkdown(String s) {
        return s.replaceAll("```json", "").replaceAll("```", "").trim();
    }
}