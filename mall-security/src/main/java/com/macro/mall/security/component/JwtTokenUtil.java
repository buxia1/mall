package com.macro.mall.security.component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenUtil {
    private static final String ADMIN_ID_CLAIM = "adminId";

    private final SecretKey signingKey;
    private final long expirationSeconds;
    private final Clock clock;

    /**
     * 有两个构造器，必须显式标注哪一个供 Spring 注入；另一个是给测试传固定 {@link Clock} 用的。
     */
    @Autowired
    public JwtTokenUtil(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public JwtTokenUtil(JwtProperties properties, Clock clock) {
        this.signingKey = signingKey(properties.getSecret());
        this.expirationSeconds = properties.getExpirationSeconds();
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        this.clock = clock;
    }

    public String generateToken(Long adminId, String username) {
        if (adminId == null || username == null || username.isBlank()) {
            throw new IllegalArgumentException("Administrator identity is required");
        }
        Instant issuedAt = clock.instant();
        return Jwts.builder()
                .subject(username)
                .claim(ADMIN_ID_CLAIM, adminId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(expirationSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public Optional<AdminIdentity> getIdentity(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long adminId = claims.get(ADMIN_ID_CLAIM, Long.class);
            String username = claims.getSubject();
            return adminId == null || username == null || username.isBlank()
                    ? Optional.empty()
                    : Optional.of(new AdminIdentity(adminId, username));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public boolean isValid(String token) {
        return getIdentity(token).isPresent();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private SecretKey signingKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret is required");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
