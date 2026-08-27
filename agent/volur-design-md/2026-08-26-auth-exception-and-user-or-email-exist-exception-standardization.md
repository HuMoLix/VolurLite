# 认证异常体系重构与 UserOrEmailExistException 全局异常捕获治理文档

## 1. 变更背景与问题定位

在用户注册流程中，当提交已存在的用户名或邮箱时，后端 `AuthServiceImpl.register()` 会抛出 `UserOrEmailExistException`。
原有的异常定义中：
- `AuthException` 直接继承了 Java 原生受检异常 `java.lang.Exception`；
- `UserOrEmailExistException`、`TokenAlreadyBlacklistedException`、`TokenInvalidException` 继承了 `AuthException`；
- `volur-framework` 中的全局异常处理器 [`GlobalExceptionHandler`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandler.java) 针对业务异常仅捕获 [`BusinessException`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/BusinessException.java) 派生类。

由于 `AuthException` 及其派生类（如 `UserOrEmailExistException`）未继承 `BusinessException`，导致注册冲突时异常直接穿透至 `GlobalExceptionHandler.handleGlobalException(Exception.class)`，触发以下报错：
```text
ERROR c.h.v.f.c.e.h.GlobalExceptionHandler : 系统未处理异常: 
com.humolix.volur.auth.exception.UserOrEmailExistException: 用户名或邮箱已存在
```
并且向前端返回了 HTTP 500（`服务器内部故障，请联系管理员`），而非规范预期的 HTTP 400 业务错误提示。

---

## 2. 核心改动点

### 2.1 认证模块异常继承体系重构
1. **[`AuthException.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/exception/AuthException.java)**：
   - 将继承基类由 `Exception` 调整为 `BusinessException`；
   - 默认 HTTP 状态码设置为 `HttpStatus.BAD_REQUEST` (400)；
   - 提供状态码、错误码以及 Cause 构造重载。
2. **[`UserOrEmailExistException.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/exception/UserOrEmailExistException.java)**：
   - 继承 `AuthException`，默认错误信息为 `"用户名或邮箱已存在"`，状态码为 400。
3. **[`InvalidCaptchaException.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/exception/InvalidCaptchaException.java)**：
   - 统一继承 `AuthException`。
4. **[`TokenAlreadyBlacklistedException.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/exception/TokenAlreadyBlacklistedException.java)** 与 **[`TokenInvalidException.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/exception/TokenInvalidException.java)**：
   - 继承 `AuthException`，默认状态码设置为 `HttpStatus.UNAUTHORIZED` (401)。

### 2.2 服务与控制层签名清理（消除冗余受检异常声明）
- 依据 [`Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md) 业务方法签名规范，由于 `BusinessException` 为非受检运行时异常（`RuntimeException`），移除以下方法签名中多余的 `throws AuthException` 等受检异常声明：
  - [`AuthService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/AuthService.java)
  - [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)
  - [`AuthController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/AuthController.java)
  - [`TokenController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/TokenController.java)

### 2.3 全局异常处理器日志优化
- [`GlobalExceptionHandler.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandler.java) 的 `handleBusinessException` 中补充 `log.warn(...)` 业务异常告警日志，便于排查与审计。

### 2.4 单元测试补充
- 在 [`AuthVerificationCrossValidationTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/AuthVerificationCrossValidationTest.java) 中新增针对用户名已存在和邮箱已存在时抛出 `UserOrEmailExistException` 并正确继承 `BusinessException`（HTTP 400）的断言测试。

---

## 3. 影响范围与验证结果

- **影响范围**：
  - `volur-auth` 模块中的认证与令牌异常体系；
  - `volur-framework` 全局异常处理机制。
- **验证结果**：
  - `mvn clean compile` 构建成功；
  - `mvn test` 全平台全部单元测试（包括认证、验证码、容器调度、动态 Flag）100% 通过（0 Failures, 0 Errors）。
