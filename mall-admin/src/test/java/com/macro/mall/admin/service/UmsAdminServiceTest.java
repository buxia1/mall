package com.macro.mall.admin.service;

import com.macro.mall.admin.dto.AdminInfo;
import com.macro.mall.admin.dto.AdminPasswordParam;
import com.macro.mall.common.exception.ApiException;
import com.macro.mall.mbg.mapper.UmsAdminMapper;
import com.macro.mall.mbg.mapper.UmsAdminRoleRelationMapper;
import com.macro.mall.mbg.mapper.UmsRoleMapper;
import com.macro.mall.mbg.model.UmsAdmin;
import com.macro.mall.mbg.model.UmsMenu;
import com.macro.mall.mbg.model.UmsRole;
import com.macro.mall.security.service.AdminTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UmsAdminServiceTest {
    private final UmsAdminMapper adminMapper = mock(UmsAdminMapper.class);
    private final UmsRoleMapper roleMapper = mock(UmsRoleMapper.class);
    private final UmsAdminRoleRelationMapper adminRoleRelationMapper = mock(UmsAdminRoleRelationMapper.class);
    private final AdminTokenService tokenService = mock(AdminTokenService.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthorizationCacheEvictor cacheEvictor = mock(AuthorizationCacheEvictor.class);
    private final UmsAdminService service = new UmsAdminService(adminMapper, roleMapper, adminRoleRelationMapper,
            tokenService, passwordEncoder, cacheEvictor);

    @Test
    void loginReturnsTokenForStoredBcryptPassword() {
        UmsAdmin admin = admin(7L, "alice", "secret");
        when(adminMapper.selectByUsername("alice")).thenReturn(admin);
        when(tokenService.login(7L, "alice")).thenReturn("signed-token");

        assertThat(service.login("alice", "secret")).isEqualTo("signed-token");
        verify(tokenService).login(7L, "alice");
    }

    @Test
    void loginRejectsMissingOrInvalidCredentials() {
        assertThat(service.login(null, "secret")).isNull();
        assertThat(service.login("alice", "wrong")).isNull();
        verifyNoInteractions(tokenService);
    }

    @Test
    void refreshAndLogoutUseOnlyActiveBearerToken() {
        when(tokenService.refresh("active")).thenReturn("active");

        assertThat(service.refresh("Bearer active")).isEqualTo("active");
        assertThat(service.refresh(null)).isNull();
        service.logout("Bearer active");
        service.logout("invalid");

        verify(tokenService).refresh("active");
        verify(tokenService).logout("active");
        verifyNoMoreInteractions(tokenService);
    }

    @Test
    void infoContainsOnlyExpectedCurrentAdminFields() {
        UmsAdmin admin = admin(7L, "alice", "secret");
        admin.setIcon("avatar.png");
        UmsMenu menu = new UmsMenu();
        menu.setTitle("Dashboard");
        UmsRole role = new UmsRole();
        role.setName("ROLE_ADMIN");
        when(adminMapper.selectById(7L)).thenReturn(admin);
        when(adminMapper.selectMenusByAdminId(7L)).thenReturn(List.of(menu));
        when(adminMapper.selectRolesByAdminId(7L)).thenReturn(List.of(role));

        AdminInfo info = service.info(7L);

        assertThat(info.username()).isEqualTo("alice");
        assertThat(info.icon()).isEqualTo("avatar.png");
        assertThat(info.menus()).containsExactly(menu);
        assertThat(info.roles()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void registerStoresBcryptHashAndNeverReturnsIt() {
        when(adminMapper.selectByUsername("bob")).thenReturn(null);

        UmsAdmin registered = service.register(plainAdmin("bob", "plain-secret"));

        verify(adminMapper).insert(any(UmsAdmin.class));
        assertThat(registered.getPassword()).isNull();
        assertThat(registered.getStatus()).isEqualTo(1);
        assertThat(registered.getCreateTime()).isNotNull();
    }

    @Test
    void registerRejectsAnExistingUsername() {
        when(adminMapper.selectByUsername("alice")).thenReturn(admin(7L, "alice", "secret"));

        assertThatThrownBy(() -> service.register(plainAdmin("alice", "x")))
                .isInstanceOf(ApiException.class)
                .hasMessage("用户名已存在");
        verify(adminMapper, never()).insert(any(UmsAdmin.class));
    }

    @Test
    void updateKeepsTheStoredPasswordWhenTheRequestOmitsIt() {
        UmsAdmin existing = admin(7L, "alice", "secret");
        when(adminMapper.selectById(7L)).thenReturn(existing);

        UmsAdmin incoming = plainAdmin("alice", null);
        incoming.setEmail("alice@example.com");
        service.update(7L, incoming);

        verify(adminMapper).updateById(argThat(updated ->
                updated.getEmail().equals("alice@example.com")
                        && updated.getPassword().equals(existing.getPassword())
                        && updated.getCreateTime().equals(existing.getCreateTime())));
    }

    @Test
    void updateReencodesANewPassword() {
        UmsAdmin existing = admin(7L, "alice", "secret");
        when(adminMapper.selectById(7L)).thenReturn(existing);

        service.update(7L, plainAdmin("alice", "brand-new"));

        verify(adminMapper).updateById(argThat(updated ->
                passwordEncoder.matches("brand-new", updated.getPassword())));
    }

    @Test
    void updatePasswordChecksTheOldPassword() {
        UmsAdmin existing = admin(7L, "alice", "secret");
        when(adminMapper.selectByUsername("alice")).thenReturn(existing);

        AdminPasswordParam wrong = passwordParam("alice", "nope", "brand-new");
        assertThatThrownBy(() -> service.updatePassword(wrong))
                .isInstanceOf(ApiException.class)
                .hasMessage("旧密码错误");

        service.updatePassword(passwordParam("alice", "secret", "brand-new"));
        verify(adminMapper).updatePassword(eq(7L), argThat(encoded -> passwordEncoder.matches("brand-new", encoded)));
    }

    @Test
    void deleteRemovesRoleRelationsAndEvictsThePermissionCache() {
        when(adminMapper.deleteById(7L)).thenReturn(1);

        assertThat(service.delete(7L)).isEqualTo(1);

        verify(adminRoleRelationMapper).deleteByAdminId(7L);
        verify(cacheEvictor).evict();
    }

    @Test
    void updateRoleRebuildsRelationsRecountsRolesAndEvictsCache() {
        when(adminMapper.selectById(7L)).thenReturn(admin(7L, "alice", "secret"));
        UmsRole previous = new UmsRole();
        previous.setId(5L);
        when(adminMapper.selectRolesByAdminId(7L)).thenReturn(List.of(previous));
        when(adminRoleRelationMapper.countByRoleId(2L)).thenReturn(3);

        assertThat(service.updateRole(7L, List.of(2L, 3L))).isEqualTo(2);

        verify(adminRoleRelationMapper).deleteByAdminId(7L);
        verify(adminRoleRelationMapper).insertBatch(7L, List.of(2L, 3L));
        // 旧角色 5 与新角色 2、3 都需要按当前关系数重算 admin_count
        verify(roleMapper).updateAdminCount(5L, 0);
        verify(roleMapper).updateAdminCount(2L, 3);
        verify(roleMapper).updateAdminCount(3L, 0);
        verify(cacheEvictor).evict();
    }

    private AdminPasswordParam passwordParam(String username, String oldPassword, String newPassword) {
        AdminPasswordParam param = new AdminPasswordParam();
        param.setUsername(username);
        param.setOldPassword(oldPassword);
        param.setNewPassword(newPassword);
        return param;
    }

    private UmsAdmin plainAdmin(String username, String password) {
        UmsAdmin admin = new UmsAdmin();
        admin.setUsername(username);
        admin.setPassword(password);
        return admin;
    }

    private UmsAdmin admin(Long id, String username, String password) {
        UmsAdmin admin = new UmsAdmin();
        admin.setId(id);
        admin.setUsername(username);
        admin.setPassword(new BCryptPasswordEncoder().encode(password));
        admin.setCreateTime(java.time.LocalDateTime.of(2020, 1, 1, 0, 0));
        return admin;
    }
}
