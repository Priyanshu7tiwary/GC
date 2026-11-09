package com.catalyst.auth;

import com.catalyst.auth.dto.LoginRequest;
import com.catalyst.auth.dto.RegisterRequest;
import com.catalyst.auth.google.GoogleIdentity;
import com.catalyst.auth.google.GoogleIdentityVerifier;
import com.catalyst.auth.otp.OtpPurpose;
import com.catalyst.auth.otp.OtpService;
import com.catalyst.common.error.BadRequestException;
import com.catalyst.common.error.ConflictException;
import com.catalyst.common.error.TooManyRequestsException;
import com.catalyst.security.RateLimiter;
import com.catalyst.common.error.UnauthorizedException;
import com.catalyst.user.ExamCalendar;
import com.catalyst.user.User;
import com.catalyst.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final GoogleIdentityVerifier google;
    private final ExamCalendar calendar;
    private final Clock clock;
    private final RateLimiter limiter;
    private final OtpService otp;
    private final double defaultTarget;
    /** Compared against when the email is unknown, so a miss takes as long as a wrong password. */
    private final String dummyHash;

    AuthService(UserRepository users, PasswordEncoder encoder, GoogleIdentityVerifier google, ExamCalendar calendar, Clock clock,
                RateLimiter limiter, OtpService otp, @Value("${app.exam.default-target-percentile}") double defaultTarget) {
        this.limiter = limiter;
        this.otp = otp;
        this.users = users;
        this.encoder = encoder;
        this.google = google;
        this.calendar = calendar;
        this.clock = clock;
        this.defaultTarget = defaultTarget;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    User register(RegisterRequest req) {
        String email = normalize(req.email());
        checkPassword(req.password(), email);
        Optional<User> existing = users.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User u = existing.get();
            if (u.isEmailVerified() || u.getGoogleSubject() != null) {
                throw new ConflictException("An account with this email already exists. Log in instead.");
            }
            u.resetUnverified(req.name().trim(), encoder.encode(req.password())); // never verified, so nobody owns it yet
            return u;
        }
        int year = calendar.upcomingYear(LocalDate.now(clock));
        return users.save(new User(email, req.name().trim(), encoder.encode(req.password()),
                defaultTarget, year, calendar.examDate(year), clock.instant()));
    }

    @Transactional(readOnly = true)
    User login(LoginRequest req) {
        String email = normalize(req.email());
        // per-account cap, so guesses spread across many IPs are still stopped
        if (!limiter.tryAcquire("login:" + email, 10, java.time.Duration.ofMinutes(15))) {
            throw new TooManyRequestsException("Too many sign-in attempts for this account. Try again in 15 minutes.");
        }
        Optional<User> found = users.findByEmailIgnoreCase(email);
        String hash = found.filter(User::hasPassword).map(User::getPasswordHash).orElse(dummyHash);
        boolean ok = encoder.matches(req.password(), hash);
        if (!ok || found.isEmpty() || !found.get().hasPassword()) {
            throw new UnauthorizedException("Email or password is incorrect.");
        }
        return found.get();
    }

    /**
     * Step 1 of "forgot password": the account to email a reset code to, if there is one.
     * The caller answers the same way either way, so the response doesn't reveal whether the email is registered.
     */
    @Transactional(readOnly = true)
    Optional<User> findForPasswordReset(String rawEmail) {
        String email = normalize(rawEmail);
        if (!limiter.tryAcquire("reset:" + email, 5, java.time.Duration.ofHours(1))) {
            throw new TooManyRequestsException("Too many reset requests for this email. Try again in an hour.");
        }
        return users.findByEmailIgnoreCase(email);
    }

    /**
     * Step 2: a correct reset code sets the new password and signs out every existing session.
     * Wrong codes must still count against the challenge, so they don't roll the transaction back.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    User resetPassword(UUID challengeId, String code, String newPassword) {
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("Password is too long. Use at most 72 characters.");
        }
        User user = otp.verify(challengeId, code, OtpPurpose.PASSWORD_RESET);
        checkPassword(newPassword, user.getEmail()); // a rejected password rolls back, so the code can be used again
        user.changePassword(encoder.encode(newPassword));
        user.revokeSessions();
        return user;
    }

    private static void checkPassword(String password, String email) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("Password is too long. Use at most 72 characters.");
        }
        String e = email.toLowerCase();
        if (password.equalsIgnoreCase(e) || password.equalsIgnoreCase(e.split("@")[0])) {
            throw new BadRequestException("Choose a password that isn't your email address.");
        }
    }

    /** Finds the account for a Google identity, linking it to an existing email account or creating a new one. */
    @Transactional
    User loginWithGoogle(String credential) {
        GoogleIdentity id = google.verify(credential);
        return users.findByGoogleSubject(id.subject())
                .or(() -> users.findByEmailIgnoreCase(id.email()).map(u -> {
                    u.linkGoogle(id.subject()); // Google has verified this email, so it's safe to link
                    return u;
                }))
                .orElseGet(() -> {
                    int year = calendar.upcomingYear(LocalDate.now(clock));
                    String name = id.name() == null || id.name().isBlank() ? id.email().split("@")[0] : id.name().trim();
                    return users.save(User.fromGoogle(id.email(), truncate(name, 120), id.subject(), defaultTarget, year,
                            calendar.examDate(year), clock.instant()));
                });
    }

    /** Ends every session for this user, on every device. */
    @Transactional
    void logoutEverywhere(UUID userId) {
        users.findById(userId).ifPresent(User::revokeSessions);
    }

    @Transactional(readOnly = true)
    Optional<User> find(UUID userId) {
        return users.findById(userId);
    }

    String googleClientId() {
        return google.clientId();
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
