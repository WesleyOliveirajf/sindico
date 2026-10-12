package br.com.sindico.app.admin;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP da area administrativa; regras ficam em {@link AdminService}. */
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final AdminService adminService;

    public AdminApiController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(adminService.estatisticas());
    }

    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios() {
        return ResponseEntity.ok(adminService.listarUsuarios());
    }

    @PostMapping("/usuarios/{id}/aprovar")
    public ResponseEntity<?> aprovar(@PathVariable UUID id) {
        return responder(id, adminService::aprovar, "Usuario aprovado com sucesso");
    }

    @PostMapping("/usuarios/{id}/rejeitar")
    public ResponseEntity<?> rejeitar(@PathVariable UUID id) {
        return responder(id, adminService::rejeitar, "Usuario desativado com sucesso");
    }

    @PostMapping("/usuarios/{id}/reativar")
    public ResponseEntity<?> reativar(@PathVariable UUID id) {
        return responder(id, adminService::reativar, "Usuario reativado com sucesso");
    }

    private ResponseEntity<?> responder(UUID id, Predicate<UUID> acao, String mensagem) {
        return acao.test(id)
                ? ResponseEntity.ok(Map.of("message", mensagem))
                : ResponseEntity.notFound().build();
    }
}
