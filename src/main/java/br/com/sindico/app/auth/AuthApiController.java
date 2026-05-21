package br.com.sindico.app.auth;

import br.com.sindico.app.cadastro.CadastroForm;
import br.com.sindico.app.cadastro.CadastroService;
import br.com.sindico.app.condominio.CondominioRepository;
import br.com.sindico.app.security.JwtService;
import br.com.sindico.app.security.UsuarioTenantPrincipal;
import br.com.sindico.app.usuario.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
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
    private final UsuarioRepository usuarioRepository;
    private final CondominioRepository condominioRepository;

    public AuthApiController(
            AuthenticationManager authenticationManager,
            CadastroService cadastroService,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            CondominioRepository condominioRepository) {
        this.authenticationManager = authenticationManager;
        this.cadastroService = cadastroService;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.condominioRepository = condominioRepository;
    }

    public record LoginRequest(String email, String senha) {}
    public record RegisterRequest(
            String nome,
            String email,
            String nomeCondominio,
            String senha,
            String confirmarSenha,
            Boolean aceitouTermos,
            Boolean aceitouMarketing
    ) {}

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
                Map<String, Object> payload = buildAuthPayload(principal);
                payload.put("token", token);
                return ResponseEntity.ok(payload);
            }

            return ResponseEntity.ok(Map.of("email", auth.getName()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Credenciais invalidas", "status", 401));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest body,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        try {
            CadastroForm form = new CadastroForm();
            form.setNome(body.nome());
            form.setEmail(body.email());
            form.setNomeCondominio(body.nomeCondominio());
            form.setSenha(body.senha());
            form.setConfirmarSenha(body.confirmarSenha());
            form.setAceitouTermos(body.aceitouTermos() != null && body.aceitouTermos());
            form.setAceitouMarketing(body.aceitouMarketing() != null && body.aceitouMarketing());

            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            cadastroService.cadastrar(form, ipAddress, userAgent, "api");

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
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Nao autenticado", "status", 401));
        }

        if (authentication.getPrincipal() instanceof UsuarioTenantPrincipal principal) {
            // MELHORIA-004: Throttle — atualiza ultimo_acesso apenas se passaram >5 min
            // Evita write no banco em cada chamada ao /api/auth/me (chamado em toda navegação)
            usuarioRepository.findById(principal.getUsuarioId()).ifPresent(u -> {
                LocalDateTime agora = LocalDateTime.now();
                if (u.getUltimoAcesso() == null ||
                        u.getUltimoAcesso().isBefore(agora.minusMinutes(5))) {
                    u.setUltimoAcesso(agora);
                    usuarioRepository.save(u);
                }
            });
            return ResponseEntity.ok(buildAuthPayload(principal));
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

    private Map<String, Object> buildAuthPayload(UsuarioTenantPrincipal principal) {
        String nomeSindico = usuarioRepository.findById(principal.getUsuarioId())
                .map(u -> u.getNome())
                .filter(nome -> nome != null && !nome.isBlank())
                .orElse(principal.getEmail());

        String nomeCondominio = condominioRepository.findById(principal.getCondominioPadraoId())
                .map(c -> c.getNome())
                .filter(nome -> nome != null && !nome.isBlank())
                .orElse("Condominio");

        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", principal.getEmail());
        payload.put("nome", nomeSindico);
        payload.put("condominioId", principal.getCondominioPadraoId().toString());
        payload.put("nomeCondominio", nomeCondominio);
        payload.put("roles", roles);
        return payload;
    }
}
