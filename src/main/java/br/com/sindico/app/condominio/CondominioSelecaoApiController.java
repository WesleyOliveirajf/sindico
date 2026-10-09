package br.com.sindico.app.condominio;

import br.com.sindico.app.security.UsuarioTenantPrincipal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Condominios que o usuario pode acessar. A SPA escolhe o ativo enviando o cabecalho X-Condominio-Id
 * (validado em SecurityTenantAccessor).
 */
@RestController
@RequestMapping("/api/condominios")
public class CondominioSelecaoApiController {

    private final CondominioRepository condominioRepository;

    public CondominioSelecaoApiController(CondominioRepository condominioRepository) {
        this.condominioRepository = condominioRepository;
    }

    @GetMapping
    public List<CondominioOpcao> listar(@AuthenticationPrincipal UsuarioTenantPrincipal principal) {
        if (principal == null) {
            return List.of();
        }
        return condominioRepository.findAllById(principal.getCondominiosPermitidos()).stream()
                .sorted(Comparator.comparing(Condominio::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(c -> new CondominioOpcao(c.getId(), c.getNome()))
                .toList();
    }

    public record CondominioOpcao(UUID id, String nome) {}
}
