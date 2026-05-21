package br.com.sindico.app.cadastro;

import br.com.sindico.app.condominio.Condominio;
import br.com.sindico.app.condominio.CondominioRepository;
import br.com.sindico.app.usuario.Usuario;
import br.com.sindico.app.usuario.UsuarioCondominio;
import br.com.sindico.app.usuario.UsuarioCondominioRepository;
import br.com.sindico.app.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do CadastroService.
 * Cobre happy path, validações de senha e e-mail duplicado.
 */
@ExtendWith(MockitoExtension.class)
class CadastroServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CondominioRepository condominioRepository;

    @Mock
    private UsuarioCondominioRepository usuarioCondominioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CadastroService cadastroService;

    private CadastroForm formValido;

    @BeforeEach
    void setUp() {
        formValido = new CadastroForm();
        formValido.setNome("Maria Silva");
        formValido.setEmail("maria@cond.com");
        formValido.setNomeCondominio("Residencial Primavera");
        formValido.setSenha("senha123");
        formValido.setConfirmarSenha("senha123");
    }

    // -----------------------------------------------------------------------
    // Happy Path
    // -----------------------------------------------------------------------

    @Test
    void should_cadastrar_successfully_when_all_data_is_valid() {
        when(usuarioRepository.existsByEmailNormalizado(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$HASH");

        Usuario usuarioSalvo = new Usuario();
        usuarioSalvo.setEmail("maria@cond.com");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioSalvo);

        Condominio condSalvo = new Condominio();
        condSalvo.setNome("Residencial Primavera");
        when(condominioRepository.save(any(Condominio.class))).thenReturn(condSalvo);

        when(usuarioCondominioRepository.save(any(UsuarioCondominio.class))).thenReturn(new UsuarioCondominio());

        assertThatCode(() -> cadastroService.cadastrar(formValido)).doesNotThrowAnyException();

        verify(usuarioRepository).save(any(Usuario.class));
        verify(condominioRepository).save(any(Condominio.class));
        verify(usuarioCondominioRepository).save(any(UsuarioCondominio.class));
    }

    @Test
    void should_hash_password_before_saving() {
        when(usuarioRepository.existsByEmailNormalizado(anyString())).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$BCRYPT_HASH");
        Usuario u = new Usuario(); u.setEmail("maria@cond.com");
        when(usuarioRepository.save(any())).thenReturn(u);
        Condominio c = new Condominio(); c.setNome("X");
        when(condominioRepository.save(any())).thenReturn(c);
        when(usuarioCondominioRepository.save(any())).thenReturn(new UsuarioCondominio());

        cadastroService.cadastrar(formValido);

        verify(passwordEncoder).encode("senha123");
    }

    @Test
    void should_normalize_email_to_lowercase() {
        formValido.setEmail("MARIA@COND.COM");
        when(usuarioRepository.existsByEmailNormalizado("maria@cond.com")).thenReturn(false);
        Usuario u = new Usuario(); u.setEmail("maria@cond.com");
        when(usuarioRepository.save(any())).thenReturn(u);
        Condominio c = new Condominio(); c.setNome("X");
        when(condominioRepository.save(any())).thenReturn(c);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(usuarioCondominioRepository.save(any())).thenReturn(new UsuarioCondominio());

        assertThatCode(() -> cadastroService.cadastrar(formValido)).doesNotThrowAnyException();
        verify(usuarioRepository).existsByEmailNormalizado("maria@cond.com");
    }

    // -----------------------------------------------------------------------
    // Validações de e-mail duplicado
    // -----------------------------------------------------------------------

    @Test
    void should_throw_when_email_already_exists() {
        when(usuarioRepository.existsByEmailNormalizado("maria@cond.com")).thenReturn(true);

        assertThatThrownBy(() -> cadastroService.cadastrar(formValido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ja existe uma conta com este e-mail");

        verify(usuarioRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // Validações de senha
    // -----------------------------------------------------------------------

    @Test
    void should_throw_when_password_too_short() {
        formValido.setSenha("abc1");
        formValido.setConfirmarSenha("abc1");
        when(usuarioRepository.existsByEmailNormalizado(anyString())).thenReturn(false);

        assertThatThrownBy(() -> cadastroService.cadastrar(formValido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimo 8 caracteres");
    }

    @Test
    void should_throw_when_passwords_dont_match() {
        formValido.setConfirmarSenha("diferente1");
        when(usuarioRepository.existsByEmailNormalizado(anyString())).thenReturn(false);

        assertThatThrownBy(() -> cadastroService.cadastrar(formValido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao conferem");
    }

    @Test
    void should_throw_when_password_has_no_letter() {
        formValido.setSenha("12345678");
        formValido.setConfirmarSenha("12345678");
        when(usuarioRepository.existsByEmailNormalizado(anyString())).thenReturn(false);

        assertThatThrownBy(() -> cadastroService.cadastrar(formValido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("letras e numeros");
    }

    @Test
    void should_throw_when_password_has_no_digit() {
        formValido.setSenha("abcdefgh");
        formValido.setConfirmarSenha("abcdefgh");
        when(usuarioRepository.existsByEmailNormalizado(anyString())).thenReturn(false);

        assertThatThrownBy(() -> cadastroService.cadastrar(formValido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("letras e numeros");
    }

    // -----------------------------------------------------------------------
    // Validações de campos obrigatórios
    // -----------------------------------------------------------------------

    @Test
    void should_throw_when_nome_is_blank() {
        formValido.setNome("  ");

        assertThatThrownBy(() -> cadastroService.cadastrar(formValido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nome");
    }

    @Test
    void should_throw_when_email_is_null() {
        formValido.setEmail(null);

        assertThatThrownBy(() -> cadastroService.cadastrar(formValido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("e-mail");
    }
}

