package br.com.sindico.app.morador;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MoradorController {

    private final MoradorGestaoService moradorGestaoService;

    public MoradorController(MoradorGestaoService moradorGestaoService) {
        this.moradorGestaoService = moradorGestaoService;
    }

    @GetMapping("/moradores")
    public String moradores(
            Model model,
            @ModelAttribute("formUnidade") NovaUnidadeForm formUnidade,
            @ModelAttribute("formMorador") NovoMoradorForm formMorador) {
        popularModel(model);
        if (!model.containsAttribute("formUnidade")) {
            model.addAttribute("formUnidade", new NovaUnidadeForm());
        }
        if (!model.containsAttribute("formMorador")) {
            model.addAttribute("formMorador", new NovoMoradorForm());
        }
        return "moradores";
    }

    @PostMapping("/moradores/unidades")
    public String criarUnidade(
            @Valid @ModelAttribute("formUnidade") NovaUnidadeForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            popularModel(model);
            model.addAttribute("formMorador", new NovoMoradorForm());
            return "moradores";
        }
        try {
            moradorGestaoService.criarUnidade(form);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("unidade.duplicada", ex.getMessage());
            popularModel(model);
            model.addAttribute("formMorador", new NovoMoradorForm());
            return "moradores";
        }
        redirectAttributes.addFlashAttribute("mensagem", "Unidade cadastrada com sucesso.");
        return "redirect:/moradores";
    }

    @PostMapping("/moradores")
    public String criarMorador(
            @Valid @ModelAttribute("formMorador") NovoMoradorForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            popularModel(model);
            model.addAttribute("formUnidade", new NovaUnidadeForm());
            return "moradores";
        }
        try {
            moradorGestaoService.criarMorador(form);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("morador.erro", ex.getMessage());
            popularModel(model);
            model.addAttribute("formUnidade", new NovaUnidadeForm());
            return "moradores";
        }
        redirectAttributes.addFlashAttribute("mensagem", "Morador cadastrado com sucesso.");
        return "redirect:/moradores";
    }

    private void popularModel(Model model) {
        model.addAttribute("condominioNome", moradorGestaoService.nomeCondominioAtual());
        model.addAttribute("unidades", moradorGestaoService.listarUnidades());
        model.addAttribute("moradores", moradorGestaoService.listarMoradoresAtivos());
    }
}
