package com.catalyst.auth.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {
    long countByUserIdAndCreatedAtAfter(UUID userId, Instant since);

    /** A new challenge supersedes any older open ones for the same user. */
    @Modifying
    @Query("update OtpChallenge c set c.consumedAt = :now where c.userId = :userId and c.consumedAt is null")
    void closeOpenChallenges(@Param("userId") UUID userId, @Param("now") Instant now);
}
