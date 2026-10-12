package br.com.sindico.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sindico.app.cadastro.CadastroForm;
import br.com.sindico.app.cadastro.CadastroService;
import br.com.sindico.app.condominio.Condominio;
import br.com.sindico.app.condominio.CondominioRepository;
import br.com.sindico.app.security.JwtService;
import br.com.sindico.app.security.UsuarioTenantPrincipal;
import br.com.sindico.app.usuario.Usuario;
import br.com.sindico.app.usuario.UsuarioCondominio;
import br.com.sindico.app.usuario.UsuarioCondominioRepository;
import br.com.sindico.app.usuario.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private CadastroService cadastroService;
    @Mock private JwtService jwtService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioCondominioRepository usuarioCondominioRepository;
    @Mock private CondominioRepository condominioRepository;
    @Mock private GoogleIdentityVerifier googleIdentityVerifier;

    @InjectMocks private AuthService service;

    private UUID usuarioId;
    private UUID condominioId;
    private Usuario usuario;
    private UsuarioTenantPrincipal principal;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        condominioId = UUID.randomUUID();
        usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
        usuario.setNome("Maria Silva");
        usuario.setEmail("maria@cond.com");
        usuario.setSenhaHash("$hash");
        principal = new UsuarioTenantPrincipal(
                usuarioId, condominioId, Set.of(condominioId), "maria@cond.com", "$hash",
                List.of(new SimpleGrantedAuthority("ROLE_SINDICO")));
    }

    // ---- login ----

    @Test
    void loginRetornaPayloadComTokenEDadosDoCondominio() {
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(condominioRepository.findById(condominioId)).thenReturn(Optional.of(condominio("Res. Primavera")));
        when(jwtService.generateToken(principal)).thenReturn("jwt-123");

        Map<String, Object> payload = service.login("maria@cond.com", "senha123");

        assertThat(payload)
                .containsEntry("token", "jwt-123")
                .containsEntry("email", "maria@cond.com")
                .containsEntry("nome", "Maria Silva")
                .containsEntry("condominioId", condominioId.toString())
                .containsEntry("nomeCondominio", "Res. Primavera")
                .containsEntry("roles", List.of("ROLE_SINDICO"));
    }

    @Test
    void loginUsaEmailComoNomeEFallbackDeCondominioQuandoAusentes() {
        usuario.setNome("  ");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(condominioRepository.findById(condominioId)).thenReturn(Optional.empty());
        when(jwtService.generateToken(principal)).thenReturn("jwt");

        Map<String, Object> payload = service.login("maria@cond.com", "senha123");

        assertThat(payload).containsEntry("nome", "maria@cond.com").containsEntry("nomeCondominio", "Condominio");
    }

    @Test
    void loginPropagaFalhaDeCredenciaisSemGerarToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("ruim"));

        assertThatThrownBy(() -> service.login("maria@cond.com", "errada"))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginComPrincipalDeOutroTipoRetornaSomenteEmailSemToken() {
        when(authenticationManager.authenticate(any())).thenReturn(new TestingAuthenticationToken("admin", "x"));

        assertThat(service.login("admin", "x")).containsOnlyKeys("email").containsEntry("email", "admin");
    }

    // ---- registrar ----

    @Test
    void registrarMapeiaCamposETratarFlagsNulasComoFalse() {
        service.registrar(
                new RegisterRequest("Maria", "m@c.com", "Cond", "senha123", "senha123", null, null),
                "10.0.0.1", "JUnit");

        ArgumentCaptor<CadastroForm> form = ArgumentCaptor.forClass(CadastroForm.class);
        verify(cadastroService).cadastrar(form.capture(), eq("10.0.0.1"), eq("JUnit"), eq("api"));
        assertThat(form.getValue().getNome()).isEqualTo("Maria");
        assertThat(form.getValue().getEmail()).isEqualTo("m@c.com");
        assertThat(form.getValue().getNomeCondominio()).isEqualTo("Cond");
        assertThat(form.getValue().getSenha()).isEqualTo("senha123");
        assertThat(form.getValue().isAceitouTermos()).isFalse();
        assertThat(form.getValue().isAceitouMarketing()).isFalse();
    }

    @Test
    void registrarRepassaAceiteDeTermosEMarketing() {
        service.registrar(
                new RegisterRequest("Maria", "m@c.com", "Cond", "senha123", "senha123", true, true), "ip", "ua");

        ArgumentCaptor<CadastroForm> form = ArgumentCaptor.forClass(CadastroForm.class);
        verify(cadastroService).cadastrar(form.capture(), any(), any(), any());
        assertThat(form.getValue().isAceitouTermos()).isTrue();
        assertThat(form.getValue().isAceitouMarketing()).isTrue();
    }

    // ---- sessaoAtual ----

    @Test
    void sessaoAtualAtualizaUltimoAcessoQuandoNuncaAcessou() {
        usuario.setUltimoAcesso(null);
        stubPayload();

        Map<String, Object> payload = service.sessaoAtual(authDe(principal));

        assertThat(payload).containsEntry("email", "maria@cond.com").doesNotContainKey("token");
        verify(usuarioRepository).save(usuario);
        assertThat(usuario.getUltimoAcesso()).isNotNull();
    }

    @Test
    void sessaoAtualAtualizaUltimoAcessoQuandoPassouDoIntervalo() {
        usuario.setUltimoAcesso(LocalDateTime.now().minusMinutes(6));
        stubPayload();

        service.sessaoAtual(authDe(principal));

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void sessaoAtualNaoGravaUltimoAcessoDentroDoIntervaloDeThrottle() {
        LocalDateTime recente = LocalDateTime.now().minusMinutes(1);
        usuario.setUltimoAcesso(recente);
        stubPayload();

        service.sessaoAtual(authDe(principal));

        verify(usuarioRepository, never()).save(any());
        assertThat(usuario.getUltimoAcesso()).isEqualTo(recente);
    }

    @Test
    void sessaoAtualComPrincipalDeOutroTipoRetornaSomenteEmail() {
        assertThat(service.sessaoAtual(new TestingAuthenticationToken("admin", "x")))
                .containsOnlyKeys("email");
    }

    // ---- loginComGoogle ----

    @Test
    void loginComGoogleRejeitaTokenEmBranco() {
        assertThatThrownBy(() -> service.loginComGoogle(new GoogleAuthDto("  ", true, false), "ip", "ua"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token do Google");

        verify(googleIdentityVerifier, never()).verificar(any());
    }

    @Test
    void loginComGoogleExigeAceiteDosTermos() {
        assertThatThrownBy(() -> service.loginComGoogle(new GoogleAuthDto("tok", false, false), "ip", "ua"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Termos de Uso");

        verify(googleIdentityVerifier, never()).verificar(any());
    }

    @Test
    void loginComGoogleRejeitaIdentidadeSemEmail() {
        when(googleIdentityVerifier.verificar("tok")).thenReturn(new GoogleIdentityVerifier.GoogleIdentity(" ", "X"));

        assertThatThrownBy(() -> service.loginComGoogle(new GoogleAuthDto("tok", true, false), "ip", "ua"))
                .isInstanceOf(GoogleIdentityVerifier.TokenInvalidoException.class);
    }

    @Test
    void loginComGoogleEmUsuarioExistenteNaoCadastraDeNovoEEmiteToken() {
        when(googleIdentityVerifier.verificar("tok"))
                .thenReturn(new GoogleIdentityVerifier.GoogleIdentity("  Maria@Cond.com ", "Maria"));
        when(usuarioRepository.findAtivoPorEmailNormalizado("maria@cond.com")).thenReturn(Optional.of(usuario));
        stubVinculo("sindico");
        stubPayload();
        when(jwtService.generateToken(any())).thenReturn("jwt-google");

        Map<String, Object> payload = service.loginComGoogle(new GoogleAuthDto("tok", true, false), "ip", "ua");

        assertThat(payload).containsEntry("token", "jwt-google").containsEntry("roles", List.of("ROLE_SINDICO"));
        verify(cadastroService, never()).cadastrar(any(), any(), any(), any());
    }

    @Test
    void loginComGoogleCadastraUsuarioNovoComNomeDoEmailQuandoSemNome() {
        when(googleIdentityVerifier.verificar("tok"))
                .thenReturn(new GoogleIdentityVerifier.GoogleIdentity("novo@cond.com", null));
        when(usuarioRepository.findAtivoPorEmailNormalizado("novo@cond.com"))
                .thenReturn(Optional.empty(), Optional.of(usuario));
        stubVinculo("sindico");
        stubPayload();
        when(jwtService.generateToken(any())).thenReturn("jwt");

        service.loginComGoogle(new GoogleAuthDto("tok", true, true), "1.1.1.1", "UA");

        ArgumentCaptor<CadastroForm> form = ArgumentCaptor.forClass(CadastroForm.class);
        verify(cadastroService).cadastrar(form.capture(), eq("1.1.1.1"), eq("UA"), eq("google"));
        assertThat(form.getValue().getNome()).isEqualTo("novo");
        assertThat(form.getValue().getNomeCondominio()).isEqualTo("Condomínio de novo");
        assertThat(form.getValue().getSenha()).isEqualTo(form.getValue().getConfirmarSenha());
        assertThat(form.getValue().isAceitouMarketing()).isTrue();
    }

    @Test
    void loginComGoogleFalhaQuandoUsuarioNaoExisteAposCadastro() {
        when(googleIdentityVerifier.verificar("tok"))
                .thenReturn(new GoogleIdentityVerifier.GoogleIdentity("novo@cond.com", "Novo"));
        when(usuarioRepository.findAtivoPorEmailNormalizado("novo@cond.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loginComGoogle(new GoogleAuthDto("tok", true, false), "ip", "ua"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("recuperar usuário");
    }

    @Test
    void loginComGoogleFalhaQuandoUsuarioNaoTemCondominio() {
        when(googleIdentityVerifier.verificar("tok"))
                .thenReturn(new GoogleIdentityVerifier.GoogleIdentity("maria@cond.com", "Maria"));
        when(usuarioRepository.findAtivoPorEmailNormalizado("maria@cond.com")).thenReturn(Optional.of(usuario));
        when(usuarioCondominioRepository.findByUsuario_IdOrderByCondominio_NomeAsc(usuarioId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.loginComGoogle(new GoogleAuthDto("tok", true, false), "ip", "ua"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sem condominio");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginComGoogleUsaPerfilUsuarioQuandoVinculoSemPerfil() {
        when(googleIdentityVerifier.verificar("tok"))
                .thenReturn(new GoogleIdentityVerifier.GoogleIdentity("maria@cond.com", "Maria"));
        when(usuarioRepository.findAtivoPorEmailNormalizado("maria@cond.com")).thenReturn(Optional.of(usuario));
        stubVinculo("  ");
        stubPayload();
        when(jwtService.generateToken(any())).thenReturn("jwt");

        Map<String, Object> payload = service.loginComGoogle(new GoogleAuthDto("tok", true, false), "ip", "ua");

        assertThat(payload).containsEntry("roles", List.of("ROLE_USUARIO"));
    }

    // ---- helpers ----

    private void stubPayload() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(condominioRepository.findById(condominioId)).thenReturn(Optional.of(condominio("Cond")));
    }

    private void stubVinculo(String perfil) {
        UsuarioCondominio vinculo = new UsuarioCondominio();
        vinculo.setCondominio(condominio("Cond"));
        vinculo.setPerfil(perfil);
        when(usuarioCondominioRepository.findByUsuario_IdOrderByCondominio_NomeAsc(usuarioId))
                .thenReturn(List.of(vinculo));
    }

    private Condominio condominio(String nome) {
        Condominio c = new Condominio();
        ReflectionTestUtils.setField(c, "id", condominioId);
        c.setNome(nome);
        return c;
    }

    private Authentication authDe(UsuarioTenantPrincipal p) {
        return new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities());
    }
}
