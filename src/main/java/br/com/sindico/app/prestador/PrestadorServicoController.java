package br.com.sindico.app.prestador;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PrestadorServicoController {

    private final PrestadorServicoService prestadorServicoService;

    public PrestadorServicoController(PrestadorServicoService prestadorServicoService) {
        this.prestadorServicoService = prestadorServicoService;
    }

    @GetMapping("/prestadores")
    public String prestadores(Model model, @ModelAttribute("form") NovoPrestadorForm form) {
        popularModel(model);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new NovoPrestadorForm());
        }
        return "prestadores";
    }

    @PostMapping("/prestadores")
    public String criar(
            @Valid @ModelAttribute("form") NovoPrestadorForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            popularModel(model);
            return "prestadores";
        }

        prestadorServicoService.criar(form);
        redirectAttributes.addFlashAttribute("mensagem", "Prestador cadastrado com sucesso.");
        return "redirect:/prestadores";
    }

    @PostMapping("/prestadores/{prestadorId}")
    public String atualizar(
            @PathVariable String prestadorId,
            @Valid @ModelAttribute("formEdicao") AtualizarPrestadorForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Dados invalidos para atualizacao do prestador.");
            return "redirect:/prestadores";
        }

        try {
            prestadorServicoService.atualizar(java.util.UUID.fromString(prestadorId), form);
            redirectAttributes.addFlashAttribute("mensagem", "Prestador atualizado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Identificador de prestador invalido.");
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        }

        return "redirect:/prestadores";
    }

    @PostMapping("/prestadores/{prestadorId}/inativar")
    public String inativar(
            @PathVariable String prestadorId,
            RedirectAttributes redirectAttributes) {
        try {
            prestadorServicoService.inativar(java.util.UUID.fromString(prestadorId));
            redirectAttributes.addFlashAttribute("mensagem", "Prestador inativado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Identificador de prestador invalido.");
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        }

        return "redirect:/prestadores";
    }

    private void popularModel(Model model) {
        model.addAttribute("condominioNome", prestadorServicoService.nomeCondominioAtual());
        model.addAttribute("prestadores", prestadorServicoService.listarDoCondominioAtual());
    }
}
