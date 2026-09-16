package com.macro.mall.admin;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
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
 * 模块 2 CRUD 的真实链路验收：跑在 MySQL + Redis 上。
 *
 * <p>重点验证三件在单元测试里证明不了的事：
 * <ol>
 *   <li>PageHelper 真的拦到了 SQL（分页字段与总数正确）</li>
 *   <li>新建资源 + 给角色授权后，<b>不重启</b>新 URL 立刻可用（权限缓存被清）</li>
 *   <li>关系的增删改在真实外键式关联下保持一致</li>
 * </ol>
 *
 * <p>默认被 surefire 排除（{@code @Tag("integration")}），需要本机 MySQL/Redis：
 * <pre>
 * mvn -pl mall-admin -am test -Dexcluded.groups=none -Dtest=AdminRbacCrudIntegrationTest
 * </pre>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminRbacCrudIntegrationTest {
    private static final long SUPER_ADMIN_ID = 3L;
    private static final long SUPER_ADMIN_ROLE_ID = 5L;
    private static final String PROBE_URL = "/it-rbac-probe/**";
    private static final String PROBE_PATH = "/it-rbac-probe/anything";

    @Autowired
    private TestRestTemplate restTemplate;

    private Long createdRoleId;
    private Long createdResourceId;

    @AfterEach
    void cleanUp() {
        String token = login();
        if (createdRoleId != null) {
            // 先把管理员角色还原成只剩超管，再删角色，避免删除时还挂着关系
            post("/admin/role/update?adminId=" + SUPER_ADMIN_ID + "&roleIds=" + SUPER_ADMIN_ROLE_ID, null, token);
            post("/role/delete?ids=" + createdRoleId, null, token);
        }
        if (createdResourceId != null) {
            post("/resource/delete/" + createdResourceId, null, token);
        }
    }

    @Test
    void pagingIsAppliedByPageHelper() {
        String token = login();

        JsonNode body = get("/admin/list?pageNum=1&pageSize=2", token).getBody();

        assertThat(body.path("code").asLong()).isEqualTo(200L);
        JsonNode page = body.path("data");
        assertThat(page.path("pageNum").asInt()).isEqualTo(1);
        assertThat(page.path("pageSize").asInt()).isEqualTo(2);
        assertThat(page.path("list").size()).isLessThanOrEqualTo(2);
        assertThat(page.path("total").asLong()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void newlyGrantedResourceBecomesUsableWithoutRestart() {
        String token = login();

        assertThat(get(PROBE_PATH, token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        createdRoleId = createRole(token, "IT-RBAC-ROLE");
        createdResourceId = createResource(token, PROBE_URL);

        assertThat(post("/role/allocResource?roleId=" + createdRoleId
                + "&resourceIds=" + createdResourceId, null, token).getBody().path("code").asLong()).isEqualTo(200L);
        assertThat(post("/admin/role/update?adminId=" + SUPER_ADMIN_ID
                + "&roleIds=" + SUPER_ADMIN_ROLE_ID + "," + createdRoleId, null, token)
                .getBody().path("code").asLong()).isEqualTo(200L);

        // 关键断言：没有重启、Service 也没动过，只因为 /resource/create 与 allocResource 清了权限缓存，
        // 新 URL 的授权立刻生效（这里没有对应 Controller，所以放行后是 404 而不是 403）。
        ResponseEntity<JsonNode> afterGrant = get(PROBE_PATH, token);
        assertThat(afterGrant.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void resourceCrudRoundTripsThroughRealSql() {
        String token = login();

        createdResourceId = createResource(token, PROBE_URL);
        JsonNode created = get("/resource/" + createdResourceId, token).getBody();
        assertThat(created.path("data").path("url").asText()).isEqualTo(PROBE_URL);

        assertThat(post("/resource/update/" + createdResourceId,
                "{\"name\":\"IT-RBAC-RES-2\",\"url\":\"/it-rbac-probe/v2/**\",\"categoryId\":1}", token)
                .getBody().path("code").asLong()).isEqualTo(200L);
        JsonNode updated = get("/resource/" + createdResourceId, token).getBody();
        assertThat(updated.path("data").path("url").asText()).isEqualTo("/it-rbac-probe/v2/**");
        // createTime 由系统维护，更新时不能被冲掉
        assertThat(updated.path("data").path("createTime").isNull()).isFalse();

        JsonNode listed = get("/resource/list?urlKeyword=it-rbac-probe&pageNum=1&pageSize=5", token).getBody();
        assertThat(listed.path("data").path("total").asLong()).isEqualTo(1L);
    }

    @Test
    void menuTreeReturnsRootsWithNestedChildren() {
        String token = login();

        JsonNode body = get("/menu/treeList", token).getBody();

        assertThat(body.path("code").asLong()).isEqualTo(200L);
        JsonNode roots = body.path("data");
        assertThat(roots.size()).isPositive();
        assertThat(roots.get(0).path("title").asText()).isNotBlank();
        assertThat(roots.get(0).path("children").isArray()).isTrue();
    }

    @Test
    void roleAllocationIsVisibleThroughTheAdminRoleEndpoint() {
        String token = login();

        createdRoleId = createRole(token, "IT-RBAC-ALLOC-ROLE");
        post("/admin/role/update?adminId=" + SUPER_ADMIN_ID
                + "&roleIds=" + SUPER_ADMIN_ROLE_ID + "," + createdRoleId, null, token);

        JsonNode roles = get("/admin/role/" + SUPER_ADMIN_ID, token).getBody();
        assertThat(roles.path("data").size()).isEqualTo(2);

        JsonNode listed = get("/role/list?keyword=IT-RBAC-ALLOC-ROLE&pageNum=1&pageSize=5", token).getBody();
        assertThat(listed.path("data").path("total").asLong()).isEqualTo(1L);
    }

    private Long createRole(String token, String name) {
        JsonNode body = post("/role/create",
                "{\"name\":\"" + name + "\",\"description\":\"integration test\",\"status\":1,\"sort\":99}", token)
                .getBody();
        assertThat(body.path("code").asLong()).isEqualTo(200L);
        return findId("/role/list?keyword=" + name + "&pageNum=1&pageSize=5", token);
    }

    private Long createResource(String token, String url) {
        JsonNode body = post("/resource/create",
                "{\"name\":\"IT-RBAC-RES\",\"url\":\"" + url + "\",\"categoryId\":1}", token).getBody();
        assertThat(body.path("code").asLong()).isEqualTo(200L);
        return findId("/resource/list?urlKeyword=it-rbac-probe&pageNum=1&pageSize=5", token);
    }

    private Long findId(String listPath, String token) {
        JsonNode list = get(listPath, token).getBody().path("data").path("list");
        assertThat(list.size()).isPositive();
        return list.get(0).path("id").asLong();
    }

    private String login() {
        String token = post("/admin/login", "{\"username\":\"admin\",\"password\":\"macro123\"}", null)
                .getBody().path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
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
        if (token != null && !token.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return headers;
    }
}
