package com.macro.mall.mbg.mapper;

import com.macro.mall.mbg.model.UmsAdmin;
import com.macro.mall.mbg.model.UmsAdminRoleRelation;
import com.macro.mall.mbg.model.UmsMenu;
import com.macro.mall.mbg.model.UmsResource;
import com.macro.mall.mbg.model.UmsResourceCategory;
import com.macro.mall.mbg.model.UmsRole;
import com.macro.mall.mbg.model.UmsRoleMenuRelation;
import com.macro.mall.mbg.model.UmsRoleResourceRelation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import static org.assertj.core.api.Assertions.assertThat;

class MapperCrudContractTest {

    @Test
    void declaresCrudSqlForEveryAuthTable() throws Exception {
        assertCrud(UmsAdminMapper.class, UmsAdmin.class, "ums_admin", "username", "nick_name");
        assertCrud(UmsRoleMapper.class, UmsRole.class, "ums_role", "admin_count");
        assertCrud(UmsMenuMapper.class, UmsMenu.class, "ums_menu", "parent_id");
        assertCrud(UmsResourceMapper.class, UmsResource.class, "ums_resource", "category_id");
        assertCrud(UmsResourceCategoryMapper.class, UmsResourceCategory.class, "ums_resource_category", "create_time");
        assertCrud(UmsAdminRoleRelationMapper.class, UmsAdminRoleRelation.class, "ums_admin_role_relation", "admin_id", "role_id");
        assertCrud(UmsRoleMenuRelationMapper.class, UmsRoleMenuRelation.class, "ums_role_menu_relation", "role_id", "menu_id");
        assertCrud(UmsRoleResourceRelationMapper.class, UmsRoleResourceRelation.class, "ums_role_resource_relation", "role_id", "resource_id");
    }

    private void assertCrud(Class<?> mapper, Class<?> model, String table, String... criticalColumns) throws Exception {
        assertSql(mapper.getMethod("selectById", Long.class).getAnnotation(Select.class).value(), table, "id");
        assertSql(mapper.getMethod("insert", model).getAnnotation(Insert.class).value(), table, criticalColumns);
        assertSql(mapper.getMethod("updateById", model).getAnnotation(Update.class).value(), table, criticalColumns);
        assertSql(mapper.getMethod("deleteById", Long.class).getAnnotation(Delete.class).value(), table, "id");
    }

    private void assertSql(String[] fragments, String table, String... required) {
        String sql = String.join(" ", fragments).toLowerCase();
        assertThat(sql).contains(table);
        for (String value : required) {
            assertThat(sql).contains(value);
        }
    }
}
