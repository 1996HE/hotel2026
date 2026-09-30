package com.example.minshuku.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.minshuku.service.PublicBookingLookupRateLimiter.LookupFailedException;
import com.example.minshuku.service.PublicBookingLookupRateLimiter.LookupRateLimitedException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PublicBookingLookupRateLimiterTest {
    @Test
    void fifthFailureStartsFifteenMinuteBlock() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-08T00:00:00Z"));
        PublicBookingLookupRateLimiter limiter = new PublicBookingLookupRateLimiter(clock);

        for (int attempt = 0; attempt < 5; attempt += 1) {
            assertThrows(LookupFailedException.class, () -> limiter.authenticate("203.0.113.1", () -> null));
        }
        assertThrows(LookupRateLimitedException.class,
                () -> limiter.authenticate("203.0.113.1", () -> "valid"));

        clock.advance(Duration.ofMinutes(15));
        assertEquals("valid", limiter.authenticate("203.0.113.1", () -> "valid"));
    }

    @Test
    void successClearsConsecutiveFailureCount() {
        PublicBookingLookupRateLimiter limiter = new PublicBookingLookupRateLimiter(
                Clock.fixed(Instant.parse("2026-09-08T00:00:00Z"), ZoneOffset.UTC));

        for (int attempt = 0; attempt < 4; attempt += 1) {
            assertThrows(LookupFailedException.class, () -> limiter.authenticate("203.0.113.2", () -> null));
        }
        assertEquals("valid", limiter.authenticate("203.0.113.2", () -> "valid"));
        for (int attempt = 0; attempt < 5; attempt += 1) {
            assertThrows(LookupFailedException.class, () -> limiter.authenticate("203.0.113.2", () -> null));
        }
        assertThrows(LookupRateLimitedException.class,
                () -> limiter.authenticate("203.0.113.2", () -> "valid"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
