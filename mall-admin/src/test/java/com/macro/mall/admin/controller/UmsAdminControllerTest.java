package com.macro.mall.admin.controller;

import com.macro.mall.admin.dto.AdminInfo;
import com.macro.mall.admin.service.UmsAdminService;
import com.macro.mall.mbg.model.UmsMenu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UmsAdminControllerTest {
    private final UmsAdminService service = mock(UmsAdminService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UmsAdminController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginReturnsTokenAndTokenHead() throws Exception {
        when(service.login("alice", "secret")).thenReturn("signed-token");

        mockMvc.perform(post("/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("signed-token"))
                .andExpect(jsonPath("$.data.tokenHead").value("Bearer "));
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        when(service.login("alice", "wrong")).thenReturn(null);

        mockMvc.perform(post("/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void refreshAndLogoutReadAuthorizationHeader() throws Exception {
        when(service.refresh("Bearer active")).thenReturn("active");

        mockMvc.perform(get("/admin/refreshToken").header("Authorization", "Bearer active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("active"))
                .andExpect(jsonPath("$.data.tokenHead").value("Bearer "));
        mockMvc.perform(post("/admin/logout").header("Authorization", "Bearer active"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(service).logout("Bearer active");
    }

    @Test
    void refreshRejectsMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/admin/refreshToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void infoReturnsExactlyTheCurrentAdminFields() throws Exception {
        UmsMenu menu = new UmsMenu();
        menu.setTitle("Dashboard");
        when(service.info(7L)).thenReturn(new AdminInfo("alice", List.of(menu), "avatar.png", List.of("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new com.macro.mall.security.component.AdminIdentity(7L, "alice"), null));

        mockMvc.perform(get("/admin/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.icon").value("avatar.png"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.data.menus[0].title").value("Dashboard"))
                .andExpect(jsonPath("$.data.*").isArray())
                .andExpect(jsonPath("$.data.*").value(org.hamcrest.Matchers.hasSize(4)));
    }
}
