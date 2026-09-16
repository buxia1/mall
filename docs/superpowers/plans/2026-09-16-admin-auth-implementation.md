# Admin Authentication and RBAC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add backend administrator JWT authentication, Redis-backed sessions, and dynamic URL RBAC while preserving the established CommonResult contract.

**Architecture:** Add generated-table models/Mapper interfaces in `mall-mbg`, reusable JWT/Spring Security components in `mall-security`, and HTTP/business layers in `mall-admin`. Authorization resolves a request URL to database resources and invalidates its Redis cache when resource data changes.

**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis, MySQL 8, Redis, Spring Security 6, JJWT, JUnit 5, MockMvc.

---

### Task 1: Add module boundaries and authentication persistence contract

**Files:**
- Modify: `pom.xml`
- Create: `mall-mbg/pom.xml`, `mall-security/pom.xml`, `mall-admin/pom.xml`
- Create: MyBatis models and Mappers for `ums_admin`, `ums_role`, `ums_menu`, `ums_resource`, `ums_resource_category`, `ums_admin_role_relation`, `ums_role_menu_relation`
- Test: mapper integration tests using a test schema or Testcontainers-compatible profile.

- [ ] Write failing mapper tests for loading an administrator's roles, menus, and resources.
- [ ] Run `mvn -pl mall-mbg -Dtest=AdminAuthorizationMapperTest test`; confirm missing mapper/model failure.
- [ ] Add only the models, mapper interfaces/XML, and dependency wiring required by the tests.
- [ ] Re-run the focused test; expect zero failures.
- [ ] Commit: `feat(mbg): add admin authorization persistence`.

### Task 2: JWT and Redis login-session service

**Files:**
- Create: `mall-security/src/main/java/com/macro/mall/security/component/JwtTokenUtil.java`
- Create: `mall-security/src/main/java/com/macro/mall/security/service/AdminTokenService.java`
- Create: `mall-security/src/test/java/.../JwtTokenUtilTest.java`
- Create: `mall-security/src/test/java/.../AdminTokenServiceTest.java`

- [ ] Write failing tests: valid token exposes username; malformed/expired token is rejected; logout makes a token unusable; refresh extends the Redis session only for an active token.
- [ ] Run `mvn -pl mall-security -Dtest=JwtTokenUtilTest,AdminTokenServiceTest test`; confirm missing types.
- [ ] Implement signed JWT identity claims and Redis-backed active-session validation with explicit TTLs.
- [ ] Re-run focused tests; expect zero failures.
- [ ] Commit: `feat(security): add jwt redis session service`.

### Task 3: Spring Security authentication and dynamic authorization

**Files:**
- Create: `mall-security/src/main/java/.../JwtAuthenticationFilter.java`
- Create: `mall-security/src/main/java/.../DynamicAuthorizationManager.java`
- Create: `mall-security/src/main/java/.../SecurityConfig.java`
- Test: MockMvc tests for 401, 403, permitted URL, and allowed dynamic resource.

- [ ] Write failing HTTP tests: absent token returns CommonResult code 401; authenticated user without matching resource returns 403; matching resource is allowed.
- [ ] Run `mvn -pl mall-security -Dtest=SecurityConfigTest test`; confirm failures before configuration exists.
- [ ] Implement filter-chain wiring using `JsonAccessDeniedHandler`, a JSON 401 entry point, and an AuthorizationManager that resolves URL resources via cache.
- [ ] Re-run tests; expect zero failures.
- [ ] Commit: `feat(security): add dynamic rbac filter chain`.

### Task 4: Administrator login and current-user API

**Files:**
- Create: `mall-admin/src/main/java/.../controller/UmsAdminController.java`
- Create: `mall-admin/src/main/java/.../service/UmsAdminService.java`
- Create: login and admin-info DTOs
- Test: `UmsAdminControllerTest.java`

- [ ] Write failing tests for `/admin/login`, `/admin/refreshToken`, `/admin/logout`, and `/admin/info`.
- [ ] Verify red with `mvn -pl mall-admin -Dtest=UmsAdminControllerTest test`.
- [ ] Implement BCrypt password validation, token responses, and exactly `username`, `menus`, `icon`, `roles` in info response.
- [ ] Verify green; commit `feat(admin): add login and current user api`.

### Task 5: Admin, role, menu, resource, and category CRUD

**Files:**
- Create/modify: matching Controller, Service, Mapper query, DTO, and test files in `mall-admin`.

- [ ] Before each endpoint group, document its table(s), routes, validation, authorization, relation effects, and cache effects in the PR description.
- [ ] Write endpoint tests before production code for administrator/role/menu/resource/category CRUD and relation allocation.
- [ ] Require resource create/update/delete to evict dynamic-resource cache; test the next authorization lookup observes changed permissions without restart.
- [ ] Run `mvn -pl mall-admin test`; expect zero failures.
- [ ] Commit focused groups, e.g. `feat(admin): add role and resource management`.

### Task 6: End-to-end verification

- [ ] Run `mvn -DskipTests=false -Ddocker.skip=true test`.
- [ ] Start `mall-admin` against local MySQL/Redis and verify login, `/admin/info`, logout, 401, 403, role allocation, and resource-cache invalidation.
- [ ] Run `git diff --check` and ensure no secrets/config credentials are tracked.
- [ ] Commit verification-only changes if needed; otherwise do not create a no-op commit.

