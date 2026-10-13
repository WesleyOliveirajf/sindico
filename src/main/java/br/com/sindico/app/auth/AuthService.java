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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

/** Casos de uso de autenticacao da API: login, cadastro, sessao atual e login com Google. */
@Service
public class AuthService {

    private static final long INTERVALO_ULTIMO_ACESSO_MIN = 5;
    private static final String STATUS_ATIVO = "ativo";
    private static final String STATUS_PENDENTE = "pendente";

    private final AuthenticationManager authenticationManager;
    private final CadastroService cadastroService;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final CondominioRepository condominioRepository;
    private final GoogleIdentityVerifier googleIdentityVerifier;

    public AuthService(
            AuthenticationManager authenticationManager,
            CadastroService cadastroService,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            UsuarioCondominioRepository usuarioCondominioRepository,
            CondominioRepository condominioRepository,
            GoogleIdentityVerifier googleIdentityVerifier) {
        this.authenticationManager = authenticationManager;
        this.cadastroService = cadastroService;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.condominioRepository = condominioRepository;
        this.googleIdentityVerifier = googleIdentityVerifier;
    }

    /** @throws org.springframework.security.core.AuthenticationException credenciais invalidas */
    public Map<String, Object> login(String email, String senha) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, senha));

        if (auth.getPrincipal() instanceof UsuarioTenantPrincipal principal) {
            return payloadComToken(principal);
        }
        return Map.of("email", auth.getName());
    }

    public void registrar(RegisterRequest body, String ipAddress, String userAgent) {
        CadastroForm form = new CadastroForm();
        form.setNome(body.nome());
        form.setEmail(body.email());
        form.setNomeCondominio(body.nomeCondominio());
        form.setSenha(body.senha());
        form.setConfirmarSenha(body.confirmarSenha());
        form.setAceitouTermos(Boolean.TRUE.equals(body.aceitouTermos()));
        form.setAceitouMarketing(Boolean.TRUE.equals(body.aceitouMarketing()));
        cadastroService.cadastrar(form, ipAddress, userAgent, "api");
    }

    /** Dados do usuario autenticado; atualiza ultimo_acesso com throttle para evitar write a cada navegacao. */
    public Map<String, Object> sessaoAtual(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UsuarioTenantPrincipal principal) {
            registrarUltimoAcesso(principal.getUsuarioId());
            return montarPayload(principal);
        }
        return Map.of("email", authentication.getName());
    }

    public Map<String, Object> loginComGoogle(GoogleAuthDto body, String ipAddress, String userAgent) {
        if (body.credentialToken() == null || body.credentialToken().isBlank()) {
            throw new IllegalArgumentException("Token do Google inválido");
        }
        if (!Boolean.TRUE.equals(body.aceitouTermos())) {
            throw new IllegalArgumentException(
                    "Você precisa ler e aceitar os Termos de Uso e a Política de Privacidade.");
        }

        var identidade = googleIdentityVerifier.verificar(body.credentialToken());
        String email = identidade.email();
        if (email == null || email.isBlank()) {
            throw new GoogleIdentityVerifier.TokenInvalidoException(
                    "Não foi possível obter o e-mail da conta do Google");
        }
        String nome = identidade.nome() == null || identidade.nome().isBlank()
                ? email.split("@")[0]
                : identidade.nome();

        String emailNorm = email.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmailNormalizado(emailNorm)
                .orElseGet(() -> cadastrarViaGoogle(emailNorm, email, nome, body, ipAddress, userAgent));
        exigirContaAtiva(usuario);

        return payloadComToken(montarPrincipal(usuario));
    }

    /** Cadastros novos nascem pendentes; so contas ativas recebem token. */
    private static void exigirContaAtiva(Usuario usuario) {
        if (STATUS_PENDENTE.equals(usuario.getStatus())) {
            throw new CadastroPendenteException();
        }
        if (!STATUS_ATIVO.equals(usuario.getStatus())) {
            throw new IllegalArgumentException("Conta inativa. Fale com o administrador.");
        }
    }

    private Usuario cadastrarViaGoogle(
            String emailNorm, String email, String nome, GoogleAuthDto body, String ipAddress, String userAgent) {
        CadastroForm form = new CadastroForm();
        form.setNome(nome);
        form.setEmail(email);
        form.setNomeCondominio("Condomínio de " + nome);
        form.setSenha(UUID.randomUUID() + "!A1");
        form.setConfirmarSenha(form.getSenha());
        form.setAceitouTermos(body.aceitouTermos());
        form.setAceitouMarketing(body.aceitouMarketing());
        cadastroService.cadastrar(form, ipAddress, userAgent, "google");

        return usuarioRepository.findByEmailNormalizado(emailNorm)
                .orElseThrow(() -> new IllegalStateException("Erro ao recuperar usuário criado via Google."));
    }

    private void registrarUltimoAcesso(UUID usuarioId) {
        usuarioRepository.findById(usuarioId).ifPresent(u -> {
            LocalDateTime agora = LocalDateTime.now();
            if (u.getUltimoAcesso() == null
                    || u.getUltimoAcesso().isBefore(agora.minusMinutes(INTERVALO_ULTIMO_ACESSO_MIN))) {
                u.setUltimoAcesso(agora);
                usuarioRepository.save(u);
            }
        });
    }

    private Map<String, Object> payloadComToken(UsuarioTenantPrincipal principal) {
        Map<String, Object> payload = montarPayload(principal);
        payload.put("token", jwtService.generateToken(principal));
        return payload;
    }

    private Map<String, Object> montarPayload(UsuarioTenantPrincipal principal) {
        String nomeSindico = usuarioRepository.findById(principal.getUsuarioId())
                .map(Usuario::getNome)
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

    private UsuarioTenantPrincipal montarPrincipal(Usuario usuario) {
        List<UsuarioCondominio> vinculos =
                usuarioCondominioRepository.findByUsuario_IdOrderByCondominio_NomeAsc(usuario.getId());
        if (vinculos.isEmpty() || vinculos.getFirst().getCondominioId() == null) {
            throw new IllegalStateException("Usuario sem condominio associado.");
        }

        Set<UUID> condominiosPermitidos =
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
                vinculos.getFirst().getCondominioId(),
                condominiosPermitidos,
                usuario.getEmail(),
                usuario.getSenhaHash(),
                List.copyOf(roles.values()));
    }
}
