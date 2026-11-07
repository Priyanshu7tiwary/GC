package com.catalyst.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Authenticates a request from its session cookie. Expired, tampered or revoked tokens are ignored (and the cookie cleared). */
@Component
class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenService tokens;
    private final SessionValidator sessions;
    private final SessionCookie cookie;

    JwtAuthenticationFilter(TokenService tokens, SessionValidator sessions, SessionCookie cookie) {
        this.tokens = tokens;
        this.sessions = sessions;
        this.cookie = cookie;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        cookie.read(req).ifPresent(token -> {
            var user = tokens.verify(token).filter(u -> sessions.isActive(u.id(), u.sessionVersion()));
            if (user.isPresent()) {
                var auth = new UsernamePasswordAuthenticationToken(user.get(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                cookie.clear(res);
            }
        });
        chain.doFilter(req, res);
    }
}
