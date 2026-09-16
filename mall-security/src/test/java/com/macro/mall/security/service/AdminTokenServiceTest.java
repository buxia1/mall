package com.macro.mall.security.service;

import com.macro.mall.common.service.RedisService;
import com.macro.mall.security.component.JwtProperties;
import com.macro.mall.security.component.JwtTokenUtil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.clearInvocations;

class AdminTokenServiceTest {
    private static final String SECRET = "security-test-secret-that-is-at-least-thirty-two-bytes";

    @Test
    void logoutRevokesAnOtherwiseValidToken() {
        RedisService redisService = mock(RedisService.class);
        AdminTokenService service = service(redisService);
        String token = service.login(7L, "alice");
        when(redisService.hasKey(anyString())).thenReturn(true);

        service.logout(token);

        verify(redisService).remove(anyString());
        when(redisService.hasKey(anyString())).thenReturn(false);
        assertThat(service.isActive(token)).isFalse();
    }

    @Test
    void refreshExtendsTtlOnlyForAnActiveValidToken() {
        RedisService redisService = mock(RedisService.class);
        AdminTokenService service = service(redisService);
        String token = service.login(7L, "alice");
        when(redisService.hasKey(anyString())).thenReturn(true);
        when(redisService.expire(anyString(), eq(600L))).thenReturn(true);

        assertThat(service.refresh(token)).isEqualTo(token);
        verify(redisService).expire(anyString(), eq(600L));

        clearInvocations(redisService);
        when(redisService.hasKey(anyString())).thenReturn(false);
        assertThat(service.refresh(token)).isNull();
        verify(redisService, never()).expire(anyString(), eq(600L));
    }

    @Test
    void malformedTokenDoesNotCreateOrRefreshASession() {
        RedisService redisService = mock(RedisService.class);
        AdminTokenService service = service(redisService);

        assertThat(service.isActive("malformed")).isFalse();
        assertThat(service.refresh("malformed")).isNull();

        verify(redisService, never()).hasKey(anyString());
        verify(redisService, never()).expire(anyString(), eq(600L));
    }

    @Test
    void refreshIsRejectedWhenTheSessionDisappearsBeforeTtlExtension() {
        RedisService redisService = mock(RedisService.class);
        AdminTokenService service = service(redisService);
        String token = service.login(7L, "alice");
        when(redisService.hasKey(anyString())).thenReturn(true);
        when(redisService.expire(anyString(), eq(600L))).thenReturn(false);

        assertThat(service.refresh(token)).isNull();
    }

    private AdminTokenService service(RedisService redisService) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpirationSeconds(600);
        JwtTokenUtil tokenUtil = new JwtTokenUtil(properties,
                Clock.fixed(Instant.parse("2026-09-16T00:00:00Z"), ZoneOffset.UTC));
        return new AdminTokenService(redisService, tokenUtil, properties);
    }
}
