package com.example.minshuku.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Limits repeated booking-number/email authentication failures per remote source.
 * Successful authentication removes the source's failure history.
 */
@Service
public class PublicBookingLookupRateLimiter {
    static final int MAX_FAILURES = 5;
    static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public PublicBookingLookupRateLimiter() {
        this(Clock.systemUTC());
    }

    PublicBookingLookupRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Runs one credential lookup atomically for a source. A null result is treated as
     * a failed authentication and never reveals which credential was incorrect.
     */
    public <T> T authenticate(String remoteSource, Supplier<T> credentialLookup) {
        String sourceKey = normalizeSource(remoteSource);
        AtomicReference<T> authenticated = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        Instant now = clock.instant();

        attempts.compute(sourceKey, (key, current) -> {
            AttemptState state = current == null ? new AttemptState() : current;
            if (state.blockedUntil != null) {
                if (now.isBefore(state.blockedUntil)) {
                    failure.set(new LookupRateLimitedException());
                    return state;
                }
                state.reset();
            }

            T result = credentialLookup.get();
            if (result == null) {
                state.failedAttempts += 1;
                if (state.failedAttempts >= MAX_FAILURES) {
                    state.blockedUntil = now.plus(BLOCK_DURATION);
                }
                failure.set(new LookupFailedException());
                return state;
            }

            authenticated.set(result);
            return null;
        });

        if (failure.get() != null) {
            throw failure.get();
        }
        return authenticated.get();
    }

    private String normalizeSource(String remoteSource) {
        if (!StringUtils.hasText(remoteSource)) {
            return "unknown";
        }
        String normalized = remoteSource.trim();
        return normalized.length() <= 128 ? normalized : normalized.substring(0, 128);
    }

    private static final class AttemptState {
        private int failedAttempts;
        private Instant blockedUntil;

        private void reset() {
            failedAttempts = 0;
            blockedUntil = null;
        }
    }

    public static final class LookupFailedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static final class LookupRateLimitedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
