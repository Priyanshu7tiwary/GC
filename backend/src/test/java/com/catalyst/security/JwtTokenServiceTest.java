package com.catalyst.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {
    private final JwtTokenService tokens = new JwtTokenService(
            new JwtProperties("a-test-secret-that-is-long-enough-for-hs256!!", Duration.ofHours(1)), Clock.systemUTC());

    @Test
    void roundTripsIdentityAndSessionVersion() {
        var me = new AuthenticatedUser(UUID.randomUUID(), "riya@example.com", 3);
        assertThat(tokens.verify(tokens.issue(me))).contains(me);
    }

    @Test
    void rejectsTamperedTokens() {
        String t = tokens.issue(new AuthenticatedUser(UUID.randomUUID(), "a@b.co", 0));
        String tampered = t.substring(0, t.length() - 3) + (t.endsWith("AAA") ? "BBB" : "AAA");
        assertThat(tokens.verify(tampered)).isEmpty();
        assertThat(tokens.verify("not-a-token")).isEmpty();
    }

    @Test
    void rejectsTokensSignedWithAnotherKey() {
        var other = new JwtTokenService(new JwtProperties("another-secret-that-is-long-enough-for-hs256", Duration.ofHours(1)), Clock.systemUTC());
        assertThat(tokens.verify(other.issue(new AuthenticatedUser(UUID.randomUUID(), "a@b.co", 0)))).isEmpty();
    }

    @Test
    void rejectsExpiredTokens() {
        var shortLived = new JwtTokenService(new JwtProperties("a-test-secret-that-is-long-enough-for-hs256!!", Duration.ofSeconds(-1)), Clock.systemUTC());
        assertThat(tokens.verify(shortLived.issue(new AuthenticatedUser(UUID.randomUUID(), "a@b.co", 0)))).isEmpty();
    }

    @Test
    void refusesToStartWithAWeakOrMissingSecret() {
        assertThatThrownBy(() -> new JwtTokenService(new JwtProperties("", Duration.ofHours(1)), Clock.systemUTC()))
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new JwtTokenService(new JwtProperties("short", Duration.ofHours(1)), Clock.systemUTC()))
                .hasMessageContaining("32 bytes");
    }
}
