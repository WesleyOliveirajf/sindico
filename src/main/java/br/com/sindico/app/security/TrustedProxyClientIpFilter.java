package br.com.sindico.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Restaura o IP real do cliente quando a requisicao chega por um proxy de aplicacao nosso
 * (o middleware do Vercel), que conecta ao back-end a partir de IPs do proprio Vercel.
 *
 * O proxy envia o IP do usuario em {@value #CLIENT_IP_HEADER} junto com o segredo compartilhado
 * em {@value #SECRET_HEADER}. Somente quando o segredo confere o valor e aceito, e entao
 * {@link HttpServletRequest#getRemoteAddr()} passa a devolver o IP do usuario (usado pelo rate
 * limit e pelo registro de aceite LGPD). Sem segredo configurado o filtro fica inativo, e um
 * cliente qualquer nao consegue forjar o IP sem conhecer o segredo.
 */
@Component
public class TrustedProxyClientIpFilter extends OncePerRequestFilter {

    static final String SECRET_HEADER = "X-Sindico-Proxy-Secret";
    static final String CLIENT_IP_HEADER = "X-Sindico-Client-Ip";

    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private static final Pattern IPV6_CHARS = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    private final byte[] secret;

    public TrustedProxyClientIpFilter(@Value("${app.security.trusted-proxy-secret:}") String secret) {
        this.secret = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return secret.length == 0;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String providedSecret = request.getHeader(SECRET_HEADER);
        if (providedSecret != null
                && MessageDigest.isEqual(secret, providedSecret.getBytes(StandardCharsets.UTF_8))) {
            String clientIp = normalizeIp(request.getHeader(CLIENT_IP_HEADER));
            if (clientIp != null) {
                chain.doFilter(new RemoteAddrOverride(request, clientIp), response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /** Retorna o IP em forma canonica ou null se o valor nao for um IP literal valido. */
    static String normalizeIp(String value) {
        if (value == null) {
            return null;
        }
        String candidate = value.trim();
        var v4 = IPV4.matcher(candidate);
        if (v4.matches()) {
            for (int i = 1; i <= 4; i++) {
                if (Integer.parseInt(v4.group(i)) > 255) {
                    return null;
                }
            }
            return candidate;
        }
        // Presenca de ':' garante literal IPv6 (um hostname nunca contem ':'), entao nao ha consulta DNS.
        if (candidate.indexOf(':') >= 0 && IPV6_CHARS.matcher(candidate).matches()) {
            try {
                return InetAddress.getByName(candidate).getHostAddress();
            } catch (java.net.UnknownHostException e) {
                return null;
            }
        }
        return null;
    }

    private static final class RemoteAddrOverride extends HttpServletRequestWrapper {

        private final String remoteAddr;

        RemoteAddrOverride(HttpServletRequest request, String remoteAddr) {
            super(request);
            this.remoteAddr = remoteAddr;
        }

        @Override
        public String getRemoteAddr() {
            return remoteAddr;
        }
    }
}
