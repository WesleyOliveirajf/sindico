package br.com.sindico.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TrustedProxyClientIpFilterTest {

    private static final String SECRET = "segredo-compartilhado-de-teste";
    private static final String CONEXAO = "76.76.21.21"; // IP do proxy (Vercel)

    /** Executa o filtro e devolve o remoteAddr que a cadeia seguinte enxerga. */
    private String remoteAddrVistoPelaCadeia(String configuredSecret, String secretHeader, String ipHeader)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(CONEXAO);
        if (secretHeader != null) {
            request.addHeader(TrustedProxyClientIpFilter.SECRET_HEADER, secretHeader);
        }
        if (ipHeader != null) {
            request.addHeader(TrustedProxyClientIpFilter.CLIENT_IP_HEADER, ipHeader);
        }
        MockFilterChain chain = new MockFilterChain();

        new TrustedProxyClientIpFilter(configuredSecret).doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest(), "a requisicao deve sempre seguir na cadeia");
        return chain.getRequest().getRemoteAddr();
    }

    @Test
    void comSegredoCorretoUsaOIpDoCliente() throws Exception {
        assertEquals("201.10.20.30", remoteAddrVistoPelaCadeia(SECRET, SECRET, "201.10.20.30"));
    }

    @Test
    void aceitaIpv6ENormalizaAForma() throws Exception {
        assertEquals("2804:14d:1:2:0:0:0:1", remoteAddrVistoPelaCadeia(SECRET, SECRET, "2804:14d:1:2::1"));
    }

    @Test
    void ignoraEspacosAoRedorDoIp() throws Exception {
        assertEquals("201.10.20.30", remoteAddrVistoPelaCadeia(SECRET, SECRET, "  201.10.20.30 "));
    }

    @Test
    void segredoErradoMantemOIpDaConexao() throws Exception {
        assertEquals(CONEXAO, remoteAddrVistoPelaCadeia(SECRET, "outro-segredo", "201.10.20.30"));
    }

    @Test
    void semCabecalhoDeSegredoMantemOIpDaConexao() throws Exception {
        assertEquals(CONEXAO, remoteAddrVistoPelaCadeia(SECRET, null, "201.10.20.30"));
    }

    @Test
    void semSegredoConfiguradoNuncaConfiaNoCabecalho() throws Exception {
        assertEquals(CONEXAO, remoteAddrVistoPelaCadeia("", "", "201.10.20.30"));
        assertEquals(CONEXAO, remoteAddrVistoPelaCadeia("", null, "201.10.20.30"));
        assertEquals(CONEXAO, remoteAddrVistoPelaCadeia(null, "qualquer", "201.10.20.30"));
    }

    @Test
    void semIpNoCabecalhoMantemOIpDaConexao() throws Exception {
        assertEquals(CONEXAO, remoteAddrVistoPelaCadeia(SECRET, SECRET, null));
    }

    @Test
    void valoresQueNaoSaoIpSaoRejeitados() throws Exception {
        for (String invalido : new String[] {
                "evil.example.com", "1.2.3.4, 5.6.7.8", "999.1.1.1", "1.2.3", "dead.beef",
                "'; drop table usuarios;--", "", "   ", "::gggg", "1.2.3.4:80"}) {
            assertEquals(CONEXAO, remoteAddrVistoPelaCadeia(SECRET, SECRET, invalido),
                    "deveria rejeitar: '" + invalido + "'");
        }
    }

    @Test
    void normalizeIpRetornaNullParaEntradaNula() {
        assertNull(TrustedProxyClientIpFilter.normalizeIp(null));
    }
}
