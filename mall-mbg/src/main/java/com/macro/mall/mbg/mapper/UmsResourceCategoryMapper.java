package com.macro.mall.mbg.mapper;

import com.macro.mall.mbg.model.UmsResourceCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface UmsResourceCategoryMapper {
    @Select("SELECT id, create_time AS createTime, name, sort FROM ums_resource_category WHERE id=#{id}") UmsResourceCategory selectById(@Param("id") Long id);

    @Select("SELECT id, create_time AS createTime, name, sort FROM ums_resource_category ORDER BY sort ASC, id ASC")
    List<UmsResourceCategory> selectAll();

    @Insert("INSERT INTO ums_resource_category (create_time, name, sort) VALUES (#{createTime}, #{name}, #{sort})") @Options(useGeneratedKeys = true, keyProperty = "id") int insert(UmsResourceCategory category);
    @Update("UPDATE ums_resource_category SET create_time=#{createTime}, name=#{name}, sort=#{sort} WHERE id=#{id}") int updateById(UmsResourceCategory category);
    @Delete("DELETE FROM ums_resource_category WHERE id=#{id}") int deleteById(@Param("id") Long id);
}
