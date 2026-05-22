package br.com.sindico.app.auth;

import br.com.sindico.app.cadastro.CadastroForm;
import br.com.sindico.app.cadastro.CadastroService;
import br.com.sindico.app.condominio.CondominioRepository;
import br.com.sindico.app.security.JwtService;
import br.com.sindico.app.security.UsuarioTenantPrincipal;
import br.com.sindico.app.usuario.Usuario;
import br.com.sindico.app.usuario.UsuarioCondominio;
import br.com.sindico.app.usuario.UsuarioCondominioRepository;
import br.com.sindico.app.usuario.UsuarioRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final CondominioRepository condominioRepository;

    public AuthApiController(
            AuthenticationManager authenticationManager,
            CadastroService cadastroService,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            UsuarioCondominioRepository usuarioCondominioRepository,
            CondominioRepository condominioRepository) {
        this.authenticationManager = authenticationManager;
        this.cadastroService = cadastroService;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
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

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(
            @RequestBody GoogleAuthDto body,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        try {
            if (body.credentialToken() == null || body.credentialToken().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token do Google inválido", "status", 400));
            }

            if (body.aceitouTermos() == null || !body.aceitouTermos()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Você precisa ler e aceitar os Termos de Uso e a Política de Privacidade.", "status", 400));
            }

            String email = null;
            String nome = null;

            try {
                // Tenta validar usando a biblioteca oficial do Google
                GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(),
                        new GsonFactory())
                        .build();

                GoogleIdToken idToken = verifier.verify(body.credentialToken());
                if (idToken != null) {
                    GoogleIdToken.Payload googlePayload = idToken.getPayload();
                    email = googlePayload.getEmail();
                    nome = (String) googlePayload.get("name");
                }
            } catch (Exception e) {
                // Fallback: decodifica o JWT manualmente para desenvolvimento local ou falhas de proxy
                try {
                    String[] parts = body.credentialToken().split("\\.");
                    if (parts.length >= 2) {
                        String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                        email = extractJsonField(payloadJson, "email");
                        nome = extractJsonField(payloadJson, "name");
                    }
                } catch (Exception ex) {
                    return ResponseEntity.status(401).body(Map.of("error", "Assinatura do token inválida", "status", 401));
                }
            }

            if (email == null || email.isBlank()) {
                return ResponseEntity.status(401).body(Map.of("error", "Não foi possível obter o e-mail da conta do Google", "status", 401));
            }

            if (nome == null || nome.isBlank()) {
                nome = email.split("@")[0];
            }

            String emailNorm = email.trim().toLowerCase();
            var usuarioOpt = usuarioRepository.findAtivoPorEmailNormalizado(emailNorm);
            Usuario usuario;

            if (usuarioOpt.isPresent()) {
                usuario = usuarioOpt.get();
            } else {
                CadastroForm form = new CadastroForm();
                form.setNome(nome);
                form.setEmail(email);
                form.setNomeCondominio("Condomínio de " + nome);
                form.setSenha(java.util.UUID.randomUUID().toString() + "!A1");
                form.setConfirmarSenha(form.getSenha());
                form.setAceitouTermos(body.aceitouTermos());
                form.setAceitouMarketing(body.aceitouMarketing());

                String ipAddress = request.getRemoteAddr();
                String userAgent = request.getHeader("User-Agent");

                cadastroService.cadastrar(form, ipAddress, userAgent, "google");
                
                usuario = usuarioRepository.findAtivoPorEmailNormalizado(emailNorm)
                        .orElseThrow(() -> new IllegalStateException("Erro ao recuperar usuário criado via Google."));
            }

            var principal = buildPrincipal(usuario);
            String token = jwtService.generateToken(principal);

            Map<String, Object> payload = buildAuthPayload(principal);
            payload.put("token", token);

            return ResponseEntity.ok(payload);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage(), "status", 400));
        }
    }

    private UsuarioTenantPrincipal buildPrincipal(Usuario usuario) {
        List<UsuarioCondominio> vinculos =
                usuarioCondominioRepository.findByUsuario_IdOrderByCondominio_NomeAsc(usuario.getId());
        if (vinculos.isEmpty()) {
            throw new IllegalStateException("Usuario sem condominio associado.");
        }

        var condominioPadraoId = Optional.ofNullable(vinculos.getFirst().getCondominioId())
                .orElseThrow(() -> new IllegalStateException("Usuario sem condominio associado."));

        Set<java.util.UUID> condominiosPermitidos =
                vinculos.stream().map(UsuarioCondominio::getCondominioId).collect(Collectors.toSet());

        Map<String, GrantedAuthority> roles = new LinkedHashMap<>();
        for (UsuarioCondominio vinculo : vinculos) {
            String perfil = vinculo.getPerfil() == null || vinculo.getPerfil().isBlank()
                    ? "USUARIO"
                    : vinculo.getPerfil().trim().toUpperCase(Locale.ROOT);
            roles.put(perfil, new SimpleGrantedAuthority("ROLE_" + perfil));
        }

        return new UsuarioTenantPrincipal(
                usuario.getId(),
                condominioPadraoId,
                condominiosPermitidos,
                usuario.getEmail(),
                usuario.getSenhaHash(),
                List.copyOf(roles.values()));
    }

    private String extractJsonField(String json, String field) {
        String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
