package com.catalyst.auth.otp;

import com.catalyst.common.error.BadRequestException;
import com.catalyst.common.error.TooManyRequestsException;
import com.catalyst.common.error.UnauthorizedException;
import com.catalyst.user.User;
import com.catalyst.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Issues, re-sends and checks one-time codes. */
@Service
public class OtpService {
    static final Duration VALID_FOR = Duration.ofMinutes(10);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);
    static final int MAX_ATTEMPTS = 5;
    static final int MAX_SENDS = 5;
    static final int MAX_CHALLENGES_PER_HOUR = 10;

    private final OtpChallengeRepository challenges;
    private final UserRepository users;
    private final OtpCodes codes;
    private final OtpSender sender;
    private final Clock clock;

    public OtpService(OtpChallengeRepository challenges, UserRepository users, OtpCodes codes, OtpSender sender, Clock clock) {
        this.challenges = challenges;
        this.users = users;
        this.codes = codes;
        this.sender = sender;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = TooManyRequestsException.class)
    public OtpChallengeView start(User user, OtpPurpose purpose) {
        Instant now = clock.instant();
        if (challenges.countByUserIdAndCreatedAtAfter(user.getId(), now.minus(Duration.ofHours(1))) >= MAX_CHALLENGES_PER_HOUR) {
            throw new TooManyRequestsException("Too many codes requested for this account. Try again in an hour.");
        }
        challenges.closeOpenChallenges(user.getId(), now);
        String code = codes.newCode();
        UUID id = UUID.randomUUID();
        OtpChallenge c = challenges.save(new OtpChallenge(id, user.getId(), purpose, codes.hash(id, code), now.plus(VALID_FOR), now));
        sender.send(user.getEmail(), user.getName(), code, purpose, VALID_FOR);
        return view(c, user);
    }

    @Transactional
    public OtpChallengeView resend(UUID challengeId) {
        Instant now = clock.instant();
        OtpChallenge c = open(challengeId, now);
        if (now.isBefore(c.getLastSentAt().plus(RESEND_COOLDOWN))) {
            throw new TooManyRequestsException("Wait a few seconds before asking for another code.");
        }
        if (c.getSends() >= MAX_SENDS) {
            throw new TooManyRequestsException("Too many codes sent. Start again from the sign-in page.");
        }
        User user = users.findById(c.getUserId()).orElseThrow(OtpService::invalid);
        String code = codes.newCode();
        c.reissue(codes.hash(c.getId(), code), now.plus(VALID_FOR), now);
        sender.send(user.getEmail(), user.getName(), code, c.getPurpose(), VALID_FOR);
        return view(c, user);
    }

    /** Checks the code; on success the challenge is used up and the (now email-verified) user is returned. */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public User verify(UUID challengeId, String code) {
        Instant now = clock.instant();
        OtpChallenge c = open(challengeId, now);
        if (c.getAttempts() >= MAX_ATTEMPTS) {
            c.consume(now);
            throw new UnauthorizedException("Too many wrong codes. Start again to get a new one.");
        }
        if (code == null || !code.matches("\\d{6}") || !codes.matches(c.getId(), code, c.getCodeHash())) {
            c.recordFailedAttempt();
            int left = MAX_ATTEMPTS - c.getAttempts();
            if (left <= 0) c.consume(now);
            throw new UnauthorizedException(left > 0
                    ? "That code is incorrect. " + left + " " + (left == 1 ? "try" : "tries") + " left."
                    : "Too many wrong codes. Start again to get a new one.");
        }
        c.consume(now);
        User user = users.findById(c.getUserId()).orElseThrow(OtpService::invalid);
        user.markEmailVerified();
        return user;
    }

    private OtpChallenge open(UUID id, Instant now) {
        OtpChallenge c = challenges.findById(id).orElseThrow(OtpService::invalid);
        if (!c.isOpen(now)) throw new BadRequestException("This code has expired. Request a new one.");
        return c;
    }

    private static UnauthorizedException invalid() {
        return new UnauthorizedException("This code is no longer valid. Start again.");
    }

    private OtpChallengeView view(OtpChallenge c, User u) {
        return new OtpChallengeView(c.getId(), c.getPurpose(), mask(u.getEmail()), c.getExpiresAt(), c.getLastSentAt().plus(RESEND_COOLDOWN));
    }

    static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "•••" + email.substring(Math.max(at, 0));
        return email.charAt(0) + "•••" + email.charAt(at - 1) + email.substring(at);
    }
}
