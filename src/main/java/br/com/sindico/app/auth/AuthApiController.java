package br.com.sindico.app.auth;

import br.com.sindico.app.cadastro.CadastroForm;
import br.com.sindico.app.cadastro.CadastroService;
import br.com.sindico.app.security.JwtService;
import br.com.sindico.app.security.UsuarioTenantPrincipal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final CadastroService cadastroService;
    private final JwtService jwtService;

    public AuthApiController(
            AuthenticationManager authenticationManager,
            CadastroService cadastroService,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.cadastroService = cadastroService;
        this.jwtService = jwtService;
    }

    public record LoginRequest(String email, String senha) {}
    public record RegisterRequest(String nome, String email, String nomeCondominio, String senha, String confirmarSenha) {}

    /**
     * Autentica via credenciais JSON, cria sessao e retorna dados do usuario.
     * Usado pelo frontend SPA (Vercel) em chamadas cross-origin com credentials: 'include'.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(body.email(), body.senha()));

            if (auth.getPrincipal() instanceof UsuarioTenantPrincipal principal) {
                String token = jwtService.generateToken(principal);
                return ResponseEntity.ok(Map.of(
                        "email", principal.getEmail(),
                        "condominioId", principal.getCondominioPadraoId().toString(),
                        "token", token
                ));
            }

            return ResponseEntity.ok(Map.of("email", auth.getName()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Credenciais invalidas", "status", 401));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest body) {
        try {
            CadastroForm form = new CadastroForm();
            form.setNome(body.nome());
            form.setEmail(body.email());
            form.setNomeCondominio(body.nomeCondominio());
            form.setSenha(body.senha());
            form.setConfirmarSenha(body.confirmarSenha());

            cadastroService.cadastrar(form);

            return ResponseEntity.status(201).body(Map.of(
                    "message", "Conta criada com sucesso"
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", ex.getMessage(),
                    "status", 400
            ));
        }
    }

    /**
     * Retorna dados do usuario autenticado ou 401 se nao houver sessao ativa.
     * Usado pelo frontend para verificar estado de autenticacao ao carregar a pagina.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Nao autenticado", "status", 401));
        }

        if (authentication.getPrincipal() instanceof UsuarioTenantPrincipal principal) {
            return ResponseEntity.ok(Map.of(
                    "email", principal.getEmail(),
                    "condominioId", principal.getCondominioPadraoId().toString()
            ));
        }

        return ResponseEntity.ok(Map.of("email", authentication.getName()));
    }

    /**
     * Encerra a sessao do usuario.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Sessao encerrada"));
    }
}
