package br.com.sindico.app.ia;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class OpenAICompatibleClient implements LLMClient {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String providerName;

    public OpenAICompatibleClient(String baseUrl, String apiKey, String model, String providerName) {
        String url = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : DEFAULT_BASE_URL;
        this.restClient = RestClient.builder().baseUrl(url).build();
        this.apiKey = apiKey;
        this.model = (model != null && !model.isBlank()) ? model : DEFAULT_MODEL;
        this.providerName = providerName;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", 4096
        );

        JsonNode response = restClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) throw new IllegalStateException("Resposta vazia do LLM");
        return response.path("choices").get(0).path("message").path("content").asText();
    }

    @Override
    public String getProviderName() { return providerName; }

    @Override
    public String getModel() { return model; }
}