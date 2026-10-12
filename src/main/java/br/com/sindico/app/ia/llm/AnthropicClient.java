package br.com.sindico.app.ia;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class AnthropicClient implements LLMClient {

    private static final String BASE_URL = "https://api.anthropic.com";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-6";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AnthropicClient(String apiKey, String model) {
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
        this.apiKey = apiKey;
        this.model = (model != null && !model.isBlank()) ? model : DEFAULT_MODEL;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 4096,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                )
        );

        JsonNode response = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) throw new IllegalStateException("Resposta vazia do LLM");
        return response.path("content").get(0).path("text").asText();
    }

    @Override
    public String getProviderName() { return "ANTHROPIC"; }

    @Override
    public String getModel() { return model; }
}