package br.com.sindico.app.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Limitador de taxa por chave com janela fixa, em memoria.
 *
 * Adequado para uma unica instancia da aplicacao: o estado se perde no restart e nao e
 * compartilhado entre replicas. Se o back-end passar a rodar com mais de uma instancia,
 * trocar por um limitador distribuido (ex.: Redis/Bucket4j).
 */
public final class FixedWindowRateLimiter {

    /** Acima deste numero de chaves, janelas expiradas sao removidas para limitar o uso de memoria. */
    static final int CLEANUP_THRESHOLD = 10_000;

    public record Decision(boolean allowed, long retryAfterSeconds) {}

    private record Window(long startMillis, int count) {}

    private final int maxRequests;
    private final long windowMillis;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int maxRequests, long windowMillis) {
        this(maxRequests, windowMillis, System::currentTimeMillis);
    }

    FixedWindowRateLimiter(int maxRequests, long windowMillis, LongSupplier clock) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("maxRequests deve ser >= 1");
        }
        if (windowMillis < 1) {
            throw new IllegalArgumentException("windowMillis deve ser >= 1");
        }
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    /** Registra uma tentativa para a chave e informa se ela cabe no limite da janela atual. */
    public Decision tryAcquire(String key) {
        long now = clock.getAsLong();
        if (windows.size() > CLEANUP_THRESHOLD) {
            evictExpired(now);
        }

        Window window = windows.compute(key, (k, current) -> {
            if (current == null || now - current.startMillis() >= windowMillis) {
                return new Window(now, 1);
            }
            return new Window(current.startMillis(), current.count() + 1);
        });

        if (window.count() <= maxRequests) {
            return new Decision(true, 0);
        }
        long remainingMillis = window.startMillis() + windowMillis - now;
        long retryAfterSeconds = Math.max(1, (remainingMillis + 999) / 1000);
        return new Decision(false, retryAfterSeconds);
    }

    int trackedKeys() {
        return windows.size();
    }

    private void evictExpired(long now) {
        windows.entrySet().removeIf(e -> now - e.getValue().startMillis() >= windowMillis);
    }
}
