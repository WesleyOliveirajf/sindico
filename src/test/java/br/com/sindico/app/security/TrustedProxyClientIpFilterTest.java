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
    private static final String CONEXAO = "76.76.21.21"; // IP do proxy (Vercel)

    /** Executa o filtro e devolve o remoteAddr que a cadeia seguinte enxerga. */
    private String remoteAddrVistoPelaCadeia(String configuredSecret, String secretHeader, String ipHeader)
            throws Exception {
        return remoteAddrVistoPelaCadeia(configuredSecret, null, secretHeader, ipHeader, "/api/auth/login");
    }

    private String remoteAddrVistoPelaCadeia(
            String configuredSecret, String configuredPreviousSecret, String secretHeader, String ipHeader)
            throws Exception {
        return remoteAddrVistoPelaCadeia(
                configuredSecret, configuredPreviousSecret, secretHeader, ipHeader, "/api/auth/login");
    }

    private String remoteAddrVistoPelaCadeia(
            String configuredSecret,
            String configuredPreviousSecret,
            String secretHeader,
            String ipHeader,
            String uri)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(CONEXAO);
        if (secretHeader != null) {
            request.addHeader(TrustedProxyClientIpFilter.SECRET_HEADER, secretHeader);
        }
        if (ipHeader != null) {
            request.addHeader(TrustedProxyClientIpFilter.CLIENT_IP_HEADER, ipHeader);
        }
        MockFilterChain chain = new MockFilterChain();

        new TrustedProxyClientIpFilter(configuredSecret, configuredPreviousSecret)
                .doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest(), "a requisicao deve sempre seguir na cadeia");
        return chain.getRequest().getRemoteAddr();
    }

    @Test
    void comSegredoCorretoUsaOIpDoCliente() throws Exception {
        assertEquals("201.10.20.30", remoteAddrVistoPelaCadeia(SECRET, SECRET, "201.10.20.30"));
    }

    @Test
    void comSegredoPrincipalQuandoAmbosConfiguradosUsaOIpDoCliente() throws Exception {
        assertEquals("201.10.20.30", remoteAddrVistoPelaCadeia(SECRET, "outro-segredo", SECRET, "201.10.20.30"));
    }

    @Test
    void comSegredoAnteriorCorretoUsaOIpDoCliente() throws Exception {
        String anterior = "segredo-antigo-valido";
        assertEquals("201.10.20.30", remoteAddrVistoPelaCadeia(SECRET, anterior, anterior, "201.10.20.30"));
    }

    @Test
    void comAmbosSegredosConfiguradosRejeitaSegredoErrado() throws Exception {
        String anterior = "segredo-antigo-valido";
        assertEquals(CONEXAO, remoteAddrVistoPelaCadeia(SECRET, anterior, "segredo-invalido", "201.10.20.30"));
    }

    @Test
    void segredoAnteriorNaoConfiguradoRejeitaSegredoAnterior() throws Exception {
        String anterior = "segredo-antigo-valido";
        assertEquals(CONEXAO, remoteAddrVistoPelaCadeia(SECRET, null, anterior, "201.10.20.30"));
    }

    @Test
    void apenasSegredoAnteriorConfiguradoAceitaAnterior() throws Exception {
        String anterior = "segredo-antigo-valido";
        assertEquals("201.10.20.30", remoteAddrVistoPelaCadeia("", anterior, anterior, "201.10.20.30"));
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
                "evil.example.com", "1.2.3.4, 5.6.7.8", "999.1.1.1", "1.2.3", "dead.beef", "dead.beef:1",
                "'; drop table usuarios;--", "", "   ", "::gggg", "1.2.3.4:80"}) {
            assertEquals(CONEXAO, remoteAddrVistoPelaCadeia(SECRET, SECRET, invalido),
                    "deveria rejeitar: '" + invalido + "'");
        }
    }

    @Test
    void literalComDoisPontosEHexNaoViraConsultaDns() {
        // Antes ia para InetAddress.getByName e disparava DNS; agora o parser rejeita o literal.
        // Os testes de normalizeIp usam apenas literais, e o codigo so usa getByAddress(byte[]).
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
    void isAuthPathIdentificaRotasDeAutenticacao() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        assertTrue(TrustedProxyClientIpFilter.isAuthPath(req));

        req = new MockHttpServletRequest("POST", "/api/auth/google");
        assertTrue(TrustedProxyClientIpFilter.isAuthPath(req));

        req = new MockHttpServletRequest("GET", "/api/auth/me");
        assertTrue(TrustedProxyClientIpFilter.isAuthPath(req));

        req = new MockHttpServletRequest("GET", "/api/auth");
        assertTrue(TrustedProxyClientIpFilter.isAuthPath(req));

        req = new MockHttpServletRequest("POST", "/api/moradores");
        assertFalse(TrustedProxyClientIpFilter.isAuthPath(req));

        req = new MockHttpServletRequest("GET", "/dashboard");
        assertFalse(TrustedProxyClientIpFilter.isAuthPath(req));

        req = new MockHttpServletRequest("GET", "/login");
        assertFalse(TrustedProxyClientIpFilter.isAuthPath(req));
    }

    @Test
    void isAuthPathRespeitaContextPath() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/contexto/api/auth/login");
        req.setContextPath("/contexto");
        assertTrue(TrustedProxyClientIpFilter.isAuthPath(req));

        req = new MockHttpServletRequest("GET", "/contexto/api/moradores");
        req.setContextPath("/contexto");
        assertFalse(TrustedProxyClientIpFilter.isAuthPath(req));
    }

    @Test
    void alertaEmitidoNoLogQuandoRequisicaoAuthChegaSemSegredo() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(TrustedProxyClientIpFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            remoteAddrVistoPelaCadeia(SECRET, null, null, "201.10.20.30");
            boolean temAlerta = appender.list.stream().anyMatch(e ->
                    e.getLevel() == Level.WARN && e.getFormattedMessage().contains("/api/auth/login"));
            assertTrue(temAlerta, "Deveria emitir alerta de log para rota /api/auth/* sem segredo");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void alertaEmitidoNoLogQuandoRequisicaoAuthChegaComSegredoErrado() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(TrustedProxyClientIpFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            remoteAddrVistoPelaCadeia(SECRET, null, "segredo-errado", "201.10.20.30");
            boolean temAlerta = appender.list.stream().anyMatch(e ->
                    e.getLevel() == Level.WARN && e.getFormattedMessage().contains("/api/auth/login"));
            assertTrue(temAlerta, "Deveria emitir alerta de log para rota /api/auth/* com segredo errado");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void alertaNaoEmitidoQuandoRequisicaoAuthChegaComSegredoValido() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(TrustedProxyClientIpFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            remoteAddrVistoPelaCadeia(SECRET, null, SECRET, "201.10.20.30");
            boolean temAlerta = appender.list.stream().anyMatch(e -> e.getLevel() == Level.WARN);
            assertFalse(temAlerta, "Nao deveria emitir alerta para requisicao com segredo valido");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void alertaNaoEmitidoParaRotasForaDeAuth() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(TrustedProxyClientIpFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            remoteAddrVistoPelaCadeia(SECRET, null, null, "201.10.20.30", "/api/moradores");
            boolean temAlerta = appender.list.stream().anyMatch(e -> e.getLevel() == Level.WARN);
            assertFalse(temAlerta, "Nao deveria emitir alerta para rotas fora de /api/auth/*");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void alertaLimitadoPorFrequenciaEContaSuprimidas() throws Exception {
        AtomicLong relogio = new AtomicLong(100_000L);
        long intervalo = 60_000L;
        TrustedProxyClientIpFilter filtro = new TrustedProxyClientIpFilter(SECRET, null, intervalo, relogio::get);

        Logger logger = (Logger) LoggerFactory.getLogger(TrustedProxyClientIpFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr(CONEXAO);

            // 1ª requisição no instante 100_000 -> emite o primeiro log
            filtro.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
            assertEquals(1, appender.list.size());
            assertEquals(0, filtro.getSuppressedWarnCount());

            // 2ª, 3ª e 4ª requisições dentro da janela de 60s -> suprimidas
            relogio.addAndGet(10_000L); // 110_000
            filtro.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
            relogio.addAndGet(10_000L); // 120_000
            filtro.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
            relogio.addAndGet(10_000L); // 130_000
            filtro.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

            assertEquals(1, appender.list.size(), "Logs dentro do intervalo devem ser suprimidos");
            assertEquals(3, filtro.getSuppressedWarnCount(), "Deveria contabilizar 3 chamadas suprimidas");

            // Avança o relógio além do intervalo de 60s -> 5ª requisição emite log com contagem de suprimidas
            relogio.addAndGet(40_000L); // 170_000 (diferença de 70_000 > 60_000)
            filtro.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

            assertEquals(2, appender.list.size(), "Deveria emitir novo log apos expirar o intervalo");
            assertEquals(0, filtro.getSuppressedWarnCount(), "Contador de suprimidas deve ser resetado apos logar");

            String segundoLog = appender.list.get(1).getFormattedMessage();
            assertTrue(segundoLog.contains("3 requisicoes similares suprimidas"),
                    "Mensagem deveria reportar as 3 requisicoes suprimidas: " + segundoLog);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void semSegredoConfiguradoNaoEmiteAlerta() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(TrustedProxyClientIpFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            remoteAddrVistoPelaCadeia("", null, null, "201.10.20.30");
            boolean temAlerta = appender.list.stream().anyMatch(e -> e.getLevel() == Level.WARN);
            assertFalse(temAlerta, "Sem segredo configurado (dev/test), o filtro e inativo e nao loga alerta");
        } finally {
            logger.detachAppender(appender);
        }
    }
}
