package com.catalyst.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * The session token lives in an HttpOnly cookie, so page scripts (and any injected script) can never read it.
 * SameSite=Lax plus the CSRF token stops other sites from using it.
 */
@Component
public class SessionCookie {
    public static final String NAME = "catalyst_session";

    private final boolean secure;

    SessionCookie(@Value("${app.cookie.secure:true}") boolean secure) {
        this.secure = secure;
    }

    public void write(HttpServletResponse res, String token, Duration ttl) {
        res.addHeader(HttpHeaders.SET_COOKIE, build(token, ttl).toString());
    }

    public void clear(HttpServletResponse res) {
        res.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO).toString());
    }

    public Optional<String> read(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies).filter(c -> NAME.equals(c.getName())).map(Cookie::getValue)
                .filter(v -> !v.isBlank()).findFirst();
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
