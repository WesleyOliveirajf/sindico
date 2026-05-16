package br.com.sindico.app.ia;

import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ia")
public class AIApiController {

    private final AIService aiService;

    public AIApiController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String mensagem = body.get("mensagem");
        if (mensagem == null || mensagem.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mensagem nao pode ser vazia"));
        }
        String resposta = aiService.chat(mensagem.trim());
        return ResponseEntity.ok(Map.of("resposta", resposta));
    }

    @PostMapping("/reuniao/{id}/ata")
    public ResponseEntity<Map<String, String>> gerarAta(@PathVariable UUID id) {
        String ata = aiService.gerarAta(id);
        return ResponseEntity.ok(Map.of("ata", ata));
    }

    @GetMapping("/gastos/analise")
    public ResponseEntity<Map<String, String>> analisarGastos(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano) {
        String analise = aiService.analisarGastos(mes, ano);
        return ResponseEntity.ok(Map.of("analise", analise));
    }

    @PostMapping("/manutencao/triar")
    public ResponseEntity<TriagemResponse> triarManutencao(@RequestBody Map<String, String> body) {
        String descricao = body.get("descricao");
        if (descricao == null || descricao.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(aiService.triarManutencao(descricao.trim()));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> tratarEstado(IllegalStateException ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> tratarNaoEncontrado(EntityNotFoundException ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> tratarErro(Exception ex) {
        return Map.of("message", "Erro ao processar requisicao IA: " + ex.getMessage());
    }
}