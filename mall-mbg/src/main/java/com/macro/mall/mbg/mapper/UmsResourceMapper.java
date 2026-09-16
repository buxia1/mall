package com.macro.mall.mbg.mapper;

import com.macro.mall.mbg.model.UmsResource;
import org.apache.ibatis.annotations.*;

public interface UmsResourceMapper {
    @Select("SELECT id, create_time AS createTime, name, url, description, category_id AS categoryId FROM ums_resource WHERE id=#{id}") UmsResource selectById(@Param("id") Long id);
    @Insert("INSERT INTO ums_resource (create_time, name, url, description, category_id) VALUES (#{createTime}, #{name}, #{url}, #{description}, #{categoryId})") @Options(useGeneratedKeys = true, keyProperty = "id") int insert(UmsResource resource);
    @Update("UPDATE ums_resource SET create_time=#{createTime}, name=#{name}, url=#{url}, description=#{description}, category_id=#{categoryId} WHERE id=#{id}") int updateById(UmsResource resource);
    @Delete("DELETE FROM ums_resource WHERE id=#{id}") int deleteById(@Param("id") Long id);
}
