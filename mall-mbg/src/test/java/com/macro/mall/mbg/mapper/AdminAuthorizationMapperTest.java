package com.macro.mall.mbg.mapper;

import com.macro.mall.mbg.model.UmsMenu;
import com.macro.mall.mbg.model.UmsResource;
import com.macro.mall.mbg.model.UmsRole;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuthorizationMapperTest {

    @Test
    void exposesRoleMenuAndResourceQueriesForAnAdministrator() throws Exception {
        assertQuery("selectRolesByAdminId", UmsRole.class, "ums_admin_role_relation", "ums_role");
        assertQuery("selectMenusByAdminId", UmsMenu.class, "ums_admin_role_relation", "ums_role_menu_relation", "ums_menu");
        assertQuery("selectResourcesByAdminId", UmsResource.class, "ums_admin_role_relation", "ums_role_resource_relation", "ums_resource");
    }

    private void assertQuery(String name, Class<?> itemType, String... tables) throws Exception {
        Method method = UmsAdminMapper.class.getMethod(name, Long.class);
        assertThat(method.getReturnType()).isEqualTo(List.class);
        assertThat(method.getGenericReturnType().getTypeName()).contains(itemType.getSimpleName());
        Select select = method.getAnnotation(Select.class);
        assertThat(select).isNotNull();
        String sql = String.join(" ", select.value()).toLowerCase();
        for (String table : tables) {
            assertThat(sql).contains(table);
        }
        assertThat(sql).contains("#{adminid}").contains("distinct");
    }
}
