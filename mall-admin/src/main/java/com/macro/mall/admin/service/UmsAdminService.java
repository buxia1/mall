package com.macro.mall.admin.service;

import com.macro.mall.admin.dto.AdminInfo;
import com.macro.mall.mbg.mapper.UmsAdminMapper;
import com.macro.mall.mbg.model.UmsAdmin;
import com.macro.mall.mbg.model.UmsMenu;
import com.macro.mall.security.service.AdminTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UmsAdminService {
    private static final String BEARER_PREFIX = "Bearer ";

    private final UmsAdminMapper adminMapper;
    private final AdminTokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public UmsAdminService(UmsAdminMapper adminMapper, AdminTokenService tokenService,
                           PasswordEncoder passwordEncoder) {
        this.adminMapper = adminMapper;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
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

    private String token(String authorization) {
        return authorization != null && authorization.startsWith(BEARER_PREFIX)
                && authorization.length() > BEARER_PREFIX.length()
                ? authorization.substring(BEARER_PREFIX.length()) : null;
    }
}
