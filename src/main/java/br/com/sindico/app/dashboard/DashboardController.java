package br.com.sindico.app.dashboard;

import br.com.sindico.app.compromisso.CompromissoService;
import br.com.sindico.app.compromisso.NovoCompromissoForm;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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

    public DashboardController(CompromissoService compromissoService) {
        this.compromissoService = compromissoService;
    }

    @GetMapping("/")
    public String dashboard(Model model, @ModelAttribute("form") NovoCompromissoForm form) {
        popularModelDashboard(model);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new NovoCompromissoForm());
        }
        return "dashboard";
    }

    @GetMapping("/anotacoes")
    public String anotacoes(Model model) {
        model.addAttribute("condominioNome", "Condominio Piloto");
        model.addAttribute("anotacoes", List.of(
                Map.of(
                        "titulo", "Lembrete de limpeza da caixa d'agua",
                        "categoria", "Manutencao",
                        "descricao", "Confirmar empresa contratada e avisar moradores sobre interrupcao no abastecimento.",
                        "dataReferencia", "Dia 12 as 08:00"
                ),
                Map.of(
                        "titulo", "Revisar contrato da portaria",
                        "categoria", "Administrativo",
                        "descricao", "Checar reajuste, clausulas de cobertura noturna e prazo de renovacao.",
                        "dataReferencia", "Antes da assembleia"
                ),
                Map.of(
                        "titulo", "Separar pauta da reuniao mensal",
                        "categoria", "Comunicacao",
                        "descricao", "Incluir prestacao de contas, fundo de reserva e melhorias no playground.",
                        "dataReferencia", "Quinta-feira"
                )
        ));
        return "anotacoes";
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

        redirectAttributes.addFlashAttribute("mensagem", "Compromisso salvo e sincronizacao iniciada com Google Agenda.");
        return "redirect:/";
    }

    private void popularModelDashboard(Model model) {
        model.addAttribute("totalManutencoes", compromissoService.totalManutencoesAgendadas());
        model.addAttribute("totalReunioes", compromissoService.totalReunioesAgendadas());
        model.addAttribute("totalPendencias", compromissoService.totalPendencias());
        model.addAttribute("condominioNome", "Condominio Piloto");
        model.addAttribute("proximosCompromissos", compromissoService.proximos());
    }
}
