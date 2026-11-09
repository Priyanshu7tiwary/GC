package com.catalyst.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides who may correct answer keys. An account is an admin when its {@code is_admin} flag is set in the
 * database, or when its verified email is listed in {@code ADMIN_EMAILS} (comma-separated).
 */
@Component
public class AdminPolicy {
    private final Set<String> adminEmails;

    public AdminPolicy(@Value("${app.admin-emails:}") String adminEmails) {
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(User user) {
        if (user.isAdmin()) return true;
        return user.isEmailVerified() && adminEmails.contains(user.getEmail().toLowerCase(Locale.ROOT));
    }
}
