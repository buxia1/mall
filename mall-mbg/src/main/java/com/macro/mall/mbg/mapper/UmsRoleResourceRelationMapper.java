package com.macro.mall.mbg.mapper;

import com.macro.mall.mbg.model.UmsRoleResourceRelation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UmsRoleResourceRelationMapper {
    @Select("SELECT id, role_id AS roleId, resource_id AS resourceId FROM ums_role_resource_relation WHERE id=#{id}")
    UmsRoleResourceRelation selectById(@Param("id") Long id);

    @Insert("INSERT INTO ums_role_resource_relation (role_id, resource_id) VALUES (#{roleId}, #{resourceId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UmsRoleResourceRelation relation);

    @Update("UPDATE ums_role_resource_relation SET role_id=#{roleId}, resource_id=#{resourceId} WHERE id=#{id}")
    int updateById(UmsRoleResourceRelation relation);

    @Delete("DELETE FROM ums_role_resource_relation WHERE id=#{id}")
    int deleteById(@Param("id") Long id);
}
