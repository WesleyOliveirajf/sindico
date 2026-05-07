package br.com.sindico.app.condominio;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CondominioController {

    private final CondominioService condominioService;

    public CondominioController(CondominioService condominioService) {
        this.condominioService = condominioService;
    }

    @GetMapping("/condominio")
    public String editar(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", condominioService.buscarFormAtual());
        }
        return "condominio";
    }

    @PostMapping("/condominio")
    public String atualizar(
            @Valid @ModelAttribute("form") CondominioForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "condominio";
        }

        try {
            condominioService.atualizar(form);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("condominio.invalido", ex.getMessage());
            return "condominio";
        }

        redirectAttributes.addFlashAttribute("mensagem", "Condominio atualizado com sucesso.");
        return "redirect:/condominio";
    }
}
