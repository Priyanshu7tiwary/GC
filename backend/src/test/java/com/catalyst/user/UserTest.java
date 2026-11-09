package com.catalyst.user;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {
    private User newUser() {
        return new User("riya@example.com", "Riya", "attacker-chosen-hash", 99, 2026, LocalDate.of(2026, 11, 29), Instant.now());
    }

    @Test
    void linkingGoogleToAnUnverifiedAccountDropsItsPasswordAndSessions() {
        User u = newUser(); // registered by someone who never confirmed the email
        int before = u.getSessionVersion();
        u.linkGoogle("google-sub");
        assertThat(u.hasPassword()).isFalse();
        assertThat(u.getSessionVersion()).isGreaterThan(before);
        assertThat(u.isEmailVerified()).isTrue();
    }

    @Test
    void linkingGoogleToAVerifiedAccountKeepsItsPassword() {
        User u = newUser();
        u.markEmailVerified();
        int before = u.getSessionVersion();
        u.linkGoogle("google-sub");
        assertThat(u.hasPassword()).isTrue();
        assertThat(u.getSessionVersion()).isEqualTo(before);
    }
}
