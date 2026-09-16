package com.macro.mall.mbg.mapper;

import com.macro.mall.mbg.model.UmsRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface UmsRoleMapper {
    @Select("SELECT id, name, description, admin_count AS adminCount, create_time AS createTime, status, sort FROM ums_role WHERE id=#{id}") UmsRole selectById(@Param("id") Long id);

    /** 按角色名称模糊分页查询，分页由 PageHelper 拦截。 */
    @Select("<script>SELECT id, name, description, admin_count AS adminCount, create_time AS createTime, status, sort FROM ums_role <where><if test=\"keyword != null and keyword != ''\">name LIKE CONCAT('%', #{keyword}, '%')</if></where> ORDER BY sort ASC, id ASC</script>")
    List<UmsRole> selectList(@Param("keyword") String keyword);

    @Select("SELECT id, name, description, admin_count AS adminCount, create_time AS createTime, status, sort FROM ums_role ORDER BY sort ASC, id ASC")
    List<UmsRole> selectAll();

    @Insert("INSERT INTO ums_role (name, description, admin_count, create_time, status, sort) VALUES (#{name}, #{description}, #{adminCount}, #{createTime}, #{status}, #{sort})") @Options(useGeneratedKeys = true, keyProperty = "id") int insert(UmsRole role);
    @Update("UPDATE ums_role SET name=#{name}, description=#{description}, admin_count=#{adminCount}, create_time=#{createTime}, status=#{status}, sort=#{sort} WHERE id=#{id}") int updateById(UmsRole role);
    @Update("UPDATE ums_role SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    @Update("UPDATE ums_role SET admin_count=#{adminCount} WHERE id=#{id}")
    int updateAdminCount(@Param("id") Long id, @Param("adminCount") Integer adminCount);
    @Delete("DELETE FROM ums_role WHERE id=#{id}") int deleteById(@Param("id") Long id);
    @Delete("<script>DELETE FROM ums_role WHERE id IN <foreach collection=\"ids\" item=\"id\" open=\"(\" separator=\",\" close=\")\">#{id}</foreach></script>")
    int deleteByIds(@Param("ids") List<Long> ids);
}
