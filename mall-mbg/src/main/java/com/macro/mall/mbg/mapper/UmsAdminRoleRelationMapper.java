package com.macro.mall.mbg.mapper;

import com.macro.mall.mbg.model.UmsAdminRoleRelation;
import org.apache.ibatis.annotations.*;

public interface UmsAdminRoleRelationMapper {
    @Select("SELECT id, admin_id AS adminId, role_id AS roleId FROM ums_admin_role_relation WHERE id=#{id}") UmsAdminRoleRelation selectById(@Param("id") Long id);
    @Insert("INSERT INTO ums_admin_role_relation (admin_id, role_id) VALUES (#{adminId}, #{roleId})") @Options(useGeneratedKeys = true, keyProperty = "id") int insert(UmsAdminRoleRelation relation);
    @Update("UPDATE ums_admin_role_relation SET admin_id=#{adminId}, role_id=#{roleId} WHERE id=#{id}") int updateById(UmsAdminRoleRelation relation);
    @Delete("DELETE FROM ums_admin_role_relation WHERE id=#{id}") int deleteById(@Param("id") Long id);
}
