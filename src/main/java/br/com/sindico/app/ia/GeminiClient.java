package br.com.sindico.app.ia;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class GeminiClient implements LLMClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(String apiKey, String model) {
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
        this.apiKey = apiKey;
        this.model = (model != null && !model.isBlank()) ? model : DEFAULT_MODEL;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", userMessage)))
                )
        );

        JsonNode response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) throw new IllegalStateException("Resposta vazia do LLM");
        return response.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
    }

    @Override
    public String getProviderName() { return "GOOGLE_GEMINI"; }

    @Override
    public String getModel() { return model; }
}