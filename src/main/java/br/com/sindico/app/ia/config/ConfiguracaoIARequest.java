package br.com.sindico.app.ia.config;

import br.com.sindico.app.ia.llm.LLMProvider;

import jakarta.validation.constraints.NotNull;

public record ConfiguracaoIARequest(
        @NotNull LLMProvider provider,
        String apiKey,
        String model,
        String baseUrl,
        boolean ativo
) {}
