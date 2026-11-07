package com.catalyst.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {
    @Test
    void blocksAfterTheLimitAndRecoversAfterTheWindow() {
        var clock = new MutableClock();
        var limiter = new RateLimiter(clock);
        for (int i = 0; i < 3; i++) assertThat(limiter.tryAcquire("ip", 3, Duration.ofMinutes(5))).isTrue();
        assertThat(limiter.tryAcquire("ip", 3, Duration.ofMinutes(5))).isFalse();
        assertThat(limiter.tryAcquire("other-ip", 3, Duration.ofMinutes(5))).isTrue();
        clock.now = clock.now.plus(Duration.ofMinutes(6));
        assertThat(limiter.tryAcquire("ip", 3, Duration.ofMinutes(5))).isTrue();
    }

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-07T10:00:00Z");
        public ZoneOffset getZone() { return ZoneOffset.UTC; }
        public Clock withZone(java.time.ZoneId z) { return this; }
        public Instant instant() { return now; }
    }
}
