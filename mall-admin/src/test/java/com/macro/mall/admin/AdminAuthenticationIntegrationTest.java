package com.macro.mall.admin;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模块 2 验收：跑在真实 MySQL + Redis 上的登录 / 令牌 / 动态权限链路。
 *
 * <p>默认被 surefire 排除（{@code @Tag("integration")}），因为需要本机服务就绪。
 * 本地起好 MySQL 3307 与 Redis 6379 后执行：
 * <pre>
 * mvn -pl mall-admin -am test -Dexcluded.groups=none -Dtest=AdminAuthenticationIntegrationTest
 * </pre>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminAuthenticationIntegrationTest {

    private static final String SEED_USERNAME = "admin";
    private static final String SEED_PASSWORD = "macro123";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void loginIssuesATokenInTheUnifiedEnvelope() {
        JsonNode body = post("/admin/login", loginBody(SEED_USERNAME, SEED_PASSWORD), null).getBody();

        assertThat(body).isNotNull();
        assertThat(body.path("code").asLong()).isEqualTo(200L);
        assertThat(body.path("data").path("token").asText()).isNotBlank();
        assertThat(body.path("data").path("tokenHead").asText()).isEqualTo("Bearer ");
    }

    @Test
    void loginRejectsAWrongPasswordWithoutIssuingAToken() {
        JsonNode body = post("/admin/login", loginBody(SEED_USERNAME, "definitely-wrong"), null).getBody();

        assertThat(body).isNotNull();
        assertThat(body.path("code").asLong()).isEqualTo(500L);
        assertThat(body.path("data").isNull()).isTrue();
    }

    @Test
    void currentUserInfoCarriesUsernameMenusIconAndRoles() {
        JsonNode body = get("/admin/info", login()).getBody();

        assertThat(body).isNotNull();
        assertThat(body.path("code").asLong()).isEqualTo(200L);
        JsonNode data = body.path("data");
        assertThat(data.path("username").asText()).isEqualTo(SEED_USERNAME);
        assertThat(data.path("icon").asText()).isNotBlank();
        assertThat(data.path("roles").size()).isPositive();
        assertThat(data.path("menus").size()).isPositive();
        assertThat(data.path("menus").get(0).path("title").asText()).isNotBlank();
    }

    @Test
    void protectedEndpointWithoutATokenReturnsTheCommonUnauthorizedEnvelope() {
        ResponseEntity<JsonNode> response = get("/admin/info", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().path("code").asLong()).isEqualTo(401L);
    }

    @Test
    void protectedEndpointWithAMalformedTokenReturns401() {
        ResponseEntity<JsonNode> response = get("/admin/info", "not-a-real-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().path("code").asLong()).isEqualTo(401L);
    }

    @Test
    void logoutRevokesTheTokenImmediately() {
        String token = login();
        assertThat(get("/admin/info", token).getBody().path("code").asLong()).isEqualTo(200L);

        assertThat(post("/admin/logout", null, token).getBody().path("code").asLong()).isEqualTo(200L);

        ResponseEntity<JsonNode> afterLogout = get("/admin/info", token);
        assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(afterLogout.getBody().path("code").asLong()).isEqualTo(401L);
    }

    @Test
    void refreshTokenKeepsAnActiveSessionUsable() {
        String token = login();

        JsonNode refreshed = get("/admin/refreshToken", token).getBody();
        assertThat(refreshed.path("code").asLong()).isEqualTo(200L);
        assertThat(refreshed.path("data").path("token").asText()).isEqualTo(token);

        assertThat(get("/admin/info", token).getBody().path("code").asLong()).isEqualTo(200L);
    }

    @Test
    void authenticatedAdminWithoutAMatchingResourceIsForbidden() {
        ResponseEntity<JsonNode> response = get("/not-a-managed-resource", login());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().path("code").asLong()).isEqualTo(403L);
    }

    private String login() {
        return post("/admin/login", loginBody(SEED_USERNAME, SEED_PASSWORD), null)
                .getBody().path("data").path("token").asText();
    }

    private String loginBody(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private ResponseEntity<JsonNode> post(String path, String body, String token) {
        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private ResponseEntity<JsonNode> get(String path, String token) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers(token)), JsonNode.class);
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return headers;
    }
}
