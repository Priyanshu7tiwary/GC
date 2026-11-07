package com.catalyst.security;

import java.time.Duration;
import java.util.Optional;

/** Issues and verifies session tokens. Abstracted so the token format can change without touching callers. */
public interface TokenService {
    String issue(AuthenticatedUser user);

    Optional<AuthenticatedUser> verify(String token);

    Duration ttl();
}
