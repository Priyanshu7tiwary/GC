package com.catalyst.auth.otp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Generates 6-digit codes and stores only an HMAC of them, keyed with a server secret and bound to the challenge id,
 * so a leaked database row can't be brute-forced offline or replayed against another challenge.
 */
@Component
class OtpCodes {
    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    OtpCodes(@Value("${app.otp.secret:${app.jwt.secret}}") String secret) {
        if (secret == null || secret.length() < 32) throw new IllegalStateException("OTP secret must be at least 32 characters.");
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    String newCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    String hash(UUID challengeId, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal((challengeId + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Constant-time comparison. */
    boolean matches(UUID challengeId, String code, String storedHash) {
        return MessageDigest.isEqual(hash(challengeId, code).getBytes(StandardCharsets.US_ASCII),
                storedHash.getBytes(StandardCharsets.US_ASCII));
    }
}
