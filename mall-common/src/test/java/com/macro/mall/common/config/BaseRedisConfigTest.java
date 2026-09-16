package com.macro.mall.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.macro.mall.common.service.RedisService;
import com.macro.mall.common.service.impl.RedisServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BaseRedisConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BaseRedisConfig.class, RedisServiceImpl.class);

    @Test
    void redisTemplateAndServiceAreWiredWhenAConnectionFactoryIsPresent() {
        contextRunner.withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisTemplate.class);
                    assertThat(context).hasSingleBean(RedisService.class);
                });
    }

    /**
     * Redis 是必需组件：缺少连接工厂时应当启动失败暴露配置问题，而不是悄悄不注册 Bean。
     */
    @Test
    void redisInfrastructureFailsFastWithoutAConnectionFactory() {
        contextRunner.run(context -> assertThat(context).hasFailed());
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
