package com.catalyst.auth.google;

/** Verifies a Google Identity Services credential (an ID token) issued for this app. */
public interface GoogleIdentityVerifier {
    boolean isConfigured();

    /** The OAuth client ID the frontend must use; null when Google sign-in is not configured. */
    String clientId();

    /** @throws com.catalyst.common.error.UnauthorizedException if the token is invalid, expired, or not for this app */
    GoogleIdentity verify(String idToken);
}
