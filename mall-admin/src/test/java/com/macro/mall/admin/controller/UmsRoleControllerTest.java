package com.macro.mall.admin.controller;

import com.macro.mall.admin.service.UmsRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 前端把多选 ID 拼成逗号字符串放在 query 参数里（mall-admin-web 的 join(',')），
 * 一个都没选时是空串。这里锁定「空串不能变成 400」这条契约。
 */
class UmsRoleControllerTest {
    private final UmsRoleService roleService = mock(UmsRoleService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UmsRoleController(roleService)).build();
    }

    @Test
    void deleteParsesACommaSeparatedIdList() throws Exception {
        when(roleService.delete(List.of(2L, 3L))).thenReturn(2);

        mockMvc.perform(post("/role/delete").param("ids", "2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(2));

        verify(roleService).delete(List.of(2L, 3L));
    }

    @Test
    void deleteAcceptsAnEmptySelection() throws Exception {
        when(roleService.delete(List.of())).thenReturn(0);

        mockMvc.perform(post("/role/delete").param("ids", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(roleService).delete(List.of());
    }

    @Test
    void allocResourceAcceptsAnEmptySelection() throws Exception {
        when(roleService.allocResource(eq(5L), anyList())).thenReturn(0);

        mockMvc.perform(post("/role/allocResource").param("roleId", "5").param("resourceIds", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(roleService).allocResource(5L, List.of());
    }

    @Test
    void allocMenuParsesACommaSeparatedIdList() throws Exception {
        when(roleService.allocMenu(5L, List.of(1L, 2L))).thenReturn(2);

        mockMvc.perform(post("/role/allocMenu").param("roleId", "5").param("menuIds", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        verify(roleService).allocMenu(5L, List.of(1L, 2L));
    }
}
