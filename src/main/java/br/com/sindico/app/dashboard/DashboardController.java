package br.com.sindico.app.dashboard;

import br.com.sindico.app.anotacao.AnotacaoService;
import br.com.sindico.app.compromisso.CompromissoService;
import br.com.sindico.app.compromisso.NovoCompromissoForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DashboardController {

    private final CompromissoService compromissoService;
    private final AnotacaoService anotacaoService;

    public DashboardController(CompromissoService compromissoService, AnotacaoService anotacaoService) {
        this.compromissoService = compromissoService;
        this.anotacaoService = anotacaoService;
    }

    @GetMapping("/")
    public String dashboard(Model model, @ModelAttribute("form") NovoCompromissoForm form) {
        popularModelDashboard(model);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new NovoCompromissoForm());
        }
        return "dashboard";
    }

    @PostMapping("/compromissos")
    public String criarCompromisso(
            @Valid @ModelAttribute("form") NovoCompromissoForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            popularModelDashboard(model);
            return "dashboard";
        }

        try {
            compromissoService.criar(form);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("data.invalida", ex.getMessage());
            popularModelDashboard(model);
            return "dashboard";
        }

        redirectAttributes.addFlashAttribute("mensagem", "Compromisso salvo com sucesso.");
        return "redirect:/";
    }

    private void popularModelDashboard(Model model) {
        model.addAttribute("totalManutencoes", compromissoService.totalManutencoesAgendadas());
        model.addAttribute("totalReunioes", compromissoService.totalReunioesAgendadas());
        model.addAttribute("totalPendencias", compromissoService.totalPendencias());
        model.addAttribute("condominioNome", anotacaoService.nomeCondominioAtual());
        model.addAttribute("proximosCompromissos", compromissoService.proximos());
    }
}
