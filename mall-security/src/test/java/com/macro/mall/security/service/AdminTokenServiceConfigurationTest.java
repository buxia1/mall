package com.macro.mall.security.service;

import com.macro.mall.common.service.RedisService;
import com.macro.mall.security.component.JwtProperties;
import com.macro.mall.security.component.JwtTokenUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AdminTokenServiceConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TokenConfiguration.class);

    @Test
    void sessionServiceIsAvailableWhenRedisServiceExists() {
        contextRunner.withBean(RedisService.class, () -> mock(RedisService.class))
                .run(context -> assertThat(context).hasSingleBean(AdminTokenService.class));
    }

    @Test
    void sessionServiceIsNotCreatedWithoutTheConditionalRedisLayer() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(AdminTokenService.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AdminTokenService.class)
    static class TokenConfiguration {
        @Bean
        JwtProperties jwtProperties() {
            JwtProperties properties = new JwtProperties();
            properties.setSecret("security-test-secret-that-is-at-least-thirty-two-bytes");
            properties.setExpirationSeconds(600);
            return properties;
        }

        @Bean
        JwtTokenUtil jwtTokenUtil(JwtProperties properties) {
            return new JwtTokenUtil(properties);
        }
    }
}
