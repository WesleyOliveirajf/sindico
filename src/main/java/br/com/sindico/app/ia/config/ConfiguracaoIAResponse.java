package br.com.sindico.app.ia;

import java.util.UUID;

public record ConfiguracaoIAResponse(
        UUID id,
        LLMProvider provider,
        boolean configurado,
        String model,
        String baseUrl,
        boolean ativo
) {
    public static ConfiguracaoIAResponse from(ConfiguracaoIA c) {
        return new ConfiguracaoIAResponse(
                c.getId(),
                c.getProvider(),
                c.getApiKeyEnc() != null && !c.getApiKeyEnc().isBlank(),
                c.getModel(),
                c.getBaseUrl(),
                c.isAtivo()
        );
    }

    public static ConfiguracaoIAResponse naoConfigurado() {
        return new ConfiguracaoIAResponse(null, null, false, null, null, false);
    }
}