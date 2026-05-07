package br.com.sindico.app.senha;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SenhaResetController {

    private final SenhaResetService senhaResetService;

    public SenhaResetController(SenhaResetService senhaResetService) {
        this.senhaResetService = senhaResetService;
    }

    // ── Esqueci minha senha ───────────────────────────────────────────────────

    @GetMapping("/esqueci-senha")
    public String esqueciForm() {
        return "esqueci-senha";
    }

    @PostMapping("/esqueci-senha")
    public String esqueciSubmit(
            @RequestParam String email,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String baseUrl = extrairBaseUrl(request);
        senhaResetService.solicitarReset(email, baseUrl);

        // Mesma mensagem independente de email existir (anti-enumeracao)
        redirectAttributes.addFlashAttribute("mensagem",
                "Se este e-mail estiver cadastrado, voce receberá um link para redefinir sua senha em breve.");
        return "redirect:/esqueci-senha";
    }

    // ── Redefinir senha ───────────────────────────────────────────────────────

    @GetMapping("/redefinir-senha")
    public String redefinirForm(@RequestParam String token, Model model) {
        try {
            senhaResetService.validarToken(token);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("erro", ex.getMessage());
            return "redefinir-senha";
        }
        model.addAttribute("token", token);
        return "redefinir-senha";
    }

    @PostMapping("/redefinir-senha")
    public String redefinirSubmit(
            @RequestParam String token,
            @RequestParam String novaSenha,
            @RequestParam String confirmarSenha,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            senhaResetService.redefinirSenha(token, novaSenha, confirmarSenha);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("token", token);
            model.addAttribute("erro", ex.getMessage());
            return "redefinir-senha";
        }

        redirectAttributes.addFlashAttribute("mensagem", "Senha redefinida com sucesso! Faca login.");
        return "redirect:/login";
    }

    private static String extrairBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String server = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = (scheme.equals("http") && port == 80)
                || (scheme.equals("https") && port == 443);
        return defaultPort
                ? scheme + "://" + server
                : scheme + "://" + server + ":" + port;
    }
}
