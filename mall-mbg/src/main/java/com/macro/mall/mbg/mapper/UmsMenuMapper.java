package com.macro.mall.mbg.mapper;

import com.macro.mall.mbg.model.UmsMenu;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface UmsMenuMapper {
    @Select("SELECT id, parent_id AS parentId, create_time AS createTime, title, level, sort, name, icon, hidden FROM ums_menu WHERE id=#{id}") UmsMenu selectById(@Param("id") Long id);

    /** 按父级菜单分页查询，分页由 PageHelper 拦截。 */
    @Select("SELECT id, parent_id AS parentId, create_time AS createTime, title, level, sort, name, icon, hidden FROM ums_menu WHERE parent_id=#{parentId} ORDER BY sort ASC, id ASC")
    List<UmsMenu> selectByParentId(@Param("parentId") Long parentId);

    /** 全量菜单，treeList 用它一次取回再在内存里组树。 */
    @Select("SELECT id, parent_id AS parentId, create_time AS createTime, title, level, sort, name, icon, hidden FROM ums_menu ORDER BY sort ASC, id ASC")
    List<UmsMenu> selectAll();

    @Select("SELECT DISTINCT m.id, m.parent_id AS parentId, m.create_time AS createTime, m.title, m.level, m.sort, m.name, m.icon, m.hidden FROM ums_menu m JOIN ums_role_menu_relation rm ON m.id = rm.menu_id WHERE rm.role_id = #{roleId} ORDER BY m.sort ASC, m.id ASC")
    List<UmsMenu> selectByRoleId(@Param("roleId") Long roleId);

    @Insert("INSERT INTO ums_menu (parent_id, create_time, title, level, sort, name, icon, hidden) VALUES (#{parentId}, #{createTime}, #{title}, #{level}, #{sort}, #{name}, #{icon}, #{hidden})") @Options(useGeneratedKeys = true, keyProperty = "id") int insert(UmsMenu menu);
    @Update("UPDATE ums_menu SET parent_id=#{parentId}, create_time=#{createTime}, title=#{title}, level=#{level}, sort=#{sort}, name=#{name}, icon=#{icon}, hidden=#{hidden} WHERE id=#{id}") int updateById(UmsMenu menu);
    @Update("UPDATE ums_menu SET hidden=#{hidden} WHERE id=#{id}")
    int updateHidden(@Param("id") Long id, @Param("hidden") Integer hidden);
    @Delete("DELETE FROM ums_menu WHERE id=#{id}") int deleteById(@Param("id") Long id);
}
