package com.catalyst.auth.otp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;
    @Column(name = "code_hash", nullable = false)
    private String codeHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private int attempts;
    @Column(nullable = false)
    private int sends;
    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OtpChallenge() {}

    /** The id is chosen by the caller because the code hash is bound to it. */
    OtpChallenge(UUID id, UUID userId, OtpPurpose purpose, String codeHash, Instant expiresAt, Instant now) {
        this.id = id;
        this.userId = userId;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.sends = 1;
        this.lastSentAt = now;
        this.createdAt = now;
    }

    boolean isOpen(Instant now) {
        return consumedAt == null && now.isBefore(expiresAt);
    }

    void recordFailedAttempt() { attempts++; }

    void consume(Instant now) { consumedAt = now; }

    void reissue(String newHash, Instant expires, Instant now) {
        codeHash = newHash;
        expiresAt = expires;
        attempts = 0;
        sends++;
        lastSentAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public OtpPurpose getPurpose() { return purpose; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getAttempts() { return attempts; }
    public int getSends() { return sends; }
    public Instant getLastSentAt() { return lastSentAt; }
}
