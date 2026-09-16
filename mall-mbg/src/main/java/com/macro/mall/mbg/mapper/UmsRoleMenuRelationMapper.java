package com.macro.mall.mbg.mapper;

import com.macro.mall.mbg.model.UmsRoleMenuRelation;
import org.apache.ibatis.annotations.*;

public interface UmsRoleMenuRelationMapper {
    @Select("SELECT id, role_id AS roleId, menu_id AS menuId FROM ums_role_menu_relation WHERE id=#{id}") UmsRoleMenuRelation selectById(@Param("id") Long id);
    @Insert("INSERT INTO ums_role_menu_relation (role_id, menu_id) VALUES (#{roleId}, #{menuId})") @Options(useGeneratedKeys = true, keyProperty = "id") int insert(UmsRoleMenuRelation relation);
    @Update("UPDATE ums_role_menu_relation SET role_id=#{roleId}, menu_id=#{menuId} WHERE id=#{id}") int updateById(UmsRoleMenuRelation relation);
    @Delete("DELETE FROM ums_role_menu_relation WHERE id=#{id}") int deleteById(@Param("id") Long id);
}
