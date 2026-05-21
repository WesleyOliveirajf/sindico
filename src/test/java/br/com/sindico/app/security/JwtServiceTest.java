package br.com.sindico.app.security;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do JwtService.
 * Verifica geração, validação e extração de claims sem dependência de contexto Spring.
 */
class JwtServiceTest {

    private static final String SECRET = "test-secret-must-be-at-least-32-chars-long";
    private static final long EXPIRATION_MINUTES = 60L;

    private JwtService jwtService;
    private UsuarioTenantPrincipal principal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MINUTES);

        UUID condominioId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID usuarioId = UUID.randomUUID();

        principal = new UsuarioTenantPrincipal(
                usuarioId,
                condominioId,
                Set.of(condominioId),
                "sindico@test.com",
                "hashed_password",
                List.of(new SimpleGrantedAuthority("ROLE_SINDICO")));
    }

    @Test
    void should_generate_valid_token() {
        String token = jwtService.generateToken(principal);
        assertThat(token).isNotBlank();
    }

    @Test
    void should_validate_token_as_true() {
        String token = jwtService.generateToken(principal);
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void should_extract_email_from_token() {
        String token = jwtService.generateToken(principal);
        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("sindico@test.com");
    }

    @Test
    void should_return_false_for_tampered_token() {
        String token = jwtService.generateToken(principal);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void should_return_false_for_blank_token() {
        assertThat(jwtService.isValid("")).isFalse();
    }

    @Test
    void should_return_false_for_random_string() {
        assertThat(jwtService.isValid("not.a.jwt")).isFalse();
    }

    @Test
    void should_reject_token_signed_with_different_key() {
        JwtService otherService = new JwtService("completely-different-secret-key-32chars!!", EXPIRATION_MINUTES);
        String tokenFromOther = otherService.generateToken(principal);
        assertThat(jwtService.isValid(tokenFromOther)).isFalse();
    }

    @Test
    void should_treat_expired_token_as_invalid() throws InterruptedException {
        // Token com expiração de 0 minutos → já nasce expirado
        JwtService shortLived = new JwtService(SECRET, 0L);
        String token = shortLived.generateToken(principal);
        Thread.sleep(100); // aguarda fração de segundo
        assertThat(shortLived.isValid(token)).isFalse();
    }
}


