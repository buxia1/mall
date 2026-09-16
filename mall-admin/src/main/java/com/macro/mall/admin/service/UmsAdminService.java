package com.macro.mall.admin.service;

import com.github.pagehelper.PageHelper;
import com.macro.mall.admin.dto.AdminInfo;
import com.macro.mall.admin.dto.AdminPasswordParam;
import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.mbg.mapper.UmsAdminMapper;
import com.macro.mall.mbg.mapper.UmsAdminRoleRelationMapper;
import com.macro.mall.mbg.mapper.UmsRoleMapper;
import com.macro.mall.mbg.model.UmsAdmin;
import com.macro.mall.mbg.model.UmsMenu;
import com.macro.mall.mbg.model.UmsRole;
import com.macro.mall.security.service.AdminTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UmsAdminService {
    private static final String BEARER_PREFIX = "Bearer ";

    private final UmsAdminMapper adminMapper;
    private final UmsRoleMapper roleMapper;
    private final UmsAdminRoleRelationMapper adminRoleRelationMapper;
    private final AdminTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationCacheEvictor cacheEvictor;

    public UmsAdminService(UmsAdminMapper adminMapper,
                           UmsRoleMapper roleMapper,
                           UmsAdminRoleRelationMapper adminRoleRelationMapper,
                           AdminTokenService tokenService,
                           PasswordEncoder passwordEncoder,
                           AuthorizationCacheEvictor cacheEvictor) {
        this.adminMapper = adminMapper;
        this.roleMapper = roleMapper;
        this.adminRoleRelationMapper = adminRoleRelationMapper;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.cacheEvictor = cacheEvictor;
    }

    public String login(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            return null;
        }
        UmsAdmin admin = adminMapper.selectByUsername(username);
        if (admin == null || admin.getPassword() == null || !passwordEncoder.matches(password, admin.getPassword())) {
            return null;
        }
        return tokenService.login(admin.getId(), admin.getUsername());
    }

    public String refresh(String authorization) {
        String token = token(authorization);
        return token == null ? null : tokenService.refresh(token);
    }

    public void logout(String authorization) {
        String token = token(authorization);
        if (token != null) {
            tokenService.logout(token);
        }
    }

    public AdminInfo info(Long adminId) {
        UmsAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            return null;
        }
        List<UmsMenu> menus = adminMapper.selectMenusByAdminId(adminId);
        List<String> roles = adminMapper.selectRolesByAdminId(adminId).stream()
                .map(role -> role.getName())
                .toList();
        return new AdminInfo(admin.getUsername(), menus, admin.getIcon(), roles);
    }

    public UmsAdmin register(UmsAdmin admin) {
        if (admin.getUsername() == null || admin.getUsername().isBlank()) {
            Asserts.fail("用户名不能为空");
        }
        if (admin.getPassword() == null || admin.getPassword().isBlank()) {
            Asserts.fail("密码不能为空");
        }
        if (adminMapper.selectByUsername(admin.getUsername()) != null) {
            Asserts.fail("用户名已存在");
        }
        admin.setId(null);
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setCreateTime(LocalDateTime.now());
        admin.setLoginTime(null);
        if (admin.getStatus() == null) {
            admin.setStatus(1);
        }
        adminMapper.insert(admin);
        return hidePassword(admin);
    }

    public CommonPage<UmsAdmin> list(String keyword, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        CommonPage<UmsAdmin> page = CommonPage.restPage(adminMapper.selectList(keyword));
        page.getList().forEach(this::hidePassword);
        return page;
    }

    public UmsAdmin getItem(Long id) {
        return hidePassword(adminMapper.selectById(id));
    }

    public int update(Long id, UmsAdmin admin) {
        UmsAdmin existing = adminMapper.selectById(id);
        if (existing == null) {
            Asserts.fail("用户不存在");
        }
        if (admin.getUsername() != null && !admin.getUsername().isBlank()
                && !admin.getUsername().equals(existing.getUsername())) {
            UmsAdmin sameName = adminMapper.selectByUsername(admin.getUsername());
            if (sameName != null && !sameName.getId().equals(id)) {
                Asserts.fail("用户名已存在");
            }
        }
        admin.setId(id);
        // 更新语句全字段覆盖：createTime/loginTime 由系统维护，不能改。
        admin.setCreateTime(existing.getCreateTime());
        admin.setLoginTime(existing.getLoginTime());
        if (admin.getUsername() == null || admin.getUsername().isBlank()) {
            admin.setUsername(existing.getUsername());
        }
        // 请求里没带密码就保持原值，带了就重新加密。
        admin.setPassword(admin.getPassword() == null || admin.getPassword().isBlank()
                ? existing.getPassword()
                : passwordEncoder.encode(admin.getPassword()));
        return adminMapper.updateById(admin);
    }

    public int updatePassword(AdminPasswordParam param) {
        if (isBlank(param.getUsername()) || isBlank(param.getOldPassword()) || isBlank(param.getNewPassword())) {
            Asserts.fail("用户名、旧密码和新密码都不能为空");
        }
        UmsAdmin admin = adminMapper.selectByUsername(param.getUsername());
        if (admin == null) {
            Asserts.fail("用户不存在");
        }
        if (admin.getPassword() == null || !passwordEncoder.matches(param.getOldPassword(), admin.getPassword())) {
            Asserts.fail("旧密码错误");
        }
        return adminMapper.updatePassword(admin.getId(), passwordEncoder.encode(param.getNewPassword()));
    }

    @Transactional
    public int delete(Long id) {
        adminRoleRelationMapper.deleteByAdminId(id);
        int count = adminMapper.deleteById(id);
        // 已发出去的 token 在 Redis 里仍然有效（会话不查库），清掉权限缓存至少让它立刻无权可用。
        cacheEvictor.evict();
        return count;
    }

    public int updateStatus(Long id, Integer status) {
        return adminMapper.updateStatus(id, status);
    }

    public List<UmsRole> getRoleList(Long adminId) {
        return adminMapper.selectRolesByAdminId(adminId);
    }

    /**
     * 重新分配管理员的角色。关系变了，该管理员的权限也就变了，必须清权限缓存；
     * 同时受影响角色的 admin_count 要重算（它是冗余统计字段）。
     */
    @Transactional
    public int updateRole(Long adminId, List<Long> roleIds) {
        if (adminMapper.selectById(adminId) == null) {
            Asserts.fail("用户不存在");
        }
        Set<Long> affectedRoleIds = new LinkedHashSet<>();
        adminMapper.selectRolesByAdminId(adminId).forEach(role -> affectedRoleIds.add(role.getId()));

        adminRoleRelationMapper.deleteByAdminId(adminId);
        int count = 0;
        if (roleIds != null && !roleIds.isEmpty()) {
            adminRoleRelationMapper.insertBatch(adminId, roleIds);
            affectedRoleIds.addAll(roleIds);
            count = roleIds.size();
        }
        affectedRoleIds.forEach(roleId -> roleMapper.updateAdminCount(roleId, adminRoleRelationMapper.countByRoleId(roleId)));
        cacheEvictor.evict();
        return count;
    }

    /** 响应里不回传密码哈希；请求方向不受影响（反序列化照旧）。 */
    private UmsAdmin hidePassword(UmsAdmin admin) {
        if (admin != null) {
            admin.setPassword(null);
        }
        return admin;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String token(String authorization) {
        return authorization != null && authorization.startsWith(BEARER_PREFIX)
                && authorization.length() > BEARER_PREFIX.length()
                ? authorization.substring(BEARER_PREFIX.length()) : null;
    }
}
