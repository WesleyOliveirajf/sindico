package br.com.sindico.app.ia;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ia/config")
public class ConfiguracaoIAApiController {

    private final ConfiguracaoIAService service;
    private final LLMClientFactory llmClientFactory;

    public ConfiguracaoIAApiController(ConfiguracaoIAService service, LLMClientFactory llmClientFactory) {
        this.service = service;
        this.llmClientFactory = llmClientFactory;
    }

    @GetMapping
    public ConfiguracaoIAResponse obter() {
        return service.obter();
    }

    @PostMapping
    public ConfiguracaoIAResponse salvar(@Valid @RequestBody ConfiguracaoIARequest request) {
        return service.salvar(request);
    }

    @PostMapping("/testar")
    public ResponseEntity<Map<String, String>> testar() {
        try {
            LLMClient client = llmClientFactory.criar();
            String resposta = client.chat(
                    "Voce e um assistente de condominio.",
                    "Responda apenas com: OK"
            );
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "provider", client.getProviderName(),
                    "model", client.getModel(),
                    "resposta", resposta
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "erro", "message", e.getMessage()));
        }
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> tratarEstado(IllegalStateException ex) {
        return Map.of("message", ex.getMessage());
    }
}