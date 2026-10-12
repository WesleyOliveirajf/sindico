package br.com.sindico.app.senha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sindico.app.email.EmailService;
import br.com.sindico.app.usuario.SenhaResetToken;
import br.com.sindico.app.usuario.SenhaResetTokenRepository;
import br.com.sindico.app.usuario.Usuario;
import br.com.sindico.app.usuario.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SenhaResetServiceTest {

    private static final String BASE_URL = "https://app.exemplo.com";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SenhaResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks private SenhaResetService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        usuario.setNome("Maria");
        usuario.setEmail("maria@cond.com");
    }

    // ---- solicitarReset ----

    @Test
    void solicitarResetNormalizaEmailGravaSomenteHashEEnviaLinkComTokenBruto() {
        when(usuarioRepository.findAtivoPorEmailNormalizado("maria@cond.com")).thenReturn(Optional.of(usuario));

        service.solicitarReset("  MARIA@Cond.com ", BASE_URL);

        ArgumentCaptor<SenhaResetToken> tokenSalvo = ArgumentCaptor.forClass(SenhaResetToken.class);
        verify(tokenRepository).invalidarTokensDoUsuario(usuario.getId());
        verify(tokenRepository).save(tokenSalvo.capture());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarResetSenha(
                org.mockito.ArgumentMatchers.eq("maria@cond.com"),
                org.mockito.ArgumentMatchers.eq("Maria"),
                link.capture());

        String tokenBruto = link.getValue().substring((BASE_URL + "/redefinir-senha?token=").length());
        assertThat(link.getValue()).startsWith(BASE_URL + "/redefinir-senha?token=");
        assertThat(tokenBruto).hasSize(43);
        assertThat(tokenSalvo.getValue().getToken()).hasSize(64).isNotEqualTo(tokenBruto);
        assertThat(tokenSalvo.getValue().getUsuario()).isSameAs(usuario);
    }

    @Test
    void solicitarResetDefineExpiracaoDeUmaHora() {
        when(usuarioRepository.findAtivoPorEmailNormalizado(anyString())).thenReturn(Optional.of(usuario));
        LocalDateTime antes = LocalDateTime.now();

        service.solicitarReset("maria@cond.com", BASE_URL);

        ArgumentCaptor<SenhaResetToken> tokenSalvo = ArgumentCaptor.forClass(SenhaResetToken.class);
        verify(tokenRepository).save(tokenSalvo.capture());
        assertThat(tokenSalvo.getValue().getExpiraEm())
                .isBetween(antes.plusMinutes(60), LocalDateTime.now().plusMinutes(60));
    }

    @Test
    void solicitarResetNaoFazNadaENaoRevelaQuandoEmailNaoExiste() {
        when(usuarioRepository.findAtivoPorEmailNormalizado("x@x.com")).thenReturn(Optional.empty());

        service.solicitarReset("x@x.com", BASE_URL);

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).enviarResetSenha(anyString(), anyString(), anyString());
    }

    // ---- validarToken ----

    @Test
    void validarTokenRetornaUsuarioQuandoTokenValido() {
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(tokenValido()));

        assertThat(service.validarToken("token-bruto")).isSameAs(usuario);
    }

    @Test
    void validarTokenConsultaRepositorioPeloHashNuncaPeloTokenBruto() {
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(tokenValido()));

        service.validarToken("token-bruto");

        ArgumentCaptor<String> consulta = ArgumentCaptor.forClass(String.class);
        verify(tokenRepository).findByToken(consulta.capture());
        assertThat(consulta.getValue()).hasSize(64).matches("[0-9a-f]{64}").isNotEqualTo("token-bruto");
    }

    @Test
    void validarTokenFalhaQuandoTokenNaoExiste() {
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validarToken("inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Link invalido ou expirado.");
    }

    @Test
    void validarTokenFalhaQuandoTokenExpirado() {
        SenhaResetToken expirado = tokenValido();
        expirado.setExpiraEm(LocalDateTime.now().minusSeconds(1));
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> service.validarToken("t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solicite um novo");
    }

    @Test
    void validarTokenFalhaQuandoTokenJaUsado() {
        SenhaResetToken usado = tokenValido();
        usado.setUsado(true);
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(usado));

        assertThatThrownBy(() -> service.validarToken("t")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validarTokenNuloOuEmBrancoNaoQuebraEFalhaComoInvalido() {
        when(tokenRepository.findByToken("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validarToken(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validarToken("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    // ---- redefinirSenha ----

    @Test
    void redefinirSenhaGravaHashDaNovaSenhaEMarcaTokenComoUsado() {
        SenhaResetToken token = tokenValido();
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("novaSenha1")).thenReturn("$HASH");

        service.redefinirSenha("t", "novaSenha1", "novaSenha1");

        assertThat(usuario.getSenhaHash()).isEqualTo("$HASH");
        assertThat(token.isUsado()).isTrue();
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).save(token);
    }

    @Test
    void redefinirSenhaRejeitaSenhasDiferentesSemAlterarNada() {
        SenhaResetToken token = tokenValido();
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.redefinirSenha("t", "novaSenha1", "outraSenha1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao conferem");

        assertThat(token.isUsado()).isFalse();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void redefinirSenhaRejeitaTokenExpiradoSemAlterarSenha() {
        SenhaResetToken token = tokenValido();
        token.setExpiraEm(LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.redefinirSenha("t", "novaSenha1", "novaSenha1"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void redefinirSenhaRejeitaTokenInexistente() {
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redefinirSenha("t", "novaSenha1", "novaSenha1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private SenhaResetToken tokenValido() {
        SenhaResetToken token = new SenhaResetToken();
        token.setUsuario(usuario);
        token.setToken("hash");
        token.setExpiraEm(LocalDateTime.now().plusMinutes(30));
        return token;
    }
}
