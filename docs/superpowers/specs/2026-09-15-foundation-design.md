# 基础层设计

## 目标

建立 Java 17 / Spring Boot 3.5 的 Maven 聚合工程和 `mall-common` 公共模块，为后续业务模块提供稳定的响应、异常、分页、Redis 与请求日志能力。

## 范围

根工程只聚合 `mall-common`。`mall-common` 提供以下冻结 API：

- `api`：`IErrorCode`、`ResultCode`、`CommonResult<T>`、`CommonPage<T>`。
- `exception`：`ApiException`、`Asserts`、`GlobalExceptionHandler`。
- `service` / `config`：`RedisService`、`RedisServiceImpl`、`BaseRedisConfig`。
- `log`：`WebLog`、`WebLogAspect`、`RequestUtil` 与 `logback-spring.xml`。

本阶段不创建数据库实体、Mapper、Controller、业务 CRUD 或其他业务模块。

## 关键决策

- 公共结果和异常使用不可变的公开签名与静态工厂方法，保证后续模块的契约稳定。
- `CommonPage` 同时适配 PageHelper 和 Spring Data 的分页模型，输出前端无关的统一字段。
- Redis 通过服务接口封装，键使用字符串序列化，值使用 JSON 序列化；业务代码不直接依赖 `RedisTemplate`。
- 异常建议映射为统一 JSON 响应，覆盖业务异常、参数校验、认证/授权和兜底异常；对参数校验保留首个字段错误消息。
- 请求日志切面使用 `@Around`，记录请求与响应耗时，并跳过 multipart 上传路径或大体积请求体。

## 验收

1. `mvn -pl mall-common test` 和 `mvn -pl mall-common compile` 成功。
2. 单元测试覆盖结果包装、断言异常、分页转换和 Redis 序列化配置。
3. 集成测试以最小测试控制器验证成功响应、参数校验失败和未捕获异常均返回统一 JSON 结构。
