package com.macro.mall.admin.service;

import com.macro.mall.admin.dto.AdminInfo;
import com.macro.mall.mbg.mapper.UmsAdminMapper;
import com.macro.mall.mbg.model.UmsAdmin;
import com.macro.mall.mbg.model.UmsMenu;
import com.macro.mall.mbg.model.UmsRole;
import com.macro.mall.security.service.AdminTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UmsAdminServiceTest {
    private final UmsAdminMapper adminMapper = mock(UmsAdminMapper.class);
    private final AdminTokenService tokenService = mock(AdminTokenService.class);
    private final UmsAdminService service = new UmsAdminService(adminMapper, tokenService, new BCryptPasswordEncoder());

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

    private UmsAdmin admin(Long id, String username, String password) {
        UmsAdmin admin = new UmsAdmin();
        admin.setId(id);
        admin.setUsername(username);
        admin.setPassword(new BCryptPasswordEncoder().encode(password));
        return admin;
    }
}
