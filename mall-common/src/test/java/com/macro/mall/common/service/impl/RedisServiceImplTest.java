package com.macro.mall.common.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class RedisServiceImplTest {

    @Test
    void valueOperationsDelegateGetAndExpiringSet() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        when(values.get("key")).thenReturn("value");
        RedisServiceImpl service = new RedisServiceImpl(template);

        service.set("key", "value", 10);

        assertThat(service.get("key")).isEqualTo("value");
        verify(values).set("key", "value", 10, TimeUnit.SECONDS);
    }

    @Test
    void nonPositiveExpiryUsesPersistentSet() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        RedisServiceImpl service = new RedisServiceImpl(template);

        service.set("zero", "value", 0);
        service.set("negative", "value", -1);

        verify(values).set("zero", "value");
        verify(values).set("negative", "value");
    }

    @Test
    void expireRejectsNonPositiveDurationsAndDelegatesPositiveDurations() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        when(template.expire("key", 10, TimeUnit.SECONDS)).thenReturn(true);
        RedisServiceImpl service = new RedisServiceImpl(template);

        assertThat(service.expire("key", 0)).isFalse();
        assertThat(service.expire("key", -1)).isFalse();
        assertThat(service.expire("key", 10)).isTrue();

        verify(template).expire("key", 10, TimeUnit.SECONDS);
    }

    @Test
    void nullKeysDoNotInteractWithTemplate() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        RedisServiceImpl service = new RedisServiceImpl(template);

        assertThat(service.get(null)).isNull();
        service.set(null, "value");
        assertThat(service.expire(null, 10)).isFalse();
        assertThat(service.remove((String) null)).isFalse();

        verify(template, never()).opsForValue();
        verify(template, never()).expire("key", 10, TimeUnit.SECONDS);
        verify(template, never()).delete("key");
    }

    @Test
    void hashOperationsDelegateReadAndWrite() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        when(template.opsForHash()).thenReturn(hashes);
        when(hashes.get("key", "field")).thenReturn("value");
        RedisServiceImpl service = new RedisServiceImpl(template);

        service.hSet("key", "field", "value");

        assertThat(service.hGet("key", "field")).isEqualTo("value");
        verify(hashes).put("key", "field", "value");
    }

    @Test
    void setOperationsDelegateAddAndMembers() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        SetOperations<String, Object> sets = mock(SetOperations.class);
        when(template.opsForSet()).thenReturn(sets);
        when(sets.add("key", "value")).thenReturn(1L);
        when(sets.members("key")).thenReturn(Set.of("value"));
        RedisServiceImpl service = new RedisServiceImpl(template);

        assertThat(service.sAdd("key", "value")).isEqualTo(1L);
        assertThat(service.sMembers("key")).containsExactly("value");
    }

    @Test
    void listOperationsDelegateGetSetAndRemove() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ListOperations<String, Object> lists = mock(ListOperations.class);
        when(template.opsForList()).thenReturn(lists);
        when(lists.range("key", 0, -1)).thenReturn(List.of("value"));
        when(lists.rightPush("key", "value")).thenReturn(1L);
        when(lists.remove("key", 1, "value")).thenReturn(1L);
        RedisServiceImpl service = new RedisServiceImpl(template);

        assertThat(service.lGet("key", 0, -1)).containsExactly("value");
        assertThat(service.lSet("key", "value")).isEqualTo(1L);
        assertThat(service.lRemove("key", 1, "value")).isEqualTo(1L);
    }
}
