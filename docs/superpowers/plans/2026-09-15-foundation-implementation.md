# Foundation Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Maven root and `mall-common` module with stable response, exception, pagination, Redis, and request-logging foundations.

**Architecture:** The root POM owns versions and aggregates only `mall-common`. `mall-common` exposes framework-independent response/error APIs, and integrates Spring only at the exception, Redis, and logging boundaries. Tests first establish public behavior; a tiny test-only controller verifies the HTTP error envelope.

**Tech Stack:** Java 17, Maven, Spring Boot 3.5, JUnit 5, Spring MVC Test, Spring Data Redis, PageHelper, Spring AOP.

---

## File map

- Create `pom.xml`: root Maven aggregator and dependency versions.
- Create `mall-common/pom.xml`: dependencies and test configuration for the shared module.
- Create `mall-common/src/main/java/com/macro/mall/common/api/*`: error codes, result and page envelopes.
- Create `mall-common/src/main/java/com/macro/mall/common/exception/*`: exceptions, assertion helper and MVC advice.
- Create `mall-common/src/main/java/com/macro/mall/common/service/*` and `config/BaseRedisConfig.java`: Redis abstraction and serializers.
- Create `mall-common/src/main/java/com/macro/mall/common/log/*` and `src/main/resources/logback-spring.xml`: structured request logging.
- Create tests under `mall-common/src/test/java/com/macro/mall/common/**`.

### Task 1: Maven skeleton and public response contract

**Files:**
- Create: `pom.xml`
- Create: `mall-common/pom.xml`
- Create: `mall-common/src/test/java/com/macro/mall/common/api/CommonResultTest.java`
- Create: `mall-common/src/main/java/com/macro/mall/common/api/IErrorCode.java`
- Create: `mall-common/src/main/java/com/macro/mall/common/api/ResultCode.java`
- Create: `mall-common/src/main/java/com/macro/mall/common/api/CommonResult.java`

- [ ] **Step 1: Write failing tests for response factories**

```java
@Test
void successWrapsDataWithSuccessCode() {
    CommonResult<String> result = CommonResult.success("ok");
    assertThat(result.getCode()).isEqualTo(200L);
    assertThat(result.getMessage()).isEqualTo("操作成功");
    assertThat(result.getData()).isEqualTo("ok");
}

@Test
void failedErrorCodeCanOverrideMessage() {
    CommonResult<Void> result = CommonResult.failed(ResultCode.FAILED, "库存不足");
    assertThat(result.getCode()).isEqualTo(500L);
    assertThat(result.getMessage()).isEqualTo("库存不足");
}
```

- [ ] **Step 2: Run the test and verify compilation fails because `CommonResult` does not exist**

Run: `mvn -pl mall-common -Dtest=CommonResultTest test`

Expected: Maven fails during test compilation with a missing `com.macro.mall.common.api.CommonResult` type.

- [ ] **Step 3: Add the smallest root/module POMs and response types needed by the test**

```java
public interface IErrorCode { long getCode(); String getMessage(); }

public enum ResultCode implements IErrorCode {
    SUCCESS(200, "操作成功"), FAILED(500, "操作失败"),
    VALIDATE_FAILED(404, "参数检验失败"), UNAUTHORIZED(401, "暂未登录或token已经过期"),
    FORBIDDEN(403, "没有相关权限");
}
```

Implement `CommonResult<T>` with the frozen constructors, getters/setters, and all specified static factory methods. The factory methods must preserve the passed data and select the matching `ResultCode`.

- [ ] **Step 4: Run the response tests and verify they pass**

Run: `mvn -pl mall-common -Dtest=CommonResultTest test`

Expected: `Tests run: 2, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit the completed response contract**

```powershell
git add pom.xml mall-common/pom.xml mall-common/src/main mall-common/src/test
git commit -m "feat(common): add response contract"
```

### Task 2: Exceptions and HTTP error envelope

**Files:**
- Create: `mall-common/src/test/java/com/macro/mall/common/exception/GlobalExceptionHandlerTest.java`
- Create: `mall-common/src/main/java/com/macro/mall/common/exception/ApiException.java`
- Create: `mall-common/src/main/java/com/macro/mall/common/exception/Asserts.java`
- Create: `mall-common/src/main/java/com/macro/mall/common/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Write failing MVC tests for business, validation, and unexpected errors**

```java
mockMvc.perform(get("/test/business"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(500))
    .andExpect(jsonPath("$.message").value("库存不足"));

mockMvc.perform(get("/test/validation").param("quantity", "0"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(404));

mockMvc.perform(get("/test/unexpected"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(500));
```

- [ ] **Step 2: Run the tests and verify they fail because no exception advice is registered**

Run: `mvn -pl mall-common -Dtest=GlobalExceptionHandlerTest test`

Expected: assertions fail because Spring returns its default error response or no matching advice exists.

- [ ] **Step 3: Implement the smallest exception boundary**

```java
public class ApiException extends RuntimeException {
    private final IErrorCode errorCode;
    public ApiException(IErrorCode errorCode) { super(errorCode.getMessage()); this.errorCode = errorCode; }
    public ApiException(IErrorCode errorCode, String message) { super(message); this.errorCode = errorCode; }
    public IErrorCode getErrorCode() { return errorCode; }
}
```

Implement `Asserts.fail(...)` by throwing `ApiException`, and `@RestControllerAdvice` handlers for `ApiException`, `MethodArgumentNotValidException`, `ConstraintViolationException`, `AccessDeniedException`, and `Exception`. Return `CommonResult` for every handler and extract the first validation message when available.

- [ ] **Step 4: Run the MVC tests and verify the unified envelope**

Run: `mvn -pl mall-common -Dtest=GlobalExceptionHandlerTest test`

Expected: `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit the exception boundary**

```powershell
git add mall-common/src/main mall-common/src/test
git commit -m "feat(common): add unified exception handling"
```

### Task 3: Pagination and Redis boundary

**Files:**
- Create: `mall-common/src/test/java/com/macro/mall/common/api/CommonPageTest.java`
- Create: `mall-common/src/test/java/com/macro/mall/common/config/BaseRedisConfigTest.java`
- Create: `mall-common/src/main/java/com/macro/mall/common/api/CommonPage.java`
- Create: `mall-common/src/main/java/com/macro/mall/common/service/RedisService.java`
- Create: `mall-common/src/main/java/com/macro/mall/common/service/impl/RedisServiceImpl.java`
- Create: `mall-common/src/main/java/com/macro/mall/common/config/BaseRedisConfig.java`

- [ ] **Step 1: Write failing tests for a Spring Data page and Redis serializers**

```java
Page<String> page = new PageImpl<>(List.of("A", "B"), PageRequest.of(1, 2), 5);
CommonPage<String> result = CommonPage.restPage(page);
assertThat(result.getPageNum()).isEqualTo(2);
assertThat(result.getPageSize()).isEqualTo(2);
assertThat(result.getTotalPage()).isEqualTo(3);

assertThat(redisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
```

- [ ] **Step 2: Run the tests and verify the API/configuration types are missing**

Run: `mvn -pl mall-common -Dtest=CommonPageTest,BaseRedisConfigTest test`

Expected: Maven fails test compilation with missing `CommonPage` and `BaseRedisConfig` types.

- [ ] **Step 3: Implement the smallest adapters**

Implement both frozen `CommonPage.restPage` overloads: PageHelper data comes from `PageInfo<T>` and Spring Data data comes from `Page<T>`. Define Redis operations only for get/set/expire/remove/increment/list/set/hash operations actually needed by the next modules; delegate each method directly to `RedisTemplate<String, Object>`. Configure string key/hash-key serializers and generic JSON value/hash-value serializers.

- [ ] **Step 4: Run the pagination/configuration tests and verify they pass**

Run: `mvn -pl mall-common -Dtest=CommonPageTest,BaseRedisConfigTest test`

Expected: `Tests run: 2, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit pagination and Redis boundary**

```powershell
git add mall-common/src/main mall-common/src/test
git commit -m "feat(common): add pagination and redis support"
```

### Task 4: Request logging and full module verification

**Files:**
- Create: `mall-common/src/test/java/com/macro/mall/common/log/WebLogAspectTest.java`
- Create: `mall-common/src/main/java/com/macro/mall/common/log/WebLog.java`
- Create: `mall-common/src/main/java/com/macro/mall/common/log/RequestUtil.java`
- Create: `mall-common/src/main/java/com/macro/mall/common/log/WebLogAspect.java`
- Create: `mall-common/src/main/resources/logback-spring.xml`

- [ ] **Step 1: Write a failing test that invokes an advised controller and exposes request metadata**

```java
mockMvc.perform(get("/test/success").header("User-Agent", "JUnit"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(200));
```

The test context must include the aspect and assert that the controller response remains unchanged when logging is enabled.

- [ ] **Step 2: Run the test and verify it fails because the logging aspect is absent**

Run: `mvn -pl mall-common -Dtest=WebLogAspectTest test`

Expected: test context fails to create the missing `WebLogAspect` bean.

- [ ] **Step 3: Implement minimal non-invasive request logging**

```java
@Around("execution(public * com.macro.mall..controller..*(..))")
public Object around(ProceedingJoinPoint point) throws Throwable {
    long started = System.currentTimeMillis();
    Object result = point.proceed();
    log.info("method={} costMs={}", point.getSignature().toShortString(), System.currentTimeMillis() - started);
    return result;
}
```

Expand the log DTO with URL, HTTP method, IP, request parameters, response value, start/end time, duration, method and description. Skip multipart payloads. Add development and production log profiles without hard-coded external endpoints.

- [ ] **Step 4: Run the complete module test and compile commands**

Run: `mvn -pl mall-common test`

Expected: exit code 0 and no test failures.

Run: `mvn -pl mall-common compile`

Expected: exit code 0 and `BUILD SUCCESS`.

- [ ] **Step 5: Review the implementation against the design and commit**

```powershell
git diff --check
git status --short
git add mall-common/src/main mall-common/src/test mall-common/src/main/resources
git commit -m "feat(common): add request logging"
```

