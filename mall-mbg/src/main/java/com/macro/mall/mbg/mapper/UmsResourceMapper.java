package com.macro.mall.mbg.mapper;

import com.macro.mall.mbg.model.UmsResource;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface UmsResourceMapper {
    @Select("SELECT id, create_time AS createTime, name, url, description, category_id AS categoryId FROM ums_resource WHERE id=#{id}") UmsResource selectById(@Param("id") Long id);

    /** 按分类 / 名称关键字 / URL 关键字模糊分页查询，三个条件都可省略。分页由 PageHelper 拦截。 */
    @Select("<script>SELECT id, create_time AS createTime, name, url, description, category_id AS categoryId FROM ums_resource <where><if test=\"categoryId != null\">category_id = #{categoryId}</if><if test=\"nameKeyword != null and nameKeyword != ''\">AND name LIKE CONCAT('%', #{nameKeyword}, '%')</if><if test=\"urlKeyword != null and urlKeyword != ''\">AND url LIKE CONCAT('%', #{urlKeyword}, '%')</if></where> ORDER BY id DESC</script>")
    List<UmsResource> selectList(@Param("categoryId") Long categoryId,
                                 @Param("nameKeyword") String nameKeyword,
                                 @Param("urlKeyword") String urlKeyword);

    @Select("SELECT id, create_time AS createTime, name, url, description, category_id AS categoryId FROM ums_resource ORDER BY id DESC")
    List<UmsResource> selectAll();

    @Select("SELECT DISTINCT r.id, r.create_time AS createTime, r.name, r.url, r.description, r.category_id AS categoryId FROM ums_resource r JOIN ums_role_resource_relation rr ON r.id = rr.resource_id WHERE rr.role_id = #{roleId} ORDER BY r.id ASC")
    List<UmsResource> selectByRoleId(@Param("roleId") Long roleId);

    @Insert("INSERT INTO ums_resource (create_time, name, url, description, category_id) VALUES (#{createTime}, #{name}, #{url}, #{description}, #{categoryId})") @Options(useGeneratedKeys = true, keyProperty = "id") int insert(UmsResource resource);
    @Update("UPDATE ums_resource SET create_time=#{createTime}, name=#{name}, url=#{url}, description=#{description}, category_id=#{categoryId} WHERE id=#{id}") int updateById(UmsResource resource);
    @Delete("DELETE FROM ums_resource WHERE id=#{id}") int deleteById(@Param("id") Long id);
}
