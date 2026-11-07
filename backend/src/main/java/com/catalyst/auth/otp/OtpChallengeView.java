package com.catalyst.auth.otp;

import java.time.Instant;
import java.util.UUID;

/** Returned when a code has been sent. The email is masked so the response never confirms a full address. */
public record OtpChallengeView(UUID challengeId, OtpPurpose purpose, String emailHint, Instant expiresAt, Instant resendAvailableAt) {}
