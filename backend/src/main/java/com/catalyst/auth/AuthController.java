package com.catalyst.auth;

import com.catalyst.auth.dto.GoogleLoginRequest;
import com.catalyst.auth.dto.LoginRequest;
import com.catalyst.auth.dto.RegisterRequest;
import com.catalyst.auth.dto.ResendOtpRequest;
import com.catalyst.auth.dto.VerifyOtpRequest;
import com.catalyst.auth.otp.OtpChallengeView;
import com.catalyst.auth.otp.OtpPurpose;
import com.catalyst.auth.otp.OtpService;
import com.catalyst.auth.dto.SessionResponse;
import com.catalyst.security.AuthenticatedUser;
import com.catalyst.security.SessionCookie;
import com.catalyst.security.TokenService;
import com.catalyst.user.User;
import com.catalyst.user.dto.ProfileResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Session endpoints. Successful sign-in sets an HttpOnly session cookie; the token is never returned in the body.
 * All POSTs require the X-XSRF-TOKEN header (read GET /api/auth/session first to receive the XSRF-TOKEN cookie).
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {
    private final AuthService auth;
    private final OtpService otp;
    private final TokenService tokens;
    private final SessionCookie cookie;
    private final Clock clock;

    AuthController(AuthService auth, OtpService otp, TokenService tokens, SessionCookie cookie, Clock clock) {
        this.auth = auth;
        this.otp = otp;
        this.tokens = tokens;
        this.cookie = cookie;
        this.clock = clock;
    }

    @GetMapping("/session")
    SessionResponse session(@AuthenticationPrincipal AuthenticatedUser me) {
        var providers = new SessionResponse.Providers(true, auth.googleClientId());
        if (me == null) return new SessionResponse(false, null, providers);
        return auth.find(me.id())
                .map(u -> new SessionResponse(true, ProfileResponse.of(u, LocalDate.now(clock)), providers))
                .orElseGet(() -> new SessionResponse(false, null, providers));
    }

    /** Creates the account and emails a verification code. No session until the code is verified. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    OtpChallengeView register(@Valid @RequestBody RegisterRequest req) {
        return otp.start(auth.register(req), OtpPurpose.SIGN_UP);
    }

    /** Step 1 of password sign-in: checks the password, then emails a one-time code. */
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.ACCEPTED)
    OtpChallengeView login(@Valid @RequestBody LoginRequest req) {
        return otp.start(auth.login(req), OtpPurpose.SIGN_IN);
    }

    /** Step 2: a correct code starts the session. */
    @PostMapping("/otp/verify")
    ProfileResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest req, HttpServletResponse res) {
        return start(otp.verify(req.challengeId(), req.code()), res);
    }

    @PostMapping("/otp/resend")
    OtpChallengeView resendOtp(@Valid @RequestBody ResendOtpRequest req) {
        return otp.resend(req.challengeId());
    }

    @PostMapping("/google")
    ProfileResponse google(@Valid @RequestBody GoogleLoginRequest req, HttpServletResponse res) {
        return start(auth.loginWithGoogle(req.credential()), res);
    }

    /** Signs out everywhere: revokes all of this user's tokens and clears the cookie. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@AuthenticationPrincipal AuthenticatedUser me, HttpServletResponse res) {
        if (me != null) auth.logoutEverywhere(me.id());
        cookie.clear(res);
    }

    private ProfileResponse start(User user, HttpServletResponse res) {
        String token = tokens.issue(new AuthenticatedUser(user.getId(), user.getEmail(), user.getSessionVersion()));
        cookie.write(res, token, tokens.ttl());
        return ProfileResponse.of(user, LocalDate.now(clock));
    }
}
