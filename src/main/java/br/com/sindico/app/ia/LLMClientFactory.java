package br.com.sindico.app.ia;

import org.springframework.stereotype.Component;

@Component
public class LLMClientFactory {

    private final ConfiguracaoIAService configuracaoIAService;

    public LLMClientFactory(ConfiguracaoIAService configuracaoIAService) {
        this.configuracaoIAService = configuracaoIAService;
    }

    public LLMClient criar() {
        ConfiguracaoIA config = configuracaoIAService.obterEntidade()
                .orElseThrow(() -> new IllegalStateException("IA nao configurada para este condominio"));

        if (!config.isAtivo()) {
            throw new IllegalStateException("IA desativada. Ative nas configuracoes de IA.");
        }

        String apiKey = configuracaoIAService.decryptApiKey(config);

        return switch (config.getProvider()) {
            case OPENAI -> new OpenAICompatibleClient(
                    "https://api.openai.com", apiKey, config.getModel(), "OPENAI");
            case GROQ -> new OpenAICompatibleClient(
                    "https://api.groq.com/openai", apiKey, config.getModel(), "GROQ");
            case OLLAMA -> new OpenAICompatibleClient(
                    config.getBaseUrl() != null ? config.getBaseUrl() : "http://localhost:11434",
                    apiKey, config.getModel(), "OLLAMA");
            case ANTHROPIC -> new AnthropicClient(apiKey, config.getModel());
            case GOOGLE_GEMINI -> new GeminiClient(apiKey, config.getModel());
        };
    }
}