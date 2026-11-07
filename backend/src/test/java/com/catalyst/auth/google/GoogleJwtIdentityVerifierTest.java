package com.catalyst.auth.google;

import com.catalyst.common.error.ServiceUnavailableException;
import com.catalyst.common.error.UnauthorizedException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleJwtIdentityVerifierTest {
    private static final String CLIENT = "123-abc.apps.googleusercontent.com";
    private final KeyPair googleKey = rsa();
    private final GoogleJwtIdentityVerifier verifier = new GoogleJwtIdentityVerifier(CLIENT,
            NimbusJwtDecoder.withPublicKey((RSAPublicKey) googleKey.getPublic()).build());

    @Test
    void acceptsAValidToken() {
        var id = verifier.verify(token(googleKey, "https://accounts.google.com", CLIENT, Instant.now().plusSeconds(600), true));
        assertThat(id.subject()).isEqualTo("google-sub-1");
        assertThat(id.email()).isEqualTo("riya@example.com");
    }

    @Test
    void rejectsATokenForAnotherApp() {
        assertThatThrownBy(() -> verifier.verify(token(googleKey, "accounts.google.com", "someone-else", Instant.now().plusSeconds(600), true)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsAForeignIssuer() {
        assertThatThrownBy(() -> verifier.verify(token(googleKey, "https://evil.example", CLIENT, Instant.now().plusSeconds(600), true)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsAnExpiredToken() {
        assertThatThrownBy(() -> verifier.verify(token(googleKey, "accounts.google.com", CLIENT, Instant.now().minusSeconds(3600), true)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsAnUnverifiedEmail() {
        assertThatThrownBy(() -> verifier.verify(token(googleKey, "accounts.google.com", CLIENT, Instant.now().plusSeconds(600), false)))
                .isInstanceOf(UnauthorizedException.class).hasMessageContaining("verified");
    }

    @Test
    void rejectsAForgedSignature() {
        assertThatThrownBy(() -> verifier.verify(token(rsa(), "accounts.google.com", CLIENT, Instant.now().plusSeconds(600), true)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refusesWhenNotConfigured() {
        var off = new GoogleJwtIdentityVerifier("", NimbusJwtDecoder.withPublicKey((RSAPublicKey) googleKey.getPublic()).build());
        assertThat(off.isConfigured()).isFalse();
        assertThatThrownBy(() -> off.verify("x")).isInstanceOf(ServiceUnavailableException.class);
    }

    private static String token(KeyPair key, String iss, String aud, Instant exp, boolean emailVerified) {
        try {
            var claims = new JWTClaimsSet.Builder().issuer(iss).audience(aud).subject("google-sub-1")
                    .claim("email", "Riya@Example.com").claim("email_verified", emailVerified).claim("name", "Riya Sharma")
                    .issueTime(Date.from(exp.minusSeconds(3600))).expirationTime(Date.from(exp)).build();
            var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner(key.getPrivate()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static KeyPair rsa() {
        try {
            var g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            return g.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
