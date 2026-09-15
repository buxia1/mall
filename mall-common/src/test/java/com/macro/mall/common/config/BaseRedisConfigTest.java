package com.macro.mall.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.macro.mall.common.service.RedisService;
import com.macro.mall.common.service.impl.RedisServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BaseRedisConfigTest {

    @Test
    void redisInfrastructureIsAbsentWithoutAConnectionFactory() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(BaseRedisConfig.class, RedisServiceImpl.class);
            context.refresh();

            assertThat(context.getBeansOfType(RedisTemplate.class)).isEmpty();
            assertThat(context.getBeansOfType(RedisService.class)).isEmpty();
        }
    }

    @Test
    void redisTemplateUsesStringKeysAndJsonValues() {
        RedisTemplate<String, Object> template = new BaseRedisConfig().redisTemplate(mock(RedisConnectionFactory.class));

        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
        assertThat(template.getHashValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
    }
}
