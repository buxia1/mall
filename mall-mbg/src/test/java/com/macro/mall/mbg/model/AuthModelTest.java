package com.macro.mall.mbg.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuthModelTest {

    @Test
    void mapsRepresentativeAuthTableColumns() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 16, 10, 0);
        UmsAdmin admin = new UmsAdmin();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setCreateTime(now);
        admin.setStatus(1);
        UmsRole role = new UmsRole();
        role.setName("operator");
        role.setAdminCount(2);
        UmsMenu menu = new UmsMenu();
        menu.setParentId(0L);
        menu.setHidden(0);
        UmsResource resource = new UmsResource();
        resource.setCategoryId(3L);
        resource.setUrl("/admin/**");
        UmsResourceCategory category = new UmsResourceCategory();
        category.setCreateTime(now);
        category.setSort(4);
        UmsAdminRoleRelation adminRole = new UmsAdminRoleRelation();
        adminRole.setAdminId(1L);
        adminRole.setRoleId(2L);
        UmsRoleMenuRelation roleMenu = new UmsRoleMenuRelation();
        roleMenu.setRoleId(2L);
        roleMenu.setMenuId(3L);
        UmsRoleResourceRelation roleResource = new UmsRoleResourceRelation();
        roleResource.setRoleId(2L);
        roleResource.setResourceId(4L);

        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getCreateTime()).isEqualTo(now);
        assertThat(role.getAdminCount()).isEqualTo(2);
        assertThat(menu.getParentId()).isZero();
        assertThat(resource.getUrl()).isEqualTo("/admin/**");
        assertThat(category.getSort()).isEqualTo(4);
        assertThat(adminRole.getRoleId()).isEqualTo(2L);
        assertThat(roleMenu.getMenuId()).isEqualTo(3L);
        assertThat(roleResource.getResourceId()).isEqualTo(4L);
    }
}
