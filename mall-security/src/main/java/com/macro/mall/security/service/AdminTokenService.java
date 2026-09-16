package com.macro.mall.security.service;

import com.macro.mall.common.service.RedisService;
import com.macro.mall.security.component.AdminIdentity;
import com.macro.mall.security.component.JwtProperties;
import com.macro.mall.security.component.JwtTokenUtil;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 后台登录会话：JWT 承载身份，Redis 承载可撤销的会话状态。
 *
 * <p>不做 {@code @ConditionalOnBean(RedisService.class)} 条件装配：本类由组件扫描发现，
 * 条件在扫描阶段求值，那时 RedisService 还没注册，条件恒为 false。会话必须依赖 Redis，
 * 缺失时应让启动失败而不是悄悄丢掉登录能力。
 */
@Service
public class AdminTokenService {
    private static final String SESSION_KEY_PREFIX = "mall:security:admin-token:";

    private final RedisService redisService;
    private final JwtTokenUtil jwtTokenUtil;
    private final long sessionTtlSeconds;

    public AdminTokenService(RedisService redisService, JwtTokenUtil jwtTokenUtil, JwtProperties properties) {
        this.redisService = redisService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.sessionTtlSeconds = properties.getExpirationSeconds();
        if (sessionTtlSeconds <= 0) {
            throw new IllegalArgumentException("Session TTL must be positive");
        }
    }

    public String login(Long adminId, String username) {
        String token = jwtTokenUtil.generateToken(adminId, username);
        redisService.set(sessionKey(token), adminId, sessionTtlSeconds);
        return token;
    }

    public boolean isActive(String token) {
        return identity(token).isPresent() && Boolean.TRUE.equals(redisService.hasKey(sessionKey(token)));
    }

    public void logout(String token) {
        if (identity(token).isPresent()) {
            redisService.remove(sessionKey(token));
        }
    }

    public String refresh(String token) {
        if (!isActive(token)) {
            return null;
        }
        return Boolean.TRUE.equals(redisService.expire(sessionKey(token), sessionTtlSeconds)) ? token : null;
    }

    private Optional<AdminIdentity> identity(String token) {
        return jwtTokenUtil.getIdentity(token);
    }

    private String sessionKey(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return SESSION_KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
