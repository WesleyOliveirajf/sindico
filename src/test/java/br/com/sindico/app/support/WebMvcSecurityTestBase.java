package br.com.sindico.app.support;

import br.com.sindico.app.security.JwtService;
import br.com.sindico.app.security.UsuarioTenantUserDetailsService;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Classe base para testes @WebMvcTest que importam SecurityConfig.
 *
 * O SecurityConfig depende de JwtAuthenticationFilter, que por sua vez depende de
 * JwtService e UsuarioTenantUserDetailsService. Como @WebMvcTest não carrega
 * @Service beans automaticamente, esses dois devem ser mockados para que o
 * contexto de teste seja criado com sucesso.
 */
public abstract class WebMvcSecurityTestBase {

    @MockBean
    protected JwtService jwtService;

    @MockBean
    protected UsuarioTenantUserDetailsService usuarioTenantUserDetailsService;
}
