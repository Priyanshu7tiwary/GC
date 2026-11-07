package com.catalyst.auth.dto;

import com.catalyst.user.dto.ProfileResponse;

/** Who is signed in (if anyone) and which sign-in methods the server offers. */
public record SessionResponse(boolean authenticated, ProfileResponse user, Providers providers) {
    public record Providers(boolean password, String googleClientId) {}
}
