package com.catalyst.auth.otp;

import com.catalyst.common.error.BadRequestException;
import com.catalyst.common.error.TooManyRequestsException;
import com.catalyst.common.error.UnauthorizedException;
import com.catalyst.user.User;
import com.catalyst.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OtpServiceTest {
    private final MutableClock clock = new MutableClock();
    private final Map<UUID, OtpChallenge> store = new HashMap<>();
    private final CapturingSender sender = new CapturingSender();
    private final User user = new User("riya@example.com", "Riya", "hash", 99, 2026, LocalDate.of(2026, 11, 29), Instant.now());
    private OtpService otp;

    @BeforeEach
    void setUp() {
        OtpChallengeRepository repo = mock(OtpChallengeRepository.class);
        when(repo.save(any())).thenAnswer(i -> { OtpChallenge c = i.getArgument(0); store.put(c.getId(), c); return c; });
        when(repo.findById(any())).thenAnswer(i -> Optional.ofNullable(store.get(i.<UUID>getArgument(0))));
        UserRepository users = mock(UserRepository.class);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        otp = new OtpService(repo, users, new OtpCodes("an-otp-secret-that-is-at-least-32-chars"), sender, clock);
    }

    @Test
    void correctCodeVerifiesTheEmailAndIsSingleUse() {
        var view = otp.start(user, OtpPurpose.SIGN_UP);
        assertThat(sender.code).matches("\\d{6}");
        assertThat(store.get(view.challengeId()).getCodeHash()).doesNotContain(sender.code);
        assertThat(view.emailHint()).isEqualTo("r•••a@example.com");

        assertThat(otp.verify(view.challengeId(), sender.code)).isSameAs(user);
        assertThat(user.isEmailVerified()).isTrue();
        assertThatThrownBy(() -> otp.verify(view.challengeId(), sender.code)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void locksAfterFiveWrongCodes() {
        var view = otp.start(user, OtpPurpose.SIGN_IN);
        String wrong = sender.code.equals("000000") ? "111111" : "000000";
        for (int i = 4; i >= 1; i--) {
            int left = i;
            assertThatThrownBy(() -> otp.verify(view.challengeId(), wrong)).isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining(left + " tr");
        }
        assertThatThrownBy(() -> otp.verify(view.challengeId(), wrong)).hasMessageContaining("Too many wrong codes");
        // even the right code no longer works
        assertThatThrownBy(() -> otp.verify(view.challengeId(), sender.code)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void expiresAfterTenMinutes() {
        var view = otp.start(user, OtpPurpose.SIGN_IN);
        clock.now = clock.now.plus(Duration.ofMinutes(11));
        assertThatThrownBy(() -> otp.verify(view.challengeId(), sender.code)).hasMessageContaining("expired");
    }

    @Test
    void resendHasACooldownAndReplacesTheCode() {
        var view = otp.start(user, OtpPurpose.SIGN_IN);
        String first = sender.code;
        assertThatThrownBy(() -> otp.resend(view.challengeId())).isInstanceOf(TooManyRequestsException.class);
        clock.now = clock.now.plusSeconds(31);
        otp.resend(view.challengeId());
        if (!first.equals(sender.code)) {
            assertThatThrownBy(() -> otp.verify(view.challengeId(), first)).isInstanceOf(UnauthorizedException.class);
        }
        assertThat(otp.verify(view.challengeId(), sender.code)).isSameAs(user);
    }

    @Test
    void rejectsMalformedCodesWithoutCrashing() {
        var view = otp.start(user, OtpPurpose.SIGN_IN);
        assertThatThrownBy(() -> otp.verify(view.challengeId(), "12ab56")).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> otp.verify(UUID.randomUUID(), "123456")).isInstanceOf(UnauthorizedException.class);
    }

    static class CapturingSender implements OtpSender {
        String code;
        public void send(String email, String name, String code, OtpPurpose purpose, Duration validFor) { this.code = code; }
    }

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-07T10:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId z) { return this; }
        public Instant instant() { return now; }
    }
}
