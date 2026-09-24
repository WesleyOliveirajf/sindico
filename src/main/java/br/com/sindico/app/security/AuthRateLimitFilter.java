package br.com.sindico.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita tentativas por IP nos POSTs publicos de autenticacao e de conta, contra forca bruta
 * e abuso de cadastro/redefinicao de senha.
 *
 * Duas faixas independentes:
 *  - login: entrada por senha ou Google (padrao 10 por minuto);
 *  - conta: cadastro, "esqueci minha senha" e redefinicao (padrao 5 a cada 10 minutos).
 *
 * O IP vem de {@link HttpServletRequest#getRemoteAddr()}; atras de proxy reverso e preciso
 * habilitar server.forward-headers-strategy=native para que seja o IP real do cliente.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    private static final Set<String> LOGIN_PATHS = Set.of("/api/auth/login", "/api/auth/google", "/login");
    private static final Set<String> ACCOUNT_PATHS =
            Set.of("/api/auth/register", "/cadastro", "/esqueci-senha", "/redefinir-senha");

    private final boolean enabled;
    private final FixedWindowRateLimiter loginLimiter;
    private final FixedWindowRateLimiter accountLimiter;

    @Autowired
    public AuthRateLimitFilter(
            @Value("${app.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.rate-limit.login.max-requests:10}") int loginMaxRequests,
            @Value("${app.security.rate-limit.login.window-seconds:60}") long loginWindowSeconds,
            @Value("${app.security.rate-limit.account.max-requests:5}") int accountMaxRequests,
            @Value("${app.security.rate-limit.account.window-seconds:600}") long accountWindowSeconds) {
        this(enabled, loginMaxRequests, loginWindowSeconds, accountMaxRequests, accountWindowSeconds,
                System::currentTimeMillis);
    }

    AuthRateLimitFilter(
            boolean enabled,
            int loginMaxRequests,
            long loginWindowSeconds,
            int accountMaxRequests,
            long accountWindowSeconds,
            LongSupplier clock) {
        this.enabled = enabled;
        this.loginLimiter = new FixedWindowRateLimiter(
                loginMaxRequests, TimeUnit.SECONDS.toMillis(loginWindowSeconds), clock);
        this.accountLimiter = new FixedWindowRateLimiter(
                accountMaxRequests, TimeUnit.SECONDS.toMillis(accountWindowSeconds), clock);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled || !HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return !LOGIN_PATHS.contains(uri) && !ACCOUNT_PATHS.contains(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean loginTier = LOGIN_PATHS.contains(uri);
        String key = (loginTier ? "login:" : "account:") + request.getRemoteAddr();

        FixedWindowRateLimiter.Decision decision =
                (loginTier ? loginLimiter : accountLimiter).tryAcquire(key);

        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit excedido: {} {} de {}", request.getMethod(), uri, request.getRemoteAddr());
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(
                "{\"error\":\"Muitas tentativas. Aguarde alguns instantes e tente novamente.\",\"status\":429}"
                        .getBytes(StandardCharsets.UTF_8));
    }
}
