# Volur Lite ApiResponseEntity 响应实体与 CookieContextHolder 上下文架构重构

## 1. 变更背景与架构动因

在重构前，Volur Lite 平台的控制器响应封装机制存在以下设计痛点与协议层脱节问题：

1. **类型声明与工厂方法名不副实**：
   - 控制器方法签名返回 `ApiResponse<T>`，而静态调用工厂却命名为 `ApiResponseEntity.ok(...)`；
   - 原先的 `ApiResponseEntity` 仅为返回 `ApiResponse<T>` 实例的静态工具类（私有化构造函数），并不继承 Spring 原生 `ResponseEntity`，导致控制器层无法直接掌控 HTTP Headers、Set-Cookie 等传输层元数据。
2. **Cookie 操作与控制器流程强耦合或侵入 Servlet 底层**：
   - 服务层在处理登录、注册授权、Cookie 下发时，必须侵入 `HttpServletResponse` 或依赖 `RequestContextHolder` 强耦合底层容器响应；
   - 控制器无法像 Spring 原生 `ResponseEntity + ResponseCookie` 那样以声明式流式 API（Fluent API）操作 Cookie。
3. **领域模型纯净性与防泄露考量**：
   - 针对在 VO 中声明 `@Cookie` 传递的设想，经严苛论证认定其违背 Clean Architecture（VO 职责异化）并存在凭据泄露风险（Fail-Closed 原则破坏）；
   - 确定采用**方案 A（基于 ThreadLocal 的 `CookieContextHolder` 透明透传与自动聚合模式）**，既保持 VO 纯粹与安全，又让 Service 与 Controller 零摩擦交互。

---

## 2. 核心架构设计与重构实现

### 2.1 不变式 Cookie 领域模型 (`ApiResponseCookie`)
在 `volur-framework` 模块中创建不可变领域模型 [`ApiResponseCookie`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/api/ApiResponseCookie.java)：
- 基于 Builder 模式构建，封装 `name`, `value`, `domain`, `path`, `maxAge`, `secure`, `httpOnly`, `sameSite`, `partitioned`；
- 原生支持转换映射为 Spring 标准 `ResponseCookie`（`toResponseCookie()`），默认开启 `httpOnly = true`, `sameSite = "Lax"`, `path = "/"` 等安全基准；
- 提供便捷静态工厂：`ofHttpOnly(...)`, `deletion(...)`, `of(...)`。

### 2.2 线程上下文透明保管机制 (`CookieContextHolder`)
在 `volur-framework` 模块中创建 [`CookieContextHolder`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/api/CookieContextHolder.java)：
- 基于 `ThreadLocal<List<ApiResponseCookie>>` 实现线程内生命周期管理；
- 提供安全暂存：`setCookie(ApiResponseCookie)`, `setCookies(Collection<ApiResponseCookie>)`；
- 提供消费式弹出与清理：`pollCookies()` 在被消费后原子清空当前线程上下文，防止内存泄露与跨请求污染。

### 2.3 实体与工厂大一统 (`ApiResponseEntity<T>`)
在 `volur-framework` 模块中重构 [`ApiResponseEntity<T>`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/api/ApiResponseEntity.java)：
- **继承 Spring 原生 `ResponseEntity<ApiResponse<T>>`**：
  使 `ApiResponseEntity<T>` 直接成为 Spring MVC 的一等公民响应实体，完全受 `HttpEntityMethodProcessor` 托管调度；
- **链式流式 API 支持**：
  提供 `.cookie(ApiResponseCookie)`, `.cookies(Collection<ApiResponseCookie>)`, `.header(String, String...)` 链式调用；
- **自动聚合上下文 Cookie**：
  所有静态工厂方法（`ok`, `success`, `created`, `badRequest`, `unauthorized`, `forbidden`, `notFound`, `conflict`, `tooManyRequests`, `error`, `internalServerError` 等）在构建响应实体时，自动调用 `CookieContextHolder.pollCookies()` 将上下文暂存的 Cookie 写入 `HttpHeaders.SET_COOKIE`；
- **解决 Java 泛型静态方法 Hiding 冲突**：
  规避与父类 `ResponseEntity.<T>ok(T body)` 产生的 Java 泛型静态方法冲突，保持 `ok(String messageKey, T data)` 双参数经典规范与 `okData(T data)`、`success(String messageKey)` 的协同。

### 2.4 全平台控制器与异常处理器返回值统一升级
- 全系统 48 个控制器方法签名统一由 `public ApiResponse<...>` 升级为 `public ApiResponseEntity<...>`；
- 全局异常处理器 [`GlobalExceptionHandler`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandler.java) 返回值类型统一升级为 `ApiResponseEntity<Void>`；
- 切面处理器 [`CompetitionLogAspect`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/aspect/CompetitionLogAspect.java) 与 [`SyslogAspect`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-logging/src/main/java/com/humolix/volur/logging/aspect/SyslogAspect.java) 增加对 `ApiResponseEntity` / `HttpEntity` 的解构与 Body 提取支持，保证落库日志中仅包含纯净的业务报文体。

### 2.5 凭据管理服务对齐 (`AuthCookieService`)
升级 [`AuthCookieService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/cookie/AuthCookieService.java)：
- 统一使用 `ApiResponseCookie` 替代散落的 Builder；
- 写入 Cookie 时同步登记至 `CookieContextHolder`，实现 Controller 返回 `ApiResponseEntity` 时的全自动 Header 组装，并向下兼容底层 `HttpServletResponse` 写入。

---

## 3. 影响范围与验证结果

### 3.1 前端影响面
- **完全零破坏兼容（Zero Breaking Changes）**：
  虽然控制器返回值改为 `ApiResponseEntity<T>`，但经过 Spring MVC `HttpEntityMethodProcessor` 与 Jackson 序列化输出给客户端的报文 Body 依旧是标准的 `ApiResponse<T>` JSON 结构，前端（经典版及 NEXT 版）代码无需变动。

### 3.2 编译与测试验证
- 全平台 13 个子模块执行 `mvn install -DskipTests` 全线 `BUILD SUCCESS`；
- 全平台执行 `mvn test` 单元测试，所有测试用例（包括新增的 Cookie 及上下文集成测试）全部以 `0 Failures, 0 Errors` 顺利通过。
