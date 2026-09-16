package com.macro.mall.mbg.mapper;

import com.macro.mall.mbg.model.UmsAdmin;
import com.macro.mall.mbg.model.UmsMenu;
import com.macro.mall.mbg.model.UmsResource;
import com.macro.mall.mbg.model.UmsRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UmsAdminMapper {
    @Select("SELECT id, username, password, icon, email, nick_name AS nickName, note, create_time AS createTime, login_time AS loginTime, status FROM ums_admin WHERE id = #{id}")
    UmsAdmin selectById(@Param("id") Long id);
    @Select("SELECT id, username, password, icon, email, nick_name AS nickName, note, create_time AS createTime, login_time AS loginTime, status FROM ums_admin WHERE username = #{username}")
    UmsAdmin selectByUsername(@Param("username") String username);

    /** 按用户名或昵称模糊分页查询，排序固定，分页由 PageHelper 拦截。 */
    @Select("<script>SELECT id, username, password, icon, email, nick_name AS nickName, note, create_time AS createTime, login_time AS loginTime, status FROM ums_admin <where><if test=\"keyword != null and keyword != ''\">(username LIKE CONCAT('%', #{keyword}, '%') OR nick_name LIKE CONCAT('%', #{keyword}, '%'))</if></where> ORDER BY id DESC</script>")
    List<UmsAdmin> selectList(@Param("keyword") String keyword);

    @Insert("INSERT INTO ums_admin (username, password, icon, email, nick_name, note, create_time, login_time, status) VALUES (#{username}, #{password}, #{icon}, #{email}, #{nickName}, #{note}, #{createTime}, #{loginTime}, #{status})") @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UmsAdmin admin);
    @Update("UPDATE ums_admin SET username=#{username}, password=#{password}, icon=#{icon}, email=#{email}, nick_name=#{nickName}, note=#{note}, create_time=#{createTime}, login_time=#{loginTime}, status=#{status} WHERE id=#{id}")
    int updateById(UmsAdmin admin);
    @Update("UPDATE ums_admin SET password=#{password} WHERE id=#{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    @Update("UPDATE ums_admin SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    @Delete("DELETE FROM ums_admin WHERE id = #{id}") int deleteById(@Param("id") Long id);

    @Select("SELECT DISTINCT r.id, r.name, r.description, r.admin_count AS adminCount, r.create_time AS createTime, r.status, r.sort FROM ums_role r JOIN ums_admin_role_relation ar ON r.id = ar.role_id WHERE ar.admin_id = #{adminId}")
    List<UmsRole> selectRolesByAdminId(@Param("adminId") Long adminId);
    @Select("SELECT DISTINCT m.id, m.parent_id AS parentId, m.create_time AS createTime, m.title, m.level, m.sort, m.name, m.icon, m.hidden FROM ums_menu m JOIN ums_role_menu_relation rm ON m.id = rm.menu_id JOIN ums_admin_role_relation ar ON rm.role_id = ar.role_id WHERE ar.admin_id = #{adminId}")
    List<UmsMenu> selectMenusByAdminId(@Param("adminId") Long adminId);
    @Select("SELECT DISTINCT r.id, r.create_time AS createTime, r.name, r.url, r.description, r.category_id AS categoryId FROM ums_resource r JOIN ums_role_resource_relation rr ON r.id = rr.resource_id JOIN ums_admin_role_relation ar ON rr.role_id = ar.role_id WHERE ar.admin_id = #{adminId}")
    List<UmsResource> selectResourcesByAdminId(@Param("adminId") Long adminId);
}
