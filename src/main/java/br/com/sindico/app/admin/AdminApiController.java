package br.com.sindico.app.admin;

import br.com.sindico.app.condominio.CondominioRepository;
import br.com.sindico.app.usuario.Usuario;
import br.com.sindico.app.usuario.UsuarioCondominio;
import br.com.sindico.app.usuario.UsuarioCondominioRepository;
import br.com.sindico.app.usuario.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final CondominioRepository condominioRepository;

    public AdminApiController(
            UsuarioRepository usuarioRepository,
            UsuarioCondominioRepository usuarioCondominioRepository,
            CondominioRepository condominioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.condominioRepository = condominioRepository;
    }

    /** Retorna metricas gerais da aplicacao. */
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        long totalUsuarios = usuarioRepository.count();
        long pendentes = usuarioRepository.countByStatus("pendente");
        long ativos = usuarioRepository.countByStatus("ativo");
        long inativos = usuarioRepository.countByStatus("inativo");
        long totalCondominios = condominioRepository.count();
        long onlineAgora = usuarioRepository.countOnlineRecentemente(
                LocalDateTime.now().minusMinutes(15));

        return ResponseEntity.ok(Map.of(
                "totalUsuarios", totalUsuarios,
                "pendentes", pendentes,
                "ativos", ativos,
                "inativos", inativos,
                "totalCondominios", totalCondominios,
                "onlineAgora", onlineAgora));
    }

    /** Lista todos os usuarios com seus dados de condominio. */
    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAllByOrderByCreatedAtDesc();

        List<Map<String, Object>> result = usuarios.stream().map(u -> {
            List<UsuarioCondominio> vinculos =
                    usuarioCondominioRepository.findByUsuario_IdOrderByCondominio_NomeAsc(u.getId());

            String condominioNome = vinculos.isEmpty()
                    ? ""
                    : vinculos.getFirst().getCondominio().getNome();
            String perfil = vinculos.isEmpty()
                    ? ""
                    : vinculos.getFirst().getPerfil();

            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", u.getId().toString());
            dto.put("nome", u.getNome());
            dto.put("email", u.getEmail());
            dto.put("status", u.getStatus());
            dto.put("condominioNome", condominioNome);
            dto.put("perfil", perfil);
            dto.put("createdAt", u.getCreatedAt());
            dto.put("ultimoAcesso", u.getUltimoAcesso());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /** Aprova um usuario pendente, alterando seu status para 'ativo'. */
    @PostMapping("/usuarios/{id}/aprovar")
    public ResponseEntity<?> aprovar(@PathVariable UUID id) {
        return usuarioRepository.findById(id)
                .map(u -> {
                    u.setStatus("ativo");
                    usuarioRepository.save(u);
                    return ResponseEntity.ok(Map.of("message", "Usuario aprovado com sucesso"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Rejeita ou desativa um usuario. */
    @PostMapping("/usuarios/{id}/rejeitar")
    public ResponseEntity<?> rejeitar(@PathVariable UUID id) {
        return usuarioRepository.findById(id)
                .map(u -> {
                    u.setStatus("inativo");
                    usuarioRepository.save(u);
                    return ResponseEntity.ok(Map.of("message", "Usuario desativado com sucesso"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Reativa um usuario inativo. */
    @PostMapping("/usuarios/{id}/reativar")
    public ResponseEntity<?> reativar(@PathVariable UUID id) {
        return usuarioRepository.findById(id)
                .map(u -> {
                    u.setStatus("ativo");
                    usuarioRepository.save(u);
                    return ResponseEntity.ok(Map.of("message", "Usuario reativado com sucesso"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
