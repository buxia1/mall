package com.macro.mall.admin.service;

import com.macro.mall.common.exception.ApiException;
import com.macro.mall.mbg.mapper.UmsResourceMapper;
import com.macro.mall.mbg.model.UmsResource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 资源的增删改必须让权限缓存失效，否则新授权要等缓存过期才生效。
 * 这条是接口契约的一部分（见 02-接口规格 §1.2），所以每个写操作都要断言。
 */
class UmsResourceServiceTest {
    private final UmsResourceMapper resourceMapper = mock(UmsResourceMapper.class);
    private final AuthorizationCacheEvictor cacheEvictor = mock(AuthorizationCacheEvictor.class);
    private final UmsResourceService service = new UmsResourceService(resourceMapper, cacheEvictor);

    @Test
    void createEvictsThePermissionCache() {
        service.create(resource("后台用户管理", "/admin/**"));

        verify(resourceMapper).insert(org.mockito.ArgumentMatchers.any(UmsResource.class));
        verify(cacheEvictor).evict();
    }

    @Test
    void updateEvictsThePermissionCacheAndKeepsCreateTime() {
        UmsResource existing = resource("旧名字", "/admin/**");
        existing.setId(25L);
        existing.setCreateTime(java.time.LocalDateTime.of(2020, 1, 1, 0, 0));
        when(resourceMapper.selectById(25L)).thenReturn(existing);

        UmsResource incoming = resource("新名字", "/admin/other/**");
        service.update(25L, incoming);

        assertThat(incoming.getCreateTime()).isEqualTo(existing.getCreateTime());
        verify(cacheEvictor).evict();
    }

    @Test
    void updateRejectsAnUnknownResource() {
        when(resourceMapper.selectById(404L)).thenReturn(null);

        UmsResource incoming = resource("x", "/x/**");
        assertThatThrownBy(() -> service.update(404L, incoming)).isInstanceOf(ApiException.class);
    }

    @Test
    void deleteEvictsThePermissionCache() {
        service.delete(25L);

        verify(resourceMapper).deleteById(25L);
        verify(cacheEvictor).evict();
    }

    @Test
    void createRejectsAMissingUrl() {
        UmsResource incoming = resource("没有URL", "  ");

        assertThatThrownBy(() -> service.create(incoming)).hasMessage("资源URL不能为空");
    }

    private UmsResource resource(String name, String url) {
        UmsResource resource = new UmsResource();
        resource.setName(name);
        resource.setUrl(url);
        return resource;
    }
}
