# 全站维护模式过滤机制解耦与 Service 冗余清理施工文档

**文档编号**：ARCH-20260921-AUTH-MAINTENANCE-REFACTOR  
**日期**：2026-09-21  
**作者**：Antigravity Agent  
**模块范围**：`volur-framework`、`volur-auth`  

---

## 1. 变更背景与诉求

1. **Service 冗余逻辑清理**：
   - 经链路审计，在全站维护模式开启时，`MaintenanceModeFilter`（位于 Spring Security 过滤器链中 `JwtAuthFilter` 之后）会率先拦截所有未在豁免白名单中的请求。
   - 由于注册接口 `/api/v1/auth/register` 未在豁免白名单中，且发起注册请求的用户尚未登录（非管理员），因此所有注册请求在过滤器层已被阻断。
   - `AuthServiceImpl.register()` 中原有的维护模式判断属于不可达的死代码（Dead Code），且与 `system.auth.registration_enabled` 业务开关混淆，需安全移除。
2. **过滤器异常响应统一与解耦**：
   - 此前 `MaintenanceModeFilter` 中直接依赖 `tools.jackson.databind.ObjectMapper` 手动设置 HttpServletResponse 状态码、Header 并序列化写入 JSON。
   - 该做法与项目中已有的 `JwtAuthFilter` 异常桥接模式不一致，导致过滤器与底层响应输出强耦合，无法复用全局异常处理中心（`GlobalExceptionHandler`）的国际化、统一响应组装及日志告警机制。
   - 需参考 `JwtAuthFilter` 模式，注入 `HandlerExceptionResolver`，抛出专用的领域异常（`MaintenanceModeException`），交由 `GlobalExceptionHandler` 统一响应。

---

## 2. 核心架构设计与改动点

### 2.1 新增全站维护模式异常 (`volur-framework`)
- **文件**：[`MaintenanceModeException.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/MaintenanceModeException.java)
- **改动点**：
  - 继承 `BusinessException`，默认绑定 `HttpStatus.SERVICE_UNAVAILABLE`（503）。
  - 提供单参数（通知文案）及带 cause 的构造函数。

### 2.2 全局异常处理器增强 (`volur-framework`)
- **文件**：[`GlobalExceptionHandler.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandler.java)
- **改动点**：
  - 新增 `@ExceptionHandler(MaintenanceModeException.class)`，标注 `@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)`，返回 `ApiResponseEntity.error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage())` 并记录告警日志；
  - 优化通用 `handleBusinessException`：支持注入 `HttpServletResponse` 动态对齐 HTTP 状态码，避免未注解状态码退化为 200。

### 2.3 全站维护过滤器重构 (`volur-auth`)
- **文件**：[`MaintenanceModeFilter.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/MaintenanceModeFilter.java)
- **改动点**：
  - 移除 `ObjectMapper`、`StandardCharsets`、`MediaType` 等手动响应写入依赖；
  - 注入 `@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver`；
  - 拦截普通用户与匿名访问时，统一读取 `SystemSettingKeys.MAINTENANCE_NOTICE_MESSAGE` 并构造 `MaintenanceModeException`，调用 `resolver.resolveException(request, response, null, ex)` 触发全局异常链；
  - 配置键名统一使用 `SystemSettingKeys.MAINTENANCE_ENABLED`。

### 2.4 鉴权业务服务层清理与规范化 (`volur-auth`)
- **文件**：[`AuthServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)
- **改动点**：
  - **`register`**：安全移除冗余的 `system.maintenance.enabled` 判断，保留纯业务注册开关 `SystemSettingKeys.AUTH_REGISTRATION_ENABLED`；
  - **`login`**：保留必要的管理员后验逻辑，同时将异常抛出由泛型 `BusinessException` 规范化为统一的 `MaintenanceModeException`，键名使用 `SystemSettingKeys` 常量。

---

## 3. 验证结果与影响分析

1. **单元测试覆盖与执行验证**：
   - 新增 [`MaintenanceModeFilterTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/security/filter/MaintenanceModeFilterTest.java)：覆盖非维护放行、白名单（login）放行、注册接口被拦截并交由 resolver 处理、管理员携带 `ROLE_ADMIN` 放行等 4 种典型场景；
   - 更新 [`GlobalExceptionHandlerTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandlerTest.java)：验证 `MaintenanceModeException` 捕获后正确输出 HTTP 503 与响应 JSON；
   - 运行 Maven 测试命令 `mvn test -Dtest=MaintenanceModeFilterTest,SecurityWhitelistMatcherTest,AuthVerificationCrossValidationTest -Dsurefire.failIfNoSpecifiedTests=false -pl volur-auth -am` 全部通过（11 tests passed, 0 failures）。
2. **前后端接口协议兼容性**：
   - 过滤器返回的数据结构为标准 `ApiResponse<Void>`（status=503, success=false, message=notice），与前端 `src/utils/request.ts` 响应拦截器判断 `status === 503` 并重定向至 `/maintenance` 完全契合，前后端契约零破坏。
