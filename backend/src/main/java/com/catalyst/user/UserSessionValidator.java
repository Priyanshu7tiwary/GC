package com.catalyst.user;

import com.catalyst.security.SessionValidator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** A token is valid only while its session version matches the user's current one, and the user still exists. */
@Component
class UserSessionValidator implements SessionValidator {
    private final UserRepository users;

    UserSessionValidator(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(UUID userId, int sessionVersion) {
        return users.findSessionVersion(userId).map(v -> v == sessionVersion).orElse(false);
    }
}
