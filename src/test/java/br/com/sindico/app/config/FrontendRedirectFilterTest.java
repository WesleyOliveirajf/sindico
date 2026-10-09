package br.com.sindico.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class FrontendRedirectFilterTest {

    private static final String FRONTEND = "https://sindico-seven.vercel.app";

    @Test
    void desabilitadoNaoRedireciona() throws Exception {
        FrontendRedirectFilter filter = new FrontendRedirectFilter(false, FRONTEND);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("GET", "/login"), response, chain);

        assertThat(response.getRedirectedUrl()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void getEmPaginaHtmlRedirecionaParaSpa() throws Exception {
        FrontendRedirectFilter filter = new FrontendRedirectFilter(true, FRONTEND);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/"), response, new MockFilterChain());

        assertThat(response.getRedirectedUrl()).isEqualTo(FRONTEND + "/");
    }

    @Test
    void preservaQueryStringNoRedirect() throws Exception {
        FrontendRedirectFilter filter = new FrontendRedirectFilter(true, FRONTEND + "/");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/redefinir-senha");
        request.setQueryString("token=abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getRedirectedUrl()).isEqualTo(FRONTEND + "/redefinir-senha?token=abc123");
    }

    @Test
    void postNaoEhInterceptado() throws Exception {
        FrontendRedirectFilter filter = new FrontendRedirectFilter(true, FRONTEND);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("POST", "/login"), response, chain);

        assertThat(response.getRedirectedUrl()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void rotasApiNaoSaoInterceptadas() throws Exception {
        FrontendRedirectFilter filter = new FrontendRedirectFilter(true, FRONTEND);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/manutencoes"), response, chain);

        assertThat(response.getRedirectedUrl()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void rotaDesconhecidaSegueParaACadeia() throws Exception {
        FrontendRedirectFilter filter = new FrontendRedirectFilter(true, FRONTEND);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("GET", "/actuator/health"), response, chain);

        assertThat(response.getRedirectedUrl()).isNull();
    }
}
