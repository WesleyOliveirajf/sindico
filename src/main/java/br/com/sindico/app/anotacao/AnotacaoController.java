package br.com.sindico.app.anotacao;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AnotacaoController {

    private final AnotacaoService anotacaoService;

    public AnotacaoController(AnotacaoService anotacaoService) {
        this.anotacaoService = anotacaoService;
    }

    @GetMapping("/anotacoes")
    public String anotacoes(Model model, @ModelAttribute("form") NovaAnotacaoForm form) {
        popularModel(model);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new NovaAnotacaoForm());
        }
        return "anotacoes";
    }

    @PostMapping("/anotacoes")
    public String criar(
            @Valid @ModelAttribute("form") NovaAnotacaoForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            popularModel(model);
            return "anotacoes";
        }

        anotacaoService.criar(form);
        redirectAttributes.addFlashAttribute("mensagem", "Anotacao registrada com sucesso.");
        return "redirect:/anotacoes";
    }

    private void popularModel(Model model) {
        model.addAttribute("condominioNome", anotacaoService.nomeCondominioAtual());
        model.addAttribute("anotacoes", anotacaoService.listarDoCondominioAtual());
    }
}
