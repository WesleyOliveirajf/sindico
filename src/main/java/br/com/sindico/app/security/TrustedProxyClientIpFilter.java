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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
 * Restaura o IP real do cliente quando a requisicao chega por um proxy de aplicacao nosso
 * (o middleware do Vercel), que conecta ao back-end a partir de IPs do proprio Vercel.
 *
 * <p><strong>Decisao de design:</strong>
 * Este filtro proprio e adotado em vez de confiar cegamente em
 * {@code server.forward-headers-strategy=native} ou no {@code RemoteIpValve} do Tomcat/Spring.
 * Como o Vercel Edge Middleware utiliza IPs de saida dinamicos e nao fornece um bloco de CIDRs
 * estavel ou previsivel para allowlist de proxies confiaveis (como exige o {@code RemoteIpValve}),
 * a autenticacao da origem do cabecalho {@value #CLIENT_IP_HEADER} precisa depender exclusivamente
 * de um segredo compartilhado ({@value #SECRET_HEADER}). Sem essa validacao por segredo, qualquer
 * cliente que conectasse diretamente a VPS poderia forjar o IP do usuario final.
 *
 * <p><strong>Rotacao sem downtime:</strong>
 * O filtro suporta simultaneamente um segredo principal ({@code app.security.trusted-proxy-secret})
 * e um anterior ({@code app.security.trusted-proxy-secret-previous}). Ambos sao comparados em
 * tempo constante ({@link MessageDigest#isEqual(byte[], byte[])}) para evitar ataques de temporizacao
 * (timing attacks), permitindo atualizar primeiro o back-end com o novo segredo e o antigo em paralelo,
 * depois o Vercel, e finalmente descartar o anterior sem que nenhum usuario perca seu bucket de rate limit.
 *
 * <p><strong>Sinal de saude / monitoramento:</strong>
 * Quando requisicoes chegam em rotas criticas de autenticacao ({@code /api/auth/*}) sem um cabecalho
 * confiavel aceito (por descompasso de segredo, IP ausente/invalido ou bypass do proxy), o filtro
 * registra um log {@code WARN}. O registro possui limitacao de frequencia em memoria para evitar
 * inundacao dos logs em cenarios de ataque de forca bruta ou falha massiva.
 *
 * <p><strong>Quando reavaliar esta implementacao:</strong>
 * Reavaliar o uso deste filtro se:
 * <ul>
 *   <li>O Vercel passar a fornecer IPs de saida dedicados/estaticos (ex.: Secure Compute / Dedicated IPs);</li>
 *   <li>A arquitetura migrar a borda para um servico com blocos estaticos de CIDR (ex.: Cloudflare ou API Gateway dedicado);</li>
 *   <li>For estabelecida autenticacao mutua TLS (mTLS) entre a borda (proxy) e o container da VPS.</li>
 * </ul>
 * Nesses cenarios, pode-se reavaliar o retorno ao {@code RemoteIpValve} nativo do Tomcat com allowlist de IP.
 */
@Component
public class TrustedProxyClientIpFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TrustedProxyClientIpFilter.class);

    static final String SECRET_HEADER = "X-Sindico-Proxy-Secret";
    static final String CLIENT_IP_HEADER = "X-Sindico-Client-Ip";

    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private static final Pattern IPV6_CHARS = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    private final byte[] secret;
    private final byte[] previousSecret;
    private final long warnIntervalMillis;
    private final LongSupplier clock;
    private final AtomicLong lastWarnTimestamp = new AtomicLong(Long.MIN_VALUE / 2);
    private final AtomicInteger suppressedWarnCount = new AtomicInteger(0);

    @Autowired
    public TrustedProxyClientIpFilter(
            @Value("${app.security.trusted-proxy-secret:}") String secret,
            @Value("${app.security.trusted-proxy-secret-previous:}") String previousSecret,
            @Value("${app.security.trusted-proxy-warn-interval-seconds:60}") long warnIntervalSeconds) {
        this(secret, previousSecret, warnIntervalSeconds, System::currentTimeMillis);
    }

    public TrustedProxyClientIpFilter(String secret) {
        this(secret, null, 60, System::currentTimeMillis);
    }

    public TrustedProxyClientIpFilter(String secret, String previousSecret) {
        this(secret, previousSecret, 60, System::currentTimeMillis);
    }

    TrustedProxyClientIpFilter(
            String secret,
            String previousSecret,
            long warnIntervalSeconds,
            LongSupplier clock) {
        this.secret = toBytes(secret);
        this.previousSecret = toBytes(previousSecret);
        this.warnIntervalMillis = TimeUnit.SECONDS.toMillis(Math.max(1, warnIntervalSeconds));
        this.clock = clock != null ? clock : System::currentTimeMillis;
    }

    private static byte[] toBytes(String s) {
        return (s == null || s.isBlank()) ? new byte[0] : s.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return secret.length == 0 && previousSecret.length == 0;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String providedSecret = request.getHeader(SECRET_HEADER);
        if (matchesAnySecret(providedSecret)) {
            String clientIp = normalizeIp(request.getHeader(CLIENT_IP_HEADER));
            if (clientIp != null) {
                chain.doFilter(new RemoteAddrOverride(request, clientIp), response);
                return;
            }
        }

        if (isAuthRequest(request)) {
            registrarAvisoIpAusente(request);
        }

        chain.doFilter(request, response);
    }

    /**
     * Valida o segredo fornecido contra o atual e o anterior em tempo constante.
     */
    private boolean matchesAnySecret(String providedSecret) {
        if (providedSecret == null) {
            return false;
        }
        byte[] providedBytes = providedSecret.getBytes(StandardCharsets.UTF_8);
        boolean matchesCurrent = secret.length > 0 && MessageDigest.isEqual(secret, providedBytes);
        boolean matchesPrevious = previousSecret.length > 0 && MessageDigest.isEqual(previousSecret, providedBytes);
        return matchesCurrent | matchesPrevious;
    }

    static boolean isAuthRequest(HttpServletRequest request) {
        if (request == null || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        return uri.equals("/api/auth") || uri.startsWith("/api/auth/");
    }

    private void registrarAvisoIpAusente(HttpServletRequest request) {
        long now = clock.getAsLong();
        long last = lastWarnTimestamp.get();
        if (now - last >= warnIntervalMillis) {
            if (lastWarnTimestamp.compareAndSet(last, now)) {
                int omitidos = suppressedWarnCount.getAndSet(0);
                String uri = request.getRequestURI();
                String remoteAddr = request.getRemoteAddr();
                if (omitidos > 0) {
                    log.warn("Requisicao para '{}' sem cabecalho de IP confiavel (segredo ausente ou invalido); "
                            + "rate limit usara o IP da conexao: {}. ({} alertas similares omitidos no intervalo)",
                            uri, remoteAddr, omitidos);
                } else {
                    log.warn("Requisicao para '{}' sem cabecalho de IP confiavel (segredo ausente ou invalido); "
                            + "rate limit usara o IP da conexao: {}",
                            uri, remoteAddr);
                }
            } else {
                suppressedWarnCount.incrementAndGet();
            }
        } else {
            suppressedWarnCount.incrementAndGet();
        }
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
