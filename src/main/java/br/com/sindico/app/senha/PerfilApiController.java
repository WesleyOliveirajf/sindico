package br.com.sindico.app.senha;

import br.com.sindico.app.usuario.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
public class PerfilApiController {

    private final PerfilService perfilService;

    public PerfilApiController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public PerfilResponse perfil() {
        return PerfilResponse.from(perfilService.usuarioAtual());
    }

    @PutMapping("/dados")
    public PerfilResponse atualizarDados(@Valid @RequestBody AtualizarDadosRequest request) {
        perfilService.atualizarPerfil(request.nome(), request.telefone());
        return PerfilResponse.from(perfilService.usuarioAtual());
    }

    @PostMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@Valid @RequestBody TrocarSenhaRequest request) {
        perfilService.trocarSenha(request.senhaAtual(), request.novaSenha(), request.confirmarSenha());
    }

    public record AtualizarDadosRequest(
            @NotBlank(message = "Nome e obrigatorio.") String nome,
            String telefone) {}

    public record TrocarSenhaRequest(
            @NotBlank(message = "Informe a senha atual.") String senhaAtual,
            @NotBlank(message = "Informe a nova senha.") String novaSenha,
            @NotBlank(message = "Confirme a nova senha.") String confirmarSenha) {}

    public record PerfilResponse(String nome, String email, String telefone) {
        static PerfilResponse from(Usuario usuario) {
            return new PerfilResponse(usuario.getNome(), usuario.getEmail(), usuario.getTelefone());
        }
    }
}
