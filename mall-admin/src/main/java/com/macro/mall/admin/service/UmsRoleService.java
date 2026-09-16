package com.macro.mall.admin.service;

import com.github.pagehelper.PageHelper;
import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.mbg.mapper.UmsAdminRoleRelationMapper;
import com.macro.mall.mbg.mapper.UmsMenuMapper;
import com.macro.mall.mbg.mapper.UmsResourceMapper;
import com.macro.mall.mbg.mapper.UmsRoleMapper;
import com.macro.mall.mbg.mapper.UmsRoleMenuRelationMapper;
import com.macro.mall.mbg.mapper.UmsRoleResourceRelationMapper;
import com.macro.mall.mbg.model.UmsMenu;
import com.macro.mall.mbg.model.UmsResource;
import com.macro.mall.mbg.model.UmsRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UmsRoleService {
    private final UmsRoleMapper roleMapper;
    private final UmsMenuMapper menuMapper;
    private final UmsResourceMapper resourceMapper;
    private final UmsRoleMenuRelationMapper roleMenuRelationMapper;
    private final UmsRoleResourceRelationMapper roleResourceRelationMapper;
    private final UmsAdminRoleRelationMapper adminRoleRelationMapper;
    private final AuthorizationCacheEvictor cacheEvictor;

    public UmsRoleService(UmsRoleMapper roleMapper,
                          UmsMenuMapper menuMapper,
                          UmsResourceMapper resourceMapper,
                          UmsRoleMenuRelationMapper roleMenuRelationMapper,
                          UmsRoleResourceRelationMapper roleResourceRelationMapper,
                          UmsAdminRoleRelationMapper adminRoleRelationMapper,
                          AuthorizationCacheEvictor cacheEvictor) {
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.resourceMapper = resourceMapper;
        this.roleMenuRelationMapper = roleMenuRelationMapper;
        this.roleResourceRelationMapper = roleResourceRelationMapper;
        this.adminRoleRelationMapper = adminRoleRelationMapper;
        this.cacheEvictor = cacheEvictor;
    }

    public int create(UmsRole role) {
        if (role.getName() == null || role.getName().isBlank()) {
            Asserts.fail("角色名称不能为空");
        }
        role.setId(null);
        role.setCreateTime(LocalDateTime.now());
        role.setAdminCount(0);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        if (role.getSort() == null) {
            role.setSort(0);
        }
        return roleMapper.insert(role);
    }

    public int update(Long id, UmsRole role) {
        UmsRole existing = roleMapper.selectById(id);
        if (existing == null) {
            Asserts.fail("角色不存在");
        }
        role.setId(id);
        // 更新语句全字段覆盖：createTime 与 adminCount 由系统维护，不能被请求体冲掉。
        role.setCreateTime(existing.getCreateTime());
        role.setAdminCount(existing.getAdminCount());
        return roleMapper.updateById(role);
    }

    /**
     * 删除角色及其关联的菜单/资源/管理员关系。角色没了，关系行留着就是脏数据，
     * 同时持有该角色的管理员权限也变了，所以要清权限缓存。
     */
    @Transactional
    public int delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        for (Long id : ids) {
            roleMapper.deleteById(id);
            roleMenuRelationMapper.deleteByRoleId(id);
            roleResourceRelationMapper.deleteByRoleId(id);
            adminRoleRelationMapper.deleteByRoleId(id);
        }
        cacheEvictor.evict();
        return ids.size();
    }

    public List<UmsRole> listAll() {
        return roleMapper.selectAll();
    }

    public CommonPage<UmsRole> list(String keyword, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return CommonPage.restPage(roleMapper.selectList(keyword));
    }

    public int updateStatus(Long id, Integer status) {
        return roleMapper.updateStatus(id, status);
    }

    public List<UmsMenu> listMenu(Long roleId) {
        return menuMapper.selectByRoleId(roleId);
    }

    public List<UmsResource> listResource(Long roleId) {
        return resourceMapper.selectByRoleId(roleId);
    }

    @Transactional
    public int allocMenu(Long roleId, List<Long> menuIds) {
        requireRole(roleId);
        roleMenuRelationMapper.deleteByRoleId(roleId);
        if (menuIds == null || menuIds.isEmpty()) {
            return 0;
        }
        roleMenuRelationMapper.insertBatch(roleId, menuIds);
        return menuIds.size();
    }

    @Transactional
    public int allocResource(Long roleId, List<Long> resourceIds) {
        requireRole(roleId);
        roleResourceRelationMapper.deleteByRoleId(roleId);
        if (resourceIds == null || resourceIds.isEmpty()) {
            cacheEvictor.evict();
            return 0;
        }
        roleResourceRelationMapper.insertBatch(roleId, resourceIds);
        cacheEvictor.evict();
        return resourceIds.size();
    }

    private void requireRole(Long roleId) {
        if (roleId == null || roleMapper.selectById(roleId) == null) {
            Asserts.fail("角色不存在");
        }
    }
}
