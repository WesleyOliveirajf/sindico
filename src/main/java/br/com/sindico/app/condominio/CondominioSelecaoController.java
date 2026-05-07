package br.com.sindico.app.condominio;

import br.com.sindico.app.security.TenantSession;
import br.com.sindico.app.security.UsuarioTenantPrincipal;
import jakarta.servlet.http.HttpSession;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/condominios")
public class CondominioSelecaoController {

    private final CondominioRepository condominioRepository;

    public CondominioSelecaoController(CondominioRepository condominioRepository) {
        this.condominioRepository = condominioRepository;
    }

    @GetMapping("/selecionar")
    public String paginaSelecao(@AuthenticationPrincipal UsuarioTenantPrincipal principal, Model model) {
        if (principal == null || principal.getCondominiosPermitidos().size() <= 1) {
            return "redirect:/";
        }

        List<CondominioOpcao> opcoes =
                condominioRepository.findAllById(principal.getCondominiosPermitidos()).stream()
                        .sorted(Comparator.comparing(Condominio::getNome, String.CASE_INSENSITIVE_ORDER))
                        .map(c -> new CondominioOpcao(c.getId(), c.getNome()))
                        .toList();

        model.addAttribute("condominios", opcoes);
        return "condominios/selecionar";
    }

    @PostMapping("/selecionar")
    public String aplicar(
            @RequestParam UUID condominioId,
            @AuthenticationPrincipal UsuarioTenantPrincipal principal,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null || !principal.getCondominiosPermitidos().contains(condominioId)) {
            redirectAttributes.addFlashAttribute(
                    "erroTipoCondominio", "Condominio nao disponivel para este usuario.");
            return "redirect:/condominios/selecionar";
        }
        session.setAttribute(TenantSession.CONDOMINIO_ID, condominioId);
        return "redirect:/";
    }

    public record CondominioOpcao(UUID id, String nome) {}
}
