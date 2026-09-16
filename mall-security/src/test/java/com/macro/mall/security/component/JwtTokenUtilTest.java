package com.macro.mall.security.component;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenUtilTest {
    private static final String SECRET = "security-test-secret-that-is-at-least-thirty-two-bytes";
    private static final Instant ISSUED_AT = Instant.parse("2026-09-16T00:00:00Z");

    @Test
    void validTokenExposesAdministratorIdentity() {
        JwtTokenUtil tokenUtil = tokenUtil(ISSUED_AT, 300);

        String token = tokenUtil.generateToken(42L, "admin");

        assertThat(tokenUtil.getIdentity(token))
                .contains(new AdminIdentity(42L, "admin"));
        assertThat(tokenUtil.isValid(token)).isTrue();
    }

    @Test
    void malformedTokenIsRejected() {
        JwtTokenUtil tokenUtil = tokenUtil(ISSUED_AT, 300);

        assertThat(tokenUtil.getIdentity("not.a.jwt")).isEmpty();
        assertThat(tokenUtil.isValid("not.a.jwt")).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        String token = tokenUtil(ISSUED_AT, 1).generateToken(42L, "admin");
        JwtTokenUtil expiredTokenUtil = tokenUtil(ISSUED_AT.plusSeconds(2), 300);

        assertThat(expiredTokenUtil.getIdentity(token)).isEmpty();
        assertThat(expiredTokenUtil.isValid(token)).isFalse();
    }

    private JwtTokenUtil tokenUtil(Instant instant, long expirationSeconds) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpirationSeconds(expirationSeconds);
        return new JwtTokenUtil(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }
}
