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
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void resetMocksAndCache() {
        reset(redisService, adminMapper);
        authorizationManager.clearResourceCache();
    }

    @Test
    void securityConfigurationStartsWithProductionComponents() {
        assertThat(authorizationManager).isNotNull();
        assertThat(jwtAuthenticationFilter).isNotNull();
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

        mockMvc.perform(get("/context/protected").contextPath("/context")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void serializedResourceCacheIsReusedWithoutAnotherMapperLookup() throws Exception {
        AtomicReference<Object> storedCache = new AtomicReference<>();
        ObjectMapper objectMapper = new ObjectMapper();
        when(redisService.hasKey(anyString())).thenReturn(true);
        when(redisService.get(anyString())).thenAnswer(invocation -> storedCache.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            storedCache.set(objectMapper.readValue(objectMapper.writeValueAsString(invocation.getArgument(1)),
                    new TypeReference<Map<String, Object>>() { }));
            return null;
        }).when(redisService).set(anyString(), org.mockito.ArgumentMatchers.any());
        when(adminMapper.selectResourcesByAdminId(7L)).thenReturn(List.of(resource("/protected")));

        mockMvc.perform(get("/protected").header("Authorization", bearerToken())).andExpect(status().isOk());
        mockMvc.perform(get("/protected").header("Authorization", bearerToken())).andExpect(status().isOk());

        verify(adminMapper).selectResourcesByAdminId(7L);
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

    /**
     * 前端跑在 Vite dev server（另一个端口）上，跨域预检必须先于鉴权被应答，
     * 否则连登录请求都发不出去 —— 这正是前端登录失败的原因。
     */
    @Test
    void corsPreflightIsAnsweredBeforeAuthorization() throws Exception {
        mockMvc.perform(options("/admin/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsRejectsAnOriginOutsideTheAllowedPatterns() throws Exception {
        mockMvc.perform(get("/admin/login").header("Origin", "https://evil.example.com"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    private String bearerToken() {
        return "Bearer " + tokenService.login(7L, "alice");
    }

    private UmsResource resource(String url) {
        UmsResource resource = new UmsResource();
        resource.setUrl(url);
        return resource;
    }

    /**
     * 这个测试把 {@code UmsAdminMapper} 和 {@code RedisService} 都 mock 掉了，不需要真实持久层。
     * {@code mall-mbg} 引入 mybatis starter 后 {@code spring-boot-starter-jdbc} 会一起进类路径，
     * 不排除数据源自动配置就会因为缺少 JDBC URL 而启动失败。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
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
        CorsProperties corsProperties() {
            return new CorsProperties();
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

        @RestController
        static class TestController {
            @GetMapping({"/protected", "/admin/login", "/admin/refreshToken", "/admin/logout"})
            String endpoint() {
                return "ok";
            }
        }
    }
}
