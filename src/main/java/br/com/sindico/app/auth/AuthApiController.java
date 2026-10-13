package br.com.sindico.app.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP da autenticacao; regras ficam em {@link AuthService}. */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;

    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    /** Autentica via credenciais JSON e retorna dados do usuario com o token JWT. */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        try {
            return ResponseEntity.ok(authService.login(body.email(), body.senha()));
        } catch (AuthenticationException e) {
            return erro(401, "Credenciais invalidas");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest body, HttpServletRequest request) {
        try {
            authService.registrar(body, request.getRemoteAddr(), request.getHeader("User-Agent"));
            return ResponseEntity.status(201).body(Map.of("message", "Conta criada com sucesso"));
        } catch (IllegalArgumentException ex) {
            return erro(400, ex.getMessage());
        }
    }

    /** Dados do usuario autenticado ou 401; usado pelo front ao carregar a pagina. */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return erro(401, "Nao autenticado");
        }
        return ResponseEntity.ok(authService.sessaoAtual(authentication));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Sessao encerrada"));
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleAuthDto body, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(
                    authService.loginComGoogle(body, request.getRemoteAddr(), request.getHeader("User-Agent")));
        } catch (CadastroPendenteException ex) {
            return ResponseEntity.accepted().body(Map.of("message", ex.getMessage(), "pendente", true));
        } catch (GoogleIdentityVerifier.TokenInvalidoException ex) {
            return erro(401, ex.getMessage());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return erro(400, ex.getMessage());
        }
    }

    private static ResponseEntity<Map<String, Object>> erro(int status, String mensagem) {
        String texto = mensagem != null ? mensagem : "Requisicao invalida";
        return ResponseEntity.status(status).body(Map.of("error", texto, "status", status));
    }
}
