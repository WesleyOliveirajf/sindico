package br.com.sindico.app.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sindico.app.security.ApiBearerEnforcementFilter;
import br.com.sindico.app.security.AuthRateLimitFilter;
import br.com.sindico.app.security.JwtAuthenticationFilter;
import br.com.sindico.app.security.TrustedProxyClientIpFilter;
import br.com.sindico.app.support.WebMvcSecurityTestBase;
import jakarta.servlet.Filter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issue #34: os filtros de seguranca sao @Component e tambem entram na SecurityFilterChain.
 * Sem desabilitar o registro de servlet, cada um rodaria duas vezes por requisicao
 * (uma na cadeia de seguranca e outra como filtro de servlet).
 */
@WebMvcTest(controllers = SecurityConfigFiltrosTest.PingController.class)
@Import({SecurityConfig.class, SecurityConfigFiltrosTest.PingController.class})
class SecurityConfigFiltrosTest extends WebMvcSecurityTestBase {

    private static final List<Class<? extends Filter>> FILTROS = List.of(
            TrustedProxyClientIpFilter.class,
            AuthRateLimitFilter.class,
            ApiBearerEnforcementFilter.class,
            JwtAuthenticationFilter.class);

    @RestController
    static class PingController {
        @PostMapping("/api/auth/login")
        String ping() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GenericApplicationContext context;

    @Autowired
    private FilterChainProxy filterChainProxy;

    @Test
    void nenhumDosQuatroFiltrosEhRegistradoComoFiltroDeServlet() {
        // Reproduz o que o Spring Boot registra no contêiner de servlet (e o MockMvc aplica).
        List<Filter> filtrosDeServlet = new ArrayList<>();
        for (ServletContextInitializer initializer : new ServletContextInitializerBeans(
                context.getDefaultListableBeanFactory(), ServletContextInitializer.class)) {
            if (initializer instanceof FilterRegistrationBean<?> registro && registro.isEnabled()) {
                filtrosDeServlet.add(registro.getFilter());
            }
        }

        for (Class<? extends Filter> tipo : FILTROS) {
            assertThat(filtrosDeServlet)
                    .as("%s nao pode estar registrado como filtro de servlet", tipo.getSimpleName())
                    .noneMatch(tipo::isInstance);
        }
    }

    @Test
    void cadaFiltroApareceUmaVezNaCadeiaEmOrdemIpRateLimitBearerJwt() {
        SecurityFilterChain cadeia = filterChainProxy.getFilterChains().get(0);
        List<Class<?>> classes = cadeia.getFilters().stream().<Class<?>>map(Object::getClass).toList();

        for (Class<? extends Filter> tipo : FILTROS) {
            assertThat(classes.stream().filter(tipo::equals).count())
                    .as("ocorrencias de %s na cadeia", tipo.getSimpleName())
                    .isEqualTo(1);
        }

        int ip = classes.indexOf(TrustedProxyClientIpFilter.class);
        int rateLimit = classes.indexOf(AuthRateLimitFilter.class);
        int bearer = classes.indexOf(ApiBearerEnforcementFilter.class);
        int jwt = classes.indexOf(JwtAuthenticationFilter.class);
        assertThat(ip).as("IP confiavel antes do rate limit").isLessThan(rateLimit);
        assertThat(rateLimit).as("rate limit antes do bearer").isLessThan(bearer);
        assertThat(rateLimit).as("rate limit antes do JWT").isLessThan(jwt);
    }

    @Test
    void requisicaoPublicaContinuaPassandoPelaCadeia() throws Exception {
        mockMvc.perform(post("/api/auth/login")).andExpect(status().isOk());
    }
}