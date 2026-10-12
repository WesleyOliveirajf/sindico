package br.com.sindico.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TrustedProxyClientIpFilterTest {

    private static final String SECRET = "segredo-compartilhado-de-teste";
    private static final String SECRET_PREVIOUS = "segredo-antigo-durante-rotacao";
    private static final String CONEXAO = "76.76.21.21"; // IP do proxy (Vercel)

    /** Executa o filtro e devolve o remoteAddr que a cadeia seguinte enxerga. */
    private String remoteAddrVistoPelaCadeia(String configuredSecret, String secretHeader, String ipHeader)
            throws Exception {
        return remoteAddrVistoPelaCadeia(configuredSecret, null, secretHeader, ipHeader, "POST", "/api/auth/login");
    }

    private String remoteAddrVistoPelaCadeia(
            String configuredSecret,
            String configuredPreviousSecret,
            String secretHeader,
            String ipHeader,
            String method,
            String uri)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(CONEXAO);
        if (secretHeader != null) {
            request.addHeader(TrustedProxyClientIpFilter.SECRET_HEADER, secretHeader);
        }
        if (ipHeader != null) {
            request.addHeader(TrustedProxyClientIpFilter.CLIENT_IP_HEADER, ipHeader);
        }
        MockFilterChain chain = new MockFilterChain();

        new TrustedProxyClientIpFilter(configuredSecret, configuredPreviousSecret).doFilter(
                request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest(), "a requisicao deve sempre seguir na cadeia");
        return chain.getRequest().getRemoteAddr();
    }

    @Test
    void comSegredoCorretoUsaOIpDoCliente() throws Exception {
        assertEquals("201.10.20.30", remoteAddrVistoPelaCadeia(SECRET, SECRET, "201.10.20.30"));
    }

    @Test
    void comSegredoAnteriorCorretoUsaOIpDoCliente() throws Exception {
        assertEquals(
                "201.10.20.30",
                remoteAddrVistoPelaCadeia(
                        SECRET, SECRET_PREVIOUS, SECRET_PREVIOUS, "201.10.20.30", "POST", "/api/auth/login"));
    }

    @Test
    void quandoAmbosEstaoConfiguradosAmbosSaoAceitos() throws Exception {
        TrustedProxyClientIpFilter filter = new TrustedProxyClientIpFilter(SECRET, SECRET_PREVIOUS);

        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/auth/login");
        req1.setRemoteAddr(CONEXAO);
        req1.addHeader(TrustedProxyClientIpFilter.SECRET_HEADER, SECRET);
        req1.addHeader(TrustedProxyClientIpFilter.CLIENT_IP_HEADER, "10.0.0.1");
        MockFilterChain chain1 = new MockFilterChain();
        filter.doFilter(req1, new MockHttpServletResponse(), chain1);
        assertEquals("10.0.0.1", chain1.getRequest().getRemoteAddr());

        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/auth/login");
        req2.setRemoteAddr(CONEXAO);
        req2.addHeader(TrustedProxyClientIpFilter.SECRET_HEADER, SECRET_PREVIOUS);
        req2.addHeader(TrustedProxyClientIpFilter.CLIENT_IP_HEADER, "10.0.0.2");
        MockFilterChain chain2 = new MockFilterChain();
        filter.doFilter(req2, new MockHttpServletResponse(), chain2);
        assertEquals("10.0.0.2", chain2.getRequest().getRemoteAddr());
    }

    @Test
    void quandoApenasSegredoAnteriorConfiguradoTambemAceita() throws Exception {
        assertEquals(
                "201.10.20.30",
                remoteAddrVistoPelaCadeia(
                        "", SECRET_PREVIOUS, SECRET_PREVIOUS, "201.10.20.30", "POST", "/api/auth/login"));
    }

    @Test
    void segredoErradoComAmbosConfiguradosMantemOIpDaConexao() throws Exception {
        assertEquals(
                CONEXAO,
                remoteAddrVistoPelaCadeia(
                        SECRET, SECRET_PREVIOUS, "segredo-terceiro", "201.10.20.30", "POST", "/api/auth/login"));
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
        assertEquals(CONEXAO, remoteAddrVistoPelaCadeia("", "", "", "201.10.20.30", "POST", "/api/auth/login"));
    }

    @Test
    void semIpNoCabecalhoMantemOIpDaConexao() throws Exception {
        assertEquals(CONEXAO, remoteAddrVistoPelaCadeia(SECRET, SECRET, null));
    }

    @Test
    void valoresQueNaoSaoIpSaoRejeitados() throws Exception {
        for (String invalido : new String[] {
                "evil.example.com", "1.2.3.4, 5.6.7.8", "999.1.1.1", "1.2.3", "dead.beef", "dead.beef:1",
                "'; drop table usuarios;--", "", "   ", "::gggg", "1.2.3.4:80"}) {
            assertEquals(CONEXAO, remoteAddrVistoPelaCadeia(SECRET, SECRET, invalido),
                    "deveria rejeitar: '" + invalido + "'");
        }
    }

    @Test
    void literalComDoisPontosEHexNaoViraConsultaDns() {
        assertNull(TrustedProxyClientIpFilter.normalizeIp("dead.beef:1"));
        assertNull(TrustedProxyClientIpFilter.normalizeIp("cafe.babe:80"));
        assertNull(TrustedProxyClientIpFilter.normalizeIp("a.b:c"));
    }

    @Test
    void ipv4EhCanonicalizadoSemZerosAEsquerda() {
        assertEquals("1.2.3.4", TrustedProxyClientIpFilter.normalizeIp("001.2.3.4"));
        assertEquals("1.2.3.4", TrustedProxyClientIpFilter.normalizeIp("1.2.3.4"));
        assertEquals("10.0.0.9", TrustedProxyClientIpFilter.normalizeIp("010.000.00.009"));
        assertEquals("255.255.255.255", TrustedProxyClientIpFilter.normalizeIp("255.255.255.255"));
        assertNull(TrustedProxyClientIpFilter.normalizeIp("256.1.1.1"));
    }

    @Test
    void ipv6AbreviadoECompletoSaoEquivalentes() {
        String esperado = "2804:14d:1:2:0:0:0:1";
        assertEquals(esperado, TrustedProxyClientIpFilter.normalizeIp("2804:14d:1:2::1"));
        assertEquals(esperado, TrustedProxyClientIpFilter.normalizeIp("2804:014d:0001:0002:0000:0000:0000:0001"));
        assertEquals(esperado, TrustedProxyClientIpFilter.normalizeIp("2804:14D:1:2:0:0:0:1"));
        assertEquals("0:0:0:0:0:0:0:1", TrustedProxyClientIpFilter.normalizeIp("::1"));
        assertEquals("0:0:0:0:0:0:0:0", TrustedProxyClientIpFilter.normalizeIp("::"));
    }

    @Test
    void ipv4MapeadoEmIpv6VirraIpv4Canonico() {
        assertEquals("1.2.3.4", TrustedProxyClientIpFilter.normalizeIp("::ffff:1.2.3.4"));
        assertEquals("1.2.3.4", TrustedProxyClientIpFilter.normalizeIp("::ffff:102:304"));
        assertEquals("1.2.3.4", TrustedProxyClientIpFilter.normalizeIp("0:0:0:0:0:ffff:001.2.3.4"));
    }

    @Test
    void ipv6MalFormadoEhRejeitado() {
        for (String invalido : new String[] {
                "1::2::3", ":::", "1:2:3:4:5:6:7", "1:2:3:4:5:6:7:8:9", "1:2:3:4:5:6:7:8::",
                "12345::1", "::1.2.3", "::1.2.3.4:5", "1.2.3.4::1", "fe80::1%eth0", ":1:2:3:4:5:6:7", "1:"}) {
            assertNull(TrustedProxyClientIpFilter.normalizeIp(invalido), "deveria rejeitar: '" + invalido + "'");
        }
    }

    @Test
    void ipsDiferentesDoMesmoClienteConvergemParaUmaUnicaChave() throws Exception {
        assertEquals(remoteAddrVistoPelaCadeia(SECRET, SECRET, "1.2.3.4"),
                remoteAddrVistoPelaCadeia(SECRET, SECRET, "001.002.003.004"));
    }

    @Test
    void normalizeIpRetornaNullParaEntradaNula() {
        assertNull(TrustedProxyClientIpFilter.normalizeIp(null));
    }

    @Test
    void isAuthRequestIdentificaCorretamenteRotasDeAutenticacao() {
        MockHttpServletRequest login = new MockHttpServletRequest("POST", "/api/auth/login");
        assertTrue(TrustedProxyClientIpFilter.isAuthRequest(login));

        MockHttpServletRequest google = new MockHttpServletRequest("POST", "/api/auth/google");
        assertTrue(TrustedProxyClientIpFilter.isAuthRequest(google));

        MockHttpServletRequest authRoot = new MockHttpServletRequest("GET", "/api/auth");
        assertTrue(TrustedProxyClientIpFilter.isAuthRequest(authRoot));

        MockHttpServletRequest options = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
        assertFalse(TrustedProxyClientIpFilter.isAuthRequest(options));

        MockHttpServletRequest outroEndpoint = new MockHttpServletRequest("GET", "/api/manutencoes");
        assertFalse(TrustedProxyClientIpFilter.isAuthRequest(outroEndpoint));

        MockHttpServletRequest authPrefixFalso = new MockHttpServletRequest("GET", "/api/auth-extra");
        assertFalse(TrustedProxyClientIpFilter.isAuthRequest(authPrefixFalso));

        assertFalse(TrustedProxyClientIpFilter.isAuthRequest(null));
    }

    @Test
    void avisoDeSaudeDisparadoQuandoAuthChegaSemSegredoOuComSegredoInvalido() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(TrustedProxyClientIpFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            TrustedProxyClientIpFilter filter = new TrustedProxyClientIpFilter(SECRET);

            MockHttpServletRequest reqSemSegredo = new MockHttpServletRequest("POST", "/api/auth/login");
            reqSemSegredo.setRemoteAddr(CONEXAO);
            filter.doFilter(reqSemSegredo, new MockHttpServletResponse(), new MockFilterChain());

            assertEquals(1, appender.list.size());
            ILoggingEvent evento = appender.list.get(0);
            assertEquals(Level.WARN, evento.getLevel());
            assertTrue(evento.getFormattedMessage().contains("/api/auth/login"));
            assertTrue(evento.getFormattedMessage().contains(CONEXAO));

            // Com segredo correto, nenhum novo aviso é logado
            MockHttpServletRequest reqCorreta = new MockHttpServletRequest("POST", "/api/auth/login");
            reqCorreta.setRemoteAddr(CONEXAO);
            reqCorreta.addHeader(TrustedProxyClientIpFilter.SECRET_HEADER, SECRET);
            reqCorreta.addHeader(TrustedProxyClientIpFilter.CLIENT_IP_HEADER, "10.0.0.1");
            filter.doFilter(reqCorreta, new MockHttpServletResponse(), new MockFilterChain());

            assertEquals(1, appender.list.size());
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void avisoDeSaudeNaoDisparadoEmRotasForaDeAuth() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(TrustedProxyClientIpFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            TrustedProxyClientIpFilter filter = new TrustedProxyClientIpFilter(SECRET);

            MockHttpServletRequest reqManutencoes = new MockHttpServletRequest("GET", "/api/manutencoes");
            reqManutencoes.setRemoteAddr(CONEXAO);
            filter.doFilter(reqManutencoes, new MockHttpServletResponse(), new MockFilterChain());

            assertEquals(0, appender.list.size());
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void avisoDeSaudeNaoDisparadoQuandoFiltroInativoSemSegredo() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(TrustedProxyClientIpFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            TrustedProxyClientIpFilter filter = new TrustedProxyClientIpFilter("", "");

            MockHttpServletRequest reqLogin = new MockHttpServletRequest("POST", "/api/auth/login");
            reqLogin.setRemoteAddr(CONEXAO);
            filter.doFilter(reqLogin, new MockHttpServletResponse(), new MockFilterChain());

            assertEquals(0, appender.list.size());
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void limiteDeFrequenciaDoAvisoSuprimeAlertasRepetidosEInformaOmitidos() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(TrustedProxyClientIpFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            AtomicLong tempoSimulado = new AtomicLong(1_000_000L);
            TrustedProxyClientIpFilter filter =
                    new TrustedProxyClientIpFilter(SECRET, null, 60, tempoSimulado::get);

            MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/auth/login");
            req1.setRemoteAddr(CONEXAO);
            filter.doFilter(req1, new MockHttpServletResponse(), new MockFilterChain());
            assertEquals(1, appender.list.size());

            // Requisicoes dentro da janela de 60s (60000ms) sao suprimidas
            tempoSimulado.addAndGet(10_000L); // +10s
            MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/auth/login");
            req2.setRemoteAddr(CONEXAO);
            filter.doFilter(req2, new MockHttpServletResponse(), new MockFilterChain());

            tempoSimulado.addAndGet(20_000L); // +20s
            MockHttpServletRequest req3 = new MockHttpServletRequest("POST", "/api/auth/google");
            req3.setRemoteAddr(CONEXAO);
            filter.doFilter(req3, new MockHttpServletResponse(), new MockFilterChain());

            assertEquals(1, appender.list.size(), "Alertas repetidos dentro do intervalo devem ser suprimidos");

            // Apos a janela de 60s, a proxima requisicao loga e cita os alertas omitidos
            tempoSimulado.addAndGet(40_000L); // total +70s desde o primeiro
            MockHttpServletRequest req4 = new MockHttpServletRequest("POST", "/api/auth/login");
            req4.setRemoteAddr(CONEXAO);
            filter.doFilter(req4, new MockHttpServletResponse(), new MockFilterChain());

            assertEquals(2, appender.list.size());
            ILoggingEvent segundoEvento = appender.list.get(1);
            assertEquals(Level.WARN, segundoEvento.getLevel());
            assertTrue(segundoEvento.getFormattedMessage().contains("2 alertas similares omitidos no intervalo"));
        } finally {
            logger.detachAppender(appender);
        }
    }
}
