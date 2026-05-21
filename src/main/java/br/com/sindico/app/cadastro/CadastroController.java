package br.com.sindico.app.cadastro;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CadastroController {

    private final CadastroService cadastroService;

    public CadastroController(CadastroService cadastroService) {
        this.cadastroService = cadastroService;
    }

    @GetMapping("/cadastro")
    public String form(@ModelAttribute("form") CadastroForm form) {
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(
            @Valid @ModelAttribute("form") CadastroForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model,
            jakarta.servlet.http.HttpServletRequest request) {

        // Validacao extra: senhas conferem (antes de chamar service)
        if (!bindingResult.hasErrors() && !form.getSenha().equals(form.getConfirmarSenha())) {
            bindingResult.rejectValue("confirmarSenha", "senhas.diferentes", "As senhas nao conferem.");
        }

        if (bindingResult.hasErrors()) {
            return "cadastro";
        }

        try {
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            cadastroService.cadastrar(form, ipAddress, userAgent, "thymeleaf");
        } catch (IllegalArgumentException ex) {
            model.addAttribute("erro", ex.getMessage());
            return "cadastro";
        }

        redirectAttributes.addFlashAttribute("mensagem",
                "Conta criada com sucesso! Faca login para continuar.");
        return "redirect:/login";
    }
}
