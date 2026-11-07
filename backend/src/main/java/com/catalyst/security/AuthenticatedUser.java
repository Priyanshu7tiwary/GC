package com.catalyst.security;

import java.util.UUID;

/** The principal placed in the security context for every authenticated request. */
public record AuthenticatedUser(UUID id, String email, int sessionVersion) {}
