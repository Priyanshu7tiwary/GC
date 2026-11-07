package com.catalyst.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Throttles auth endpoints per client IP: credential submissions tightly (password guessing, sign-up spam),
 * code entry more loosely (each code already allows only 5 tries).
 */
class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Duration WINDOW = Duration.ofMinutes(5);
    private static final int CREDENTIAL_LIMIT = 10;
    private static final int CODE_LIMIT = 30;

    private final RateLimiter limiter;

    AuthRateLimitFilter(RateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !("POST".equals(req.getMethod()) && req.getRequestURI().startsWith("/api/auth/")
                && !req.getRequestURI().equals("/api/auth/logout"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        boolean code = req.getRequestURI().startsWith("/api/auth/otp/");
        String bucket = (code ? "otp:" : "cred:") + req.getRemoteAddr();
        if (!limiter.tryAcquire(bucket, code ? CODE_LIMIT : CREDENTIAL_LIMIT, WINDOW)) {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
            res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            res.getWriter().write("{\"status\":429,\"title\":\"Too Many Requests\",\"detail\":\"Too many sign-in attempts. Wait a few minutes and try again.\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
