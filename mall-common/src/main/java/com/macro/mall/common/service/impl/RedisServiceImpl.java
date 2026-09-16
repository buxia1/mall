package com.macro.mall.common.service.impl;

import com.macro.mall.common.service.RedisService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Redis 是本项目的必需组件，所以不做条件装配，见 {@code BaseRedisConfig} 的说明。 */
@Service
public class RedisServiceImpl implements RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Object get(String key) { return key == null ? null : redisTemplate.opsForValue().get(key); }
    public void set(String key, Object value) { if (key != null) redisTemplate.opsForValue().set(key, value); }
    public void set(String key, Object value, long expire) {
        if (key == null) return;
        if (expire > 0) redisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS);
        else redisTemplate.opsForValue().set(key, value);
    }
    public Boolean expire(String key, long expire) { return key != null && expire > 0 ? redisTemplate.expire(key, expire, TimeUnit.SECONDS) : false; }
    public Long getExpire(String key) { return key == null ? -1L : redisTemplate.getExpire(key, TimeUnit.SECONDS); }
    public Boolean hasKey(String key) { return key != null && Boolean.TRUE.equals(redisTemplate.hasKey(key)); }
    public Boolean remove(String key) { return key != null && Boolean.TRUE.equals(redisTemplate.delete(key)); }
    public Long remove(Collection<String> keys) { return keys == null || keys.isEmpty() ? 0L : redisTemplate.delete(keys); }
    public Long increment(String key, long delta) { return key == null ? 0L : redisTemplate.opsForValue().increment(key, delta); }
    public Long decrement(String key, long delta) { return key == null ? 0L : redisTemplate.opsForValue().increment(key, -delta); }
    public Object hGet(String key, String item) { return key == null || item == null ? null : redisTemplate.opsForHash().get(key, item); }
    public Map<Object, Object> hGetAll(String key) { return key == null ? Collections.emptyMap() : redisTemplate.opsForHash().entries(key); }
    public void hSet(String key, String item, Object value) { if (key != null && item != null) redisTemplate.opsForHash().put(key, item, value); }
    public void hSet(String key, String item, Object value, long expire) { hSet(key, item, value); if (key != null && expire > 0) expire(key, expire); }
    public Long hDelete(String key, Object... items) { return key == null || items == null || items.length == 0 ? 0L : redisTemplate.opsForHash().delete(key, items); }
    public Boolean hHasKey(String key, String item) { return key != null && item != null && Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(key, item)); }
    public Double hIncrement(String key, String item, double delta) { return key == null || item == null ? 0D : redisTemplate.opsForHash().increment(key, item, delta); }
    public Double hDecrement(String key, String item, double delta) { return hIncrement(key, item, -delta); }
    public Long sAdd(String key, Object... values) { return key == null || values == null || values.length == 0 ? 0L : redisTemplate.opsForSet().add(key, values); }
    public Long sAdd(String key, long expire, Object... values) { Long added = sAdd(key, values); if (key != null && expire > 0) expire(key, expire); return added; }
    public Set<Object> sMembers(String key) { return key == null ? Collections.emptySet() : redisTemplate.opsForSet().members(key); }
    public Boolean sHasKey(String key, Object value) { return key != null && Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value)); }
    public Long sRemove(String key, Object... values) { return key == null || values == null || values.length == 0 ? 0L : redisTemplate.opsForSet().remove(key, values); }
    public List<Object> lGet(String key, long start, long end) { return key == null ? Collections.emptyList() : redisTemplate.opsForList().range(key, start, end); }
    public Long lSet(String key, Object value) { return key == null ? 0L : redisTemplate.opsForList().rightPush(key, value); }
    public Long lSet(String key, Object value, long expire) { if (key == null) return 0L; Long size = redisTemplate.opsForList().rightPush(key, value); if (expire > 0) expire(key, expire); return size; }
    public Long lRemove(String key, long count, Object value) { return key == null ? 0L : redisTemplate.opsForList().remove(key, count, value); }
}
