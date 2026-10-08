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
