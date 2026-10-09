package br.com.sindico.app.senha;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fluxo "esqueci minha senha" consumido pela SPA (rotas /esqueci-senha e /redefinir-senha).
 * Todos os endpoints sao publicos; ver SecurityConfig, ApiBearerEnforcementFilter e AuthRateLimitFilter.
 */
@RestController
@RequestMapping("/api/senha")
public class SenhaResetApiController {

    private final SenhaResetService senhaResetService;
    private final String publicBaseUrl;

    public SenhaResetApiController(
            SenhaResetService senhaResetService,
            @Value("${app.public-base-url}") String publicBaseUrl) {
        this.senhaResetService = senhaResetService;
        this.publicBaseUrl = publicBaseUrl;
    }

    // Sempre 202, com ou sem e-mail cadastrado (anti-enumeracao).
    @PostMapping("/esqueci")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void esqueci(@Valid @RequestBody EsqueciSenhaRequest request) {
        senhaResetService.solicitarReset(request.email(), publicBaseUrl);
    }

    // 204 se o token e valido; 400 com a mensagem do servico caso contrario.
    @GetMapping("/validar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validar(@RequestParam @NotBlank String token) {
        senhaResetService.validarToken(token);
    }

    @PostMapping("/redefinir")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinir(@Valid @RequestBody RedefinirSenhaRequest request) {
        senhaResetService.redefinirSenha(request.token(), request.novaSenha(), request.confirmarSenha());
    }

    public record EsqueciSenhaRequest(
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail invalido.") String email) {}

    public record RedefinirSenhaRequest(
            @NotBlank(message = "Token ausente.") String token,
            @NotBlank(message = "Informe a nova senha.") String novaSenha,
            @NotBlank(message = "Confirme a nova senha.") String confirmarSenha) {}
}
