package br.com.sindico.app.senha;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public String perfil(Model model) {
        model.addAttribute("usuario", perfilService.usuarioAtual());
        return "perfil";
    }

    @PostMapping("/dados")
    public String atualizarDados(
            @RequestParam String nome,
            @RequestParam(required = false) String telefone,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            perfilService.atualizarPerfil(nome, telefone);
            redirectAttributes.addFlashAttribute("mensagem", "Dados atualizados com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erroDados", ex.getMessage());
        }
        return "redirect:/perfil";
    }

    @PostMapping("/senha")
    public String trocarSenha(
            @RequestParam String senhaAtual,
            @RequestParam String novaSenha,
            @RequestParam String confirmarSenha,
            RedirectAttributes redirectAttributes) {
        try {
            perfilService.trocarSenha(senhaAtual, novaSenha, confirmarSenha);
            redirectAttributes.addFlashAttribute("mensagem", "Senha alterada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erroSenha", ex.getMessage());
        }
        return "redirect:/perfil";
    }
}
