package br.com.sindico.app.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sindico.app.support.WebMvcSecurityTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issue #35: o rate limit por IP so funciona se o TrustedProxyClientIpFilter rodar antes do
 * AuthRateLimitFilter. Trocar essa ordem faria todos os usuarios cairem no bucket do IP do Vercel,
 * e os testes abaixo falhariam.
 *
 * Cada teste usa um IP de proxy e IPs de cliente proprios, porque o limitador e um singleton
 * compartilhado pelo contexto de teste. Login do PingController: limite padrao de 10 por minuto.
 */
@WebMvcTest(controllers = ProxyClientIpRateLimitTest.PingController.class,
        properties = {
            "app.security.rate-limit.enabled=true",
            "app.security.trusted-proxy-secret=" + ProxyClientIpRateLimitTest.SEGREDO
        })
@Import({SecurityConfig.class, ProxyClientIpRateLimitTest.PingController.class})
class ProxyClientIpRateLimitTest extends WebMvcSecurityTestBase {

    static final String SEGREDO = "segredo-de-teste-issue-35";
    private static final int LIMITE_LOGIN = 10;

    @RestController
    static class PingController {
        @PostMapping("/api/auth/login")
        String ping() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void comSegredoCorretoOBucketEhDoIpDoCliente() throws Exception {
        String proxy = "10.35.1.1";
        String clienteA = "198.51.100.1";
        String clienteB = "198.51.100.2";

        for (int i = 0; i < LIMITE_LOGIN; i++) {
            login(proxy, SEGREDO, clienteA).andExpect(status().isOk());
        }
        login(proxy, SEGREDO, clienteA).andExpect(status().isTooManyRequests());

        // Outro usuario atras do mesmo proxy tem bucket proprio.
        login(proxy, SEGREDO, clienteB).andExpect(status().isOk());
    }

    @Test
    void semSegredoOCabecalhoDeIpEhIgnorado() throws Exception {
        String proxy = "10.35.2.1";

        // Sem o segredo, todas as requisicoes contam no IP do proxy, mesmo com IPs de cliente diferentes.
        for (int i = 0; i < LIMITE_LOGIN; i++) {
            login(proxy, null, "198.51.100." + (10 + i)).andExpect(status().isOk());
        }
        login(proxy, null, "198.51.100.99").andExpect(status().isTooManyRequests());
    }

    @Test
    void segredoErradoTambemIgnoraOCabecalhoDeIp() throws Exception {
        String proxy = "10.35.3.1";

        for (int i = 0; i < LIMITE_LOGIN; i++) {
            login(proxy, "segredo-errado", "198.51.100." + (50 + i)).andExpect(status().isOk());
        }
        login(proxy, "segredo-errado", "198.51.100.200").andExpect(status().isTooManyRequests());
    }

    private ResultActions login(
            String remoteAddr, String segredo, String ipCliente) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/auth/login")
                .with(r -> {
                    r.setRemoteAddr(remoteAddr);
                    return r;
                });
        if (segredo != null) {
            request.header("X-Sindico-Proxy-Secret", segredo);
        }
        request.header("X-Sindico-Client-Ip", ipCliente);
        return mockMvc.perform(request);
    }
}
