package br.com.sindico.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Quando habilitado, envia as paginas HTML antigas (Thymeleaf) para a SPA na Vercel.
 * So intercepta GET nas rotas listadas; POST (login por formulario, cadastros legados) segue normal.
 * Desabilitado por padrao: sem app.frontend.redirect-enabled=true o comportamento nao muda.
 *
 * Nao e @Component de proposito: registrado em FrontendRedirectConfig com ordem mais alta,
 * para rodar antes do Spring Security.
 */
public class FrontendRedirectFilter extends OncePerRequestFilter {

    static final Set<String> PAGINAS_SPA = Set.of(
            "/", "/login", "/cadastro",
            "/termos", "/privacidade", "/cookies",
            "/esqueci-senha", "/redefinir-senha",
            "/condominio", "/condominios/selecionar",
            "/anotacoes", "/moradores", "/prestadores", "/perfil");

    private final boolean enabled;
    private final String frontendUrl;

    public FrontendRedirectFilter(boolean enabled, String frontendUrl) {
        this.enabled = enabled;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!enabled || !HttpMethod.GET.matches(request.getMethod()) || !PAGINAS_SPA.contains(uri)) {
            chain.doFilter(request, response);
            return;
        }

        String query = request.getQueryString();
        response.sendRedirect(frontendUrl + uri + (query == null ? "" : "?" + query));
    }
}
