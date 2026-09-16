package com.macro.mall.mbg.mapper;

import com.macro.mall.mbg.model.UmsRole;
import org.apache.ibatis.annotations.*;

public interface UmsRoleMapper {
    @Select("SELECT id, name, description, admin_count AS adminCount, create_time AS createTime, status, sort FROM ums_role WHERE id=#{id}") UmsRole selectById(@Param("id") Long id);
    @Insert("INSERT INTO ums_role (name, description, admin_count, create_time, status, sort) VALUES (#{name}, #{description}, #{adminCount}, #{createTime}, #{status}, #{sort})") @Options(useGeneratedKeys = true, keyProperty = "id") int insert(UmsRole role);
    @Update("UPDATE ums_role SET name=#{name}, description=#{description}, admin_count=#{adminCount}, create_time=#{createTime}, status=#{status}, sort=#{sort} WHERE id=#{id}") int updateById(UmsRole role);
    @Delete("DELETE FROM ums_role WHERE id=#{id}") int deleteById(@Param("id") Long id);
}
