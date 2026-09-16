package com.macro.mall.admin.service;

import com.macro.mall.mbg.mapper.UmsAdminRoleRelationMapper;
import com.macro.mall.mbg.mapper.UmsMenuMapper;
import com.macro.mall.mbg.mapper.UmsResourceMapper;
import com.macro.mall.mbg.mapper.UmsRoleMapper;
import com.macro.mall.mbg.mapper.UmsRoleMenuRelationMapper;
import com.macro.mall.mbg.mapper.UmsRoleResourceRelationMapper;
import com.macro.mall.mbg.model.UmsMenu;
import com.macro.mall.mbg.model.UmsResource;
import com.macro.mall.mbg.model.UmsRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UmsRoleServiceTest {
    private final UmsRoleMapper roleMapper = mock(UmsRoleMapper.class);
    private final UmsMenuMapper menuMapper = mock(UmsMenuMapper.class);
    private final UmsResourceMapper resourceMapper = mock(UmsResourceMapper.class);
    private final UmsRoleMenuRelationMapper roleMenuRelationMapper = mock(UmsRoleMenuRelationMapper.class);
    private final UmsRoleResourceRelationMapper roleResourceRelationMapper = mock(UmsRoleResourceRelationMapper.class);
    private final UmsAdminRoleRelationMapper adminRoleRelationMapper = mock(UmsAdminRoleRelationMapper.class);
    private final AuthorizationCacheEvictor cacheEvictor = mock(AuthorizationCacheEvictor.class);
    private final UmsRoleService service = new UmsRoleService(roleMapper, menuMapper, resourceMapper,
            roleMenuRelationMapper, roleResourceRelationMapper, adminRoleRelationMapper, cacheEvictor);

    @Test
    void createDefaultsStatusCountAndSort() {
        UmsRole role = new UmsRole();
        role.setName("运营");

        service.create(role);

        assertThat(role.getStatus()).isEqualTo(1);
        assertThat(role.getAdminCount()).isZero();
        assertThat(role.getSort()).isZero();
        assertThat(role.getCreateTime()).isNotNull();
        verify(roleMapper).insert(role);
    }

    @Test
    void createRejectsABlankName() {
        UmsRole role = new UmsRole();
        role.setName("  ");

        assertThatThrownBy(() -> service.create(role)).hasMessage("角色名称不能为空");
    }

    @Test
    void updateKeepsSystemMaintainedFields() {
        UmsRole existing = new UmsRole();
        existing.setId(5L);
        existing.setAdminCount(9);
        existing.setCreateTime(java.time.LocalDateTime.of(2020, 1, 1, 0, 0));
        when(roleMapper.selectById(5L)).thenReturn(existing);

        UmsRole incoming = new UmsRole();
        incoming.setName("新名字");
        service.update(5L, incoming);

        assertThat(incoming.getAdminCount()).isEqualTo(9);
        assertThat(incoming.getCreateTime()).isEqualTo(existing.getCreateTime());
    }

    @Test
    void deleteCascadesRelationsAndEvictsThePermissionCache() {
        assertThat(service.delete(List.of(2L, 3L))).isEqualTo(2);

        verify(roleMapper).deleteById(2L);
        verify(roleMapper).deleteById(3L);
        verify(roleMenuRelationMapper).deleteByRoleId(2L);
        verify(roleResourceRelationMapper).deleteByRoleId(2L);
        verify(adminRoleRelationMapper).deleteByRoleId(2L);
        verify(cacheEvictor).evict();
    }

    @Test
    void allocResourceReplacesTheGrantSetAndEvictsThePermissionCache() {
        when(roleMapper.selectById(5L)).thenReturn(new UmsRole());

        assertThat(service.allocResource(5L, List.of(25L, 31L))).isEqualTo(2);

        verify(roleResourceRelationMapper).deleteByRoleId(5L);
        verify(roleResourceRelationMapper).insertBatch(5L, List.of(25L, 31L));
        verify(cacheEvictor).evict();
    }

    @Test
    void allocResourceWithAnEmptySelectionStillRevokesAndEvicts() {
        when(roleMapper.selectById(5L)).thenReturn(new UmsRole());

        assertThat(service.allocResource(5L, List.of())).isZero();

        verify(roleResourceRelationMapper).deleteByRoleId(5L);
        verify(roleResourceRelationMapper, never()).insertBatch(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyList());
        verify(cacheEvictor).evict();
    }

    @Test
    void allocMenuReplacesTheMenuSetWithoutTouchingThePermissionCache() {
        when(roleMapper.selectById(5L)).thenReturn(new UmsRole());

        assertThat(service.allocMenu(5L, List.of(1L, 2L))).isEqualTo(2);

        verify(roleMenuRelationMapper).deleteByRoleId(5L);
        verify(roleMenuRelationMapper).insertBatch(5L, List.of(1L, 2L));
        // 菜单只影响侧边栏，不参与 URL 鉴权，因此不需要清权限缓存
        verify(cacheEvictor, never()).evict();
    }

    @Test
    void allocationRejectsAnUnknownRole() {
        when(roleMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.allocResource(404L, List.of(1L))).hasMessage("角色不存在");
    }

    @Test
    void listResourceReadsThroughTheRoleResourceJoin() {
        UmsResource resource = new UmsResource();
        resource.setUrl("/admin/**");
        when(resourceMapper.selectByRoleId(5L)).thenReturn(List.of(resource));

        assertThat(service.listResource(5L)).containsExactly(resource);
    }

    @Test
    void listMenuReadsThroughTheRoleMenuJoin() {
        UmsMenu menu = new UmsMenu();
        menu.setTitle("商品");
        when(menuMapper.selectByRoleId(5L)).thenReturn(List.of(menu));

        assertThat(service.listMenu(5L)).containsExactly(menu);
    }
}
