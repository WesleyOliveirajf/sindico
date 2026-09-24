package br.com.sindico.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitFilterTest {

    private AtomicLong now;
    private AuthRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        now = new AtomicLong(0);
        // login: 3 por 60 s | conta: 2 por 600 s
        filter = new AuthRateLimitFilter(true, 3, 60, 2, 600, now::get);
    }

    private MockHttpServletResponse call(String method, String uri, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return response;
    }

    private boolean passou(MockHttpServletResponse response) {
        return response.getStatus() == 200;
    }

    @Test
    void bloqueiaLoginApiAposOLimiteComStatus429() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertTrue(passou(call("POST", "/api/auth/login", "10.0.0.1")));
        }

        MockHttpServletResponse bloqueada = call("POST", "/api/auth/login", "10.0.0.1");

        assertEquals(429, bloqueada.getStatus());
        assertEquals("60", bloqueada.getHeader("Retry-After"));
        assertTrue(bloqueada.getContentType().startsWith("application/json"));
        assertTrue(bloqueada.getContentAsString().contains("\"status\":429"));
    }

    @Test
    void loginApiGoogleELoginDeFormularioCompartilhamAMesmaFaixa() throws Exception {
        assertTrue(passou(call("POST", "/api/auth/login", "10.0.0.1")));
        assertTrue(passou(call("POST", "/api/auth/google", "10.0.0.1")));
        assertTrue(passou(call("POST", "/login", "10.0.0.1")));

        assertEquals(429, call("POST", "/api/auth/login", "10.0.0.1").getStatus());
    }

    @Test
    void faixaDeContaEIndependenteDaDeLogin() throws Exception {
        for (int i = 0; i < 3; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }
        assertEquals(429, call("POST", "/api/auth/login", "10.0.0.1").getStatus());

        assertTrue(passou(call("POST", "/api/auth/register", "10.0.0.1")));
        assertTrue(passou(call("POST", "/cadastro", "10.0.0.1")));
        assertEquals(429, call("POST", "/esqueci-senha", "10.0.0.1").getStatus());
    }

    @Test
    void redefinirSenhaTambemEhLimitado() throws Exception {
        call("POST", "/redefinir-senha", "10.0.0.1");
        call("POST", "/redefinir-senha", "10.0.0.1");

        assertEquals(429, call("POST", "/redefinir-senha", "10.0.0.1").getStatus());
    }

    @Test
    void ipsDiferentesNaoInterferemEntreSi() throws Exception {
        for (int i = 0; i < 4; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }

        assertEquals(429, call("POST", "/api/auth/login", "10.0.0.1").getStatus());
        assertTrue(passou(call("POST", "/api/auth/login", "10.0.0.2")));
    }

    @Test
    void naoLimitaMetodosQueNaoSaoPost() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertTrue(passou(call("GET", "/login", "10.0.0.1")));
            assertTrue(passou(call("OPTIONS", "/api/auth/login", "10.0.0.1")));
        }
    }

    @Test
    void naoLimitaOutrasRotas() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertTrue(passou(call("POST", "/api/gastos", "10.0.0.1")));
            assertTrue(passou(call("GET", "/actuator/health", "10.0.0.1")));
        }
    }

    @Test
    void liberaDepoisQueAJanelaExpira() throws Exception {
        for (int i = 0; i < 4; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }
        assertEquals(429, call("POST", "/api/auth/login", "10.0.0.1").getStatus());

        now.addAndGet(60_000);

        assertTrue(passou(call("POST", "/api/auth/login", "10.0.0.1")));
    }

    @Test
    void respostaBloqueadaNaoChegaAoRestanteDaCadeia() throws Exception {
        for (int i = 0; i < 3; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.1");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(chain.getRequest(), "a requisicao bloqueada nao deve seguir na cadeia");
    }

    @Test
    void requisicaoPermitidaSegueNaCadeia() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.1");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void desabilitadoNaoLimitaNada() throws Exception {
        filter = new AuthRateLimitFilter(false, 1, 60, 1, 600, now::get);

        for (int i = 0; i < 10; i++) {
            assertTrue(passou(call("POST", "/api/auth/login", "10.0.0.1")));
            assertTrue(passou(call("POST", "/cadastro", "10.0.0.1")));
        }
    }
}
