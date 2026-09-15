package com.macro.mall.common.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RedisService {
    Object get(String key);
    void set(String key, Object value);
    void set(String key, Object value, long expire);
    Boolean expire(String key, long expire);
    Long getExpire(String key);
    Boolean hasKey(String key);
    Boolean remove(String key);
    Long remove(Collection<String> keys);
    Long increment(String key, long delta);
    Long decrement(String key, long delta);
    Object hGet(String key, String item);
    Map<Object, Object> hGetAll(String key);
    void hSet(String key, String item, Object value);
    void hSet(String key, String item, Object value, long expire);
    Long hDelete(String key, Object... items);
    Boolean hHasKey(String key, String item);
    Double hIncrement(String key, String item, double delta);
    Double hDecrement(String key, String item, double delta);
    Long sAdd(String key, Object... values);
    Long sAdd(String key, long expire, Object... values);
    Set<Object> sMembers(String key);
    Boolean sHasKey(String key, Object value);
    Long sRemove(String key, Object... values);
    List<Object> lGet(String key, long start, long end);
    Long lSet(String key, Object value);
    Long lSet(String key, Object value, long expire);
    Long lRemove(String key, long count, Object value);
}
