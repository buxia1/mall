package com.macro.mall.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 的序列化配置。
 *
 * <p>这里刻意不加 {@code @ConditionalOnBean(RedisConnectionFactory.class)}：本类由组件扫描发现，
 * 条件是<b>扫描阶段</b>求值的，此时自动配置的 {@code RedisConnectionFactory} 尚未注册，
 * 条件恒为 false，Bean 永远不会创建。Redis 是本项目的必需组件（登录会话、权限缓存），
 * 缺少时应当启动失败并暴露配置问题，而不是静默降级。
 */
@Configuration
public class BaseRedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
