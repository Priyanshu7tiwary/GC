package com.catalyst.auth.google;

import com.catalyst.common.error.ServiceUnavailableException;
import com.catalyst.common.error.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * Checks the token's RS256 signature against Google's published keys, and that it was issued by Google,
 * for our client ID, is unexpired, and carries a verified email.
 */
@Component
class GoogleJwtIdentityVerifier implements GoogleIdentityVerifier {
    static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private final JwtDecoder decoder;

    @Autowired
    GoogleJwtIdentityVerifier(@Value("${app.google.client-id:}") String clientId,
                              @Value("${app.google.jwk-set-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwkSetUri) {
        this(clientId, NimbusJwtDecoder.withJwkSetUri(jwkSetUri).jwsAlgorithm(SignatureAlgorithm.RS256).build());
    }

    /** Visible for tests: plug in a decoder backed by a local key. */
    GoogleJwtIdentityVerifier(String clientId, JwtDecoder decoder) {
        this.clientId = clientId == null || clientId.isBlank() ? null : clientId.trim();
        this.decoder = decoder;
        if (decoder instanceof NimbusJwtDecoder nimbus) {
            nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    new JwtTimestampValidator(),
                    new JwtClaimValidator<Object>("iss", iss -> iss != null && ISSUERS.contains(iss.toString())),
                    new JwtClaimValidator<Collection<String>>("aud", aud -> aud != null && this.clientId != null && aud.contains(this.clientId))));
        }
    }

    @Override
    public boolean isConfigured() {
        return clientId != null;
    }

    @Override
    public String clientId() {
        return clientId;
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        if (!isConfigured()) throw new ServiceUnavailableException("Google sign-in isn't configured on this server.");
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new UnauthorizedException("Google sign-in failed. Try again.");
        }
        Object verified = jwt.getClaims().get("email_verified");
        String email = jwt.getClaimAsString("email");
        if (email == null || !(Boolean.TRUE.equals(verified) || "true".equals(String.valueOf(verified)))) {
            throw new UnauthorizedException("Your Google account's email address isn't verified.");
        }
        return new GoogleIdentity(jwt.getSubject(), email.trim().toLowerCase(), jwt.getClaimAsString("name"));
    }
}
