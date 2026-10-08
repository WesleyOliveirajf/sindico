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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Restaura o IP real do cliente quando a requisição chega por um proxy de aplicação nosso
 * (o middleware do Vercel), que conecta ao back-end a partir de IPs do próprio Vercel.
 *
 * <p>O proxy envia o IP do usuário em {@value #CLIENT_IP_HEADER} junto com o segredo compartilhado
 * em {@value #SECRET_HEADER}. Somente quando o segredo confere o valor é aceito, e então
 * {@link HttpServletRequest#getRemoteAddr()} passa a devolver o IP do usuário (usado pelo rate
 * limit e pelo registro de aceite LGPD). Sem segredo configurado o filtro fica inativo, e um
 * cliente qualquer não consegue forjar o IP sem conhecer o segredo.</p>
 *
 * <p><b>Decisão de design — por que um filtro próprio com segredo em vez de
 * {@code server.forward-headers-strategy=native} / {@code RemoteIpValve} do Spring/Tomcat:</b><br>
 * Os nós de borda da rede do Vercel (Edge Middleware) utilizam endereços IP dinâmicos e
 * compartilhados por múltiplos clientes da plataforma Vercel. Por isso, não é viável nem
 * seguro configurar uma allowlist de CIDRs/IPs confiáveis no Tomcat/Spring (como exige o
 * {@code RemoteIpValve}): qualquer outro locatário do Vercel que realizasse requisições à nossa
 * VPS compartilharia a mesma faixa de IPs e conseguiria forjar cabeçalhos {@code X-Forwarded-For}
 * arbitrários. A relação de confiança com a borda depende, portanto, da posse do segredo
 * compartilhado pré-acordado transportado em {@value #SECRET_HEADER}.</p>
 *
 * <p><b>Rotação de segredo sem downtime:</b><br>
 * Para permitir a troca periódica ou emergencial do segredo sem indisponibilidade e sem desviar
 * clientes legítimos para o bucket compartilhado de rate limit, o filtro aceita simultaneamente
 * o segredo ativo ({@code app.security.trusted-proxy-secret}) e o segredo anterior em transição
 * ({@code app.security.trusted-proxy-secret-previous}). Ambos são validados via comparação em
 * tempo constante ({@link MessageDigest#isEqual(byte[], byte[])}) para prevenir ataques de
 * temporização (timing attacks).</p>
 *
 * <p><b>Sinal de saúde e alerta:</b><br>
 * Requisições para rotas de autenticação ({@code /api/auth/*}) que chegam sem o cabeçalho
 * confiável ou com segredo inválido emitem alerta em log com limitação de frequência (cooldown)
 * para não sobrecarregar os registros em caso de ataque, reportando a contagem de mensagens
 * suprimidas no período.</p>
 *
 * <p><b>Quando reavaliar este design:</b>
 * <ul>
 *   <li>Se a comunicação entre o front-end/borda e a VPS passar a ocorrer por rede privada ou VPN
 *       dedicada (ex.: VPC Peering, Tailscale/WireGuard ou Cloudflare Tunnel autenticado);</li>
 *   <li>Se o provedor de borda (Vercel ou outro) passar a fornecer IPs de saída estáticos dedicados
 *       ou suporte a mTLS nativo (autenticação mútua via certificado client);</li>
 *   <li>Se o roteamento e rate limit forem transferidos integralmente para um API Gateway de borda
 *       com autenticação criptográfica antes de atingir a VPS.</li>
 * </ul>
 * </p>
 */
@Component
public class TrustedProxyClientIpFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TrustedProxyClientIpFilter.class);

    static final String SECRET_HEADER = "X-Sindico-Proxy-Secret";
    static final String CLIENT_IP_HEADER = "X-Sindico-Client-Ip";
    static final long DEFAULT_LOG_INTERVAL_MILLIS = 60_000L;

    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private static final Pattern IPV6_CHARS = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    private final byte[] secret;
    private final byte[] previousSecret;
    private final long logIntervalMillis;
    private final LongSupplier clock;
    private final AtomicLong lastWarnMillis;
    private final AtomicLong suppressedWarnCount = new AtomicLong(0);

    @Autowired
    public TrustedProxyClientIpFilter(
            @Value("${app.security.trusted-proxy-secret:}") String secret,
            @Value("${app.security.trusted-proxy-secret-previous:}") String previousSecret) {
        this(secret, previousSecret, DEFAULT_LOG_INTERVAL_MILLIS, System::currentTimeMillis);
    }

    public TrustedProxyClientIpFilter(String secret) {
        this(secret, null, DEFAULT_LOG_INTERVAL_MILLIS, System::currentTimeMillis);
    }

    TrustedProxyClientIpFilter(String secret, String previousSecret, long logIntervalMillis, LongSupplier clock) {
        this.secret = toBytes(secret);
        this.previousSecret = toBytes(previousSecret);
        this.logIntervalMillis = logIntervalMillis;
        this.clock = clock;
        this.lastWarnMillis = new AtomicLong(Long.MIN_VALUE / 2);
    }

    private static byte[] toBytes(String s) {
        if (s == null || s.isBlank()) {
            return new byte[0];
        }
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private boolean isConfigured() {
        return secret.length > 0 || previousSecret.length > 0;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isConfigured();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String providedSecret = request.getHeader(SECRET_HEADER);
        byte[] providedBytes = providedSecret == null ? null : providedSecret.getBytes(StandardCharsets.UTF_8);

        if (matchesAnySecret(providedBytes)) {
            String clientIp = normalizeIp(request.getHeader(CLIENT_IP_HEADER));
            if (clientIp != null) {
                chain.doFilter(new RemoteAddrOverride(request, clientIp), response);
                return;
            }
        }

        if (isAuthPath(request)) {
            alertMissingTrustedHeader(request);
        }

        chain.doFilter(request, response);
    }

    /**
     * Valida o segredo fornecido contra o atual e o anterior em tempo constante.
     * O operador '|' assegura a execução de ambas as comparações sem curto-circuito.
     */
    private boolean matchesAnySecret(byte[] providedBytes) {
        if (providedBytes == null) {
            return false;
        }
        boolean matchCurrent = secret.length > 0 && MessageDigest.isEqual(secret, providedBytes);
        boolean matchPrevious = previousSecret.length > 0 && MessageDigest.isEqual(previousSecret, providedBytes);
        return matchCurrent | matchPrevious;
    }

    static boolean isAuthPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri.equals("/api/auth") || uri.startsWith("/api/auth/");
    }

    private void alertMissingTrustedHeader(HttpServletRequest request) {
        String uri = request.getRequestURI();
        long now = clock.getAsLong();
        long last = lastWarnMillis.get();

        if (now - last < logIntervalMillis) {
            suppressedWarnCount.incrementAndGet();
            return;
        }

        if (lastWarnMillis.compareAndSet(last, now)) {
            long suppressed = suppressedWarnCount.getAndSet(0);
            if (suppressed > 0) {
                log.warn(
                        "Requisicao para {} recebida sem cabecalho de proxy confiavel valido (remoteAddr={}, {} requisicoes similares suprimidas nos ultimos {}s). "
                                + "Se o proxy estiver ativo, verifique se PROXY_SHARED_SECRET no Vercel confere com APP_TRUSTED_PROXY_SECRET na VPS.",
                        uri, request.getRemoteAddr(), suppressed, logIntervalMillis / 1000);
            } else {
                log.warn(
                        "Requisicao para {} recebida sem cabecalho de proxy confiavel valido (remoteAddr={}). "
                                + "Se o proxy estiver ativo, verifique se PROXY_SHARED_SECRET no Vercel confere com APP_TRUSTED_PROXY_SECRET na VPS.",
                        uri, request.getRemoteAddr());
            }
        } else {
            suppressedWarnCount.incrementAndGet();
        }
    }

    long getSuppressedWarnCount() {
        return suppressedWarnCount.get();
    }

    /**
     * Retorna o IP em forma canonica ou null se o valor nao for um IP literal valido.
     *
     * O literal e validado por um parser proprio e estrito (IPv4 e IPv6) que produz os bytes do
     * endereco; a forma canonica vem de {@link InetAddress#getByAddress(byte[])}, que nunca faz
     * consulta DNS. IPv4 e IPv6 passam pelo mesmo caminho, entao {@code 001.2.3.4} e
     * {@code 1.2.3.4} resultam no mesmo valor, assim como as formas abreviada e completa de um IPv6.
     */
    static String normalizeIp(String value) {
        if (value == null) {
            return null;
        }
        String candidate = value.trim();
        byte[] bytes = candidate.indexOf(':') >= 0 ? parseIpv6(candidate) : parseIpv4(candidate);
        if (bytes == null) {
            return null;
        }
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (java.net.UnknownHostException e) {
            return null; // so ocorre se o tamanho do array for invalido; nao ha DNS envolvido
        }
    }

    /** IPv4 em decimal com 4 octetos de 1 a 3 digitos (0-255); zeros a esquerda sao aceitos. */
    private static byte[] parseIpv4(String s) {
        var m = IPV4.matcher(s);
        if (!m.matches()) {
            return null;
        }
        byte[] out = new byte[4];
        for (int i = 0; i < 4; i++) {
            int octet = Integer.parseInt(m.group(i + 1));
            if (octet > 255) {
                return null;
            }
            out[i] = (byte) octet;
        }
        return out;
    }

    /** IPv6 literal (RFC 4291), com "::" opcional e IPv4 embutido opcional no final. Sem zona ('%'). */
    private static byte[] parseIpv6(String s) {
        if (!IPV6_CHARS.matcher(s).matches()) {
            return null;
        }
        int gap = s.indexOf("::");
        if (gap >= 0 && s.lastIndexOf("::") != gap) {
            return null; // mais de um "::" (inclui ":::")
        }
        byte[] out = new byte[16];
        if (gap < 0) {
            byte[] all = parseIpv6Side(s, true);
            return all != null && all.length == 16 ? all : null;
        }
        byte[] head = parseIpv6Side(s.substring(0, gap), false);
        byte[] tail = parseIpv6Side(s.substring(gap + 2), true);
        if (head == null || tail == null || head.length + tail.length > 14) {
            return null; // "::" representa ao menos um grupo de zeros
        }
        System.arraycopy(head, 0, out, 0, head.length);
        System.arraycopy(tail, 0, out, 16 - tail.length, tail.length);
        return out;
    }

    /** Converte grupos separados por ':' em bytes; o ultimo pode ser IPv4 se allowV4Tail. */
    private static byte[] parseIpv6Side(String side, boolean allowV4Tail) {
        if (side.isEmpty()) {
            return new byte[0];
        }
        String[] tokens = side.split(":", -1);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.indexOf('.') >= 0) {
                byte[] v4 = allowV4Tail && i == tokens.length - 1 ? parseIpv4(t) : null;
                if (v4 == null) {
                    return null;
                }
                out.write(v4, 0, 4);
            } else {
                if (t.isEmpty() || t.length() > 4) {
                    return null;
                }
                int group = Integer.parseInt(t, 16);
                out.write(group >> 8);
                out.write(group & 0xFF);
            }
        }
        return out.toByteArray();
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
