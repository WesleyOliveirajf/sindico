package br.com.sindico.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTest {

    private static final long WINDOW_MS = 60_000;

    private AtomicLong now;
    private FixedWindowRateLimiter limiter;

    @BeforeEach
    void setUp() {
        now = new AtomicLong(1_000_000);
        limiter = new FixedWindowRateLimiter(3, WINDOW_MS, now::get);
    }

    @Test
    void permiteAteOLimiteEBloqueiaAPartirDaProximaTentativa() {
        assertTrue(limiter.tryAcquire("ip").allowed());
        assertTrue(limiter.tryAcquire("ip").allowed());
        assertTrue(limiter.tryAcquire("ip").allowed());

        FixedWindowRateLimiter.Decision bloqueada = limiter.tryAcquire("ip");

        assertFalse(bloqueada.allowed());
        assertEquals(60, bloqueada.retryAfterSeconds());
    }

    @Test
    void retryAfterDiminuiConformeAJanelaAvanca() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("ip");
        }
        now.addAndGet(45_500);

        FixedWindowRateLimiter.Decision bloqueada = limiter.tryAcquire("ip");

        assertFalse(bloqueada.allowed());
        // faltam 14,5 s: arredonda para cima
        assertEquals(15, bloqueada.retryAfterSeconds());
    }

    @Test
    void liberaNovamenteQuandoAJanelaExpira() {
        for (int i = 0; i < 4; i++) {
            limiter.tryAcquire("ip");
        }
        assertFalse(limiter.tryAcquire("ip").allowed());

        now.addAndGet(WINDOW_MS);

        assertTrue(limiter.tryAcquire("ip").allowed());
    }

    @Test
    void naoLiberaUmMilissegundoAntesDaJanelaExpirar() {
        for (int i = 0; i < 4; i++) {
            limiter.tryAcquire("ip");
        }

        now.addAndGet(WINDOW_MS - 1);

        assertFalse(limiter.tryAcquire("ip").allowed());
    }

    @Test
    void chavesDiferentesTemContagemIndependente() {
        for (int i = 0; i < 4; i++) {
            limiter.tryAcquire("ip-a");
        }

        assertFalse(limiter.tryAcquire("ip-a").allowed());
        assertTrue(limiter.tryAcquire("ip-b").allowed());
    }

    @Test
    void removeJanelasExpiradasQuandoPassaDoLimiteDeChaves() {
        for (int i = 0; i <= FixedWindowRateLimiter.CLEANUP_THRESHOLD; i++) {
            limiter.tryAcquire("chave-" + i);
        }
        assertTrue(limiter.trackedKeys() > FixedWindowRateLimiter.CLEANUP_THRESHOLD);

        now.addAndGet(WINDOW_MS);
        limiter.tryAcquire("nova");

        assertEquals(1, limiter.trackedKeys());
    }

    @Test
    void rejeitaConfiguracaoInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new FixedWindowRateLimiter(0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new FixedWindowRateLimiter(1, 0));
    }
}
