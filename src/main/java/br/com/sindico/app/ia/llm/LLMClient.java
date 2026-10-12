package br.com.sindico.app.ia.llm;

public interface LLMClient {
    String chat(String systemPrompt, String userMessage);
    String getProviderName();
    String getModel();
}