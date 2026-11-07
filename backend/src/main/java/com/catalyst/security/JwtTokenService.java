package com.catalyst.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
class JwtTokenService implements TokenService {
    private static final String ISSUER = "catalyst-api";
    private static final String AUDIENCE = "catalyst-web";

    private final SecretKey key;
    private final JwtProperties props;
    private final Clock clock;

    JwtTokenService(JwtProperties props, Clock clock) {
        if (props.secret() == null || props.secret().isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not set. Generate one with: openssl rand -base64 48");
        }
        byte[] bytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes long.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.props = props;
        this.clock = clock;
    }

    @Override
    public String issue(AuthenticatedUser user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(user.id().toString())
                .claim("email", user.email())
                .claim("sv", user.sessionVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.ttl())))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public Optional<AuthenticatedUser> verify(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .requireAudience(AUDIENCE)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token).getPayload();
            Integer sv = claims.get("sv", Integer.class);
            return Optional.of(new AuthenticatedUser(UUID.fromString(claims.getSubject()), claims.get("email", String.class),
                    sv == null ? -1 : sv));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public java.time.Duration ttl() {
        return props.ttl();
    }
}
