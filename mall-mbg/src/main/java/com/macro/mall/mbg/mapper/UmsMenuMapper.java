package com.macro.mall.mbg.mapper;

import com.macro.mall.mbg.model.UmsMenu;
import org.apache.ibatis.annotations.*;

public interface UmsMenuMapper {
    @Select("SELECT id, parent_id AS parentId, create_time AS createTime, title, level, sort, name, icon, hidden FROM ums_menu WHERE id=#{id}") UmsMenu selectById(@Param("id") Long id);
    @Insert("INSERT INTO ums_menu (parent_id, create_time, title, level, sort, name, icon, hidden) VALUES (#{parentId}, #{createTime}, #{title}, #{level}, #{sort}, #{name}, #{icon}, #{hidden})") @Options(useGeneratedKeys = true, keyProperty = "id") int insert(UmsMenu menu);
    @Update("UPDATE ums_menu SET parent_id=#{parentId}, create_time=#{createTime}, title=#{title}, level=#{level}, sort=#{sort}, name=#{name}, icon=#{icon}, hidden=#{hidden} WHERE id=#{id}") int updateById(UmsMenu menu);
    @Delete("DELETE FROM ums_menu WHERE id=#{id}") int deleteById(@Param("id") Long id);
}
