package br.com.sindico.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class SecurityTenantAccessorTest {

    private final UUID padrao = UUID.randomUUID();
    private final UUID outro = UUID.randomUUID();
    private final UUID naoPermitido = UUID.randomUUID();
    private final SecurityTenantAccessor accessor = new SecurityTenantAccessor();

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void usaCabecalhoQuandoCondominioEhPermitido() {
        autenticar();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityTenantAccessor.CONDOMINIO_HEADER, outro.toString());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(accessor.condominioAtual()).isEqualTo(outro);
    }

    @Test
    void ignoraCabecalhoDeCondominioNaoPermitido() {
        autenticar();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityTenantAccessor.CONDOMINIO_HEADER, naoPermitido.toString());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(accessor.condominioAtual()).isEqualTo(padrao);
    }

    @Test
    void ignoraCabecalhoInvalido() {
        autenticar();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityTenantAccessor.CONDOMINIO_HEADER, "nao-e-uuid");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(accessor.condominioAtual()).isEqualTo(padrao);
    }

    @Test
    void semCabecalhoUsaCondominioPadrao() {
        autenticar();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        assertThat(accessor.condominioAtual()).isEqualTo(padrao);
    }

    private void autenticar() {
        UsuarioTenantPrincipal principal = new UsuarioTenantPrincipal(
                UUID.randomUUID(),
                padrao,
                Set.of(padrao, outro),
                "sindico@email.com",
                "hash",
                List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
