package com.catalyst.user;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPolicyTest {
    private final AdminPolicy policy = new AdminPolicy(" Admin@Example.com , other@example.com");

    private static User user(String email, boolean verified) {
        User u = new User(email, "Test", "hash", 99, 2026, LocalDate.of(2026, 11, 29), Instant.EPOCH);
        if (verified) u.markEmailVerified();
        return u;
    }

    @Test
    void listedVerifiedEmailIsAdminIgnoringCase() {
        assertThat(policy.isAdmin(user("admin@example.COM", true))).isTrue();
    }

    @Test
    void listedButUnverifiedEmailIsNotAdmin() {
        assertThat(policy.isAdmin(user("admin@example.com", false))).isFalse();
    }

    @Test
    void unlistedUserIsNotAdmin() {
        assertThat(policy.isAdmin(user("someone@example.com", true))).isFalse();
        assertThat(new AdminPolicy("").isAdmin(user("someone@example.com", true))).isFalse();
    }
}
