package com.catalyst.security;

import java.util.UUID;

/** Confirms a token's session hasn't been revoked (e.g. by logging out). */
public interface SessionValidator {
    boolean isActive(UUID userId, int sessionVersion);
}
