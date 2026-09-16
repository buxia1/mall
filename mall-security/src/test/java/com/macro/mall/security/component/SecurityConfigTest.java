package com.macro.mall.security.component;

import com.macro.mall.common.service.RedisService;
import com.macro.mall.mbg.mapper.UmsAdminMapper;
import com.macro.mall.mbg.model.UmsResource;
import com.macro.mall.security.service.AdminTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SecurityConfigTest.TestApplication.class)
@AutoConfigureMockMvc
class SecurityConfigTest {
    private static final String SECRET = "security-test-secret-that-is-at-least-thirty-two-bytes";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AdminTokenService tokenService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private UmsAdminMapper adminMapper;
    @Autowired
    private DynamicAuthorizationManager authorizationManager;

    @BeforeEach
    void resetMocksAndCache() {
        reset(redisService, adminMapper);
        authorizationManager.clearResourceCache();
    }

    @Test
    void protectedUrlWithoutATokenReturnsTheCommonUnauthorizedEnvelope() throws Exception {
        mockMvc.perform(get("/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void invalidTokenForProtectedUrlReturnsTheCommonUnauthorizedEnvelope() throws Exception {
        mockMvc.perform(get("/protected").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void authenticatedIdentityWithoutTheRequestedResourceIsForbidden() throws Exception {
        when(redisService.hasKey(anyString())).thenReturn(true);
        when(redisService.get(anyString())).thenReturn(null);
        when(adminMapper.selectResourcesByAdminId(7L)).thenReturn(List.of());

        mockMvc.perform(get("/protected").header("Authorization", bearerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void authenticatedIdentityWithAMatchingResourceMayAccessTheEndpoint() throws Exception {
        when(redisService.hasKey(anyString())).thenReturn(true);
        when(redisService.get(anyString())).thenReturn(null);
        when(adminMapper.selectResourcesByAdminId(7L)).thenReturn(List.of(resource("/protected")));

        mockMvc.perform(get("/protected").header("Authorization", bearerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void onlyExplicitLoginAndRefreshEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/admin/login")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/refreshToken")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void resourceCacheHasAnExplicitEvictionBoundaryForFutureResourceCrud() {
        clearInvocations(redisService);
        authorizationManager.clearResourceCache();

        verify(redisService).remove("mall:security:authorization:resources");
    }

    private String bearerToken() {
        return "Bearer " + tokenService.login(7L, "alice");
    }

    private UmsResource resource(String url) {
        UmsResource resource = new UmsResource();
        resource.setUrl(url);
        return resource;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableWebSecurity
    @Import(SecurityConfig.class)
    static class TestApplication {
        @Bean
        JwtProperties jwtProperties() {
            JwtProperties properties = new JwtProperties();
            properties.setSecret(SECRET);
            properties.setExpirationSeconds(600);
            return properties;
        }

        @Bean
        JwtTokenUtil jwtTokenUtil(JwtProperties properties) {
            return new JwtTokenUtil(properties);
        }

        @Bean
        RedisService redisService() {
            return mock(RedisService.class);
        }

        @Bean
        UmsAdminMapper umsAdminMapper() {
            return mock(UmsAdminMapper.class);
        }

        @Bean
        AdminTokenService adminTokenService(RedisService redisService, JwtTokenUtil jwtTokenUtil,
                                            JwtProperties properties) {
            return new AdminTokenService(redisService, jwtTokenUtil, properties);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil,
                                                        AdminTokenService tokenService) {
            return new JwtAuthenticationFilter(jwtTokenUtil, tokenService);
        }

        @Bean
        DynamicAuthorizationManager dynamicAuthorizationManager(UmsAdminMapper adminMapper,
                                                                RedisService redisService) {
            return new DynamicAuthorizationManager(adminMapper, redisService);
        }

        @RestController
        static class TestController {
            @GetMapping({"/protected", "/admin/login", "/admin/refreshToken", "/admin/logout"})
            String endpoint() {
                return "ok";
            }
        }
    }
}
