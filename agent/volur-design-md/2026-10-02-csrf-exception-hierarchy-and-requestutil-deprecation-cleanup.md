# Volur Lite CSRF 异常处理体系化重构与废弃 RequestUtil 彻底剔除

## 1. 变更背景与架构诱因

在本次重构前，平台在异常响应规范与工具类架构上存在两处违背设计准则的隐患：

1. **底层 Servlet 容器报错打断统一 API 响应格式**：
   - 在 [`CsrfProtectionFilter`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/CsrfProtectionFilter.java) 原先的实现中，当 Origin 校验失败或 CSRF Token 缺失/不匹配时，调用了底层的 `response.sendError(HttpServletResponse.SC_FORBIDDEN, ...)`；
   - 这种处理方式绕过了 Spring Boot 的统一全局异常拦截器机制，由底层 Servlet 容器（如 Tomcat）渲染出默认 HTML 错误页或扁平格式错误响应，破坏了前端对 `ApiResponse<Void>` 标准 JSON 结构的解析契约。

2. **保留历史废弃兼容门面违背彻底性铁律**：
   - 在 `volur-framework` 中，存在旧门面类 `com.humolix.volur.framework.security.util.RequestUtil`，其内容仅为继承 `com.humolix.volur.framework.common.util.RequestUtil` 的 `@Deprecated` 桥接类；
   - 依据 `GEMINI.md` 核心规范第 3 条“重构彻底性铁律（严禁妥协保留与旧命名别名）”，代码库必须彻底清除历史包袱，禁止保留任何废弃桥接或别名层，确保单一真实源。

---

## 2. 核心架构设计与重构方案

### 2.1 强类型 CSRF 异常继承体系设计
参考 `TokenAuthenticationException` 的密封类体系设计，在 `volur-framework` 新增包路径 `com.humolix.volur.framework.common.exception.authException.csrfException`：
- **基类 `CsrfValidationException`**：
  - 继承自 `BusinessException`，HTTP 状态码固定为 `HttpStatus.FORBIDDEN` (403)；
  - 声明为 `sealed abstract class`，显式限定许可子类（`permits CsrfOriginDeniedException, CsrfTokenMismatchException`），为模式匹配提供语法级完备性保证。
- **子类 `CsrfOriginDeniedException`**：
  - 错误码：`131401`；
  - 默认错误文案：“CSRF 来源域名校验未通过”；
- **子类 `CsrfTokenMismatchException`**：
  - 错误码：`131402`；
  - 默认错误文案：“CSRF 凭证校验失败”。

### 2.2 全局异常处理器接入 (`GlobalExceptionHandler`)
在 [`GlobalExceptionHandler`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandler.java) 中注册针对 `CsrfValidationException.class` 的统一处理入口：
- 标注 `@ResponseStatus(HttpStatus.FORBIDDEN)`；
- 采用 Java 21+ 增强型 Switch 模式匹配，根据具体子类自动装配 `ApiResponseEntity.forbidden(errorCode, message)`；
- 保证返回前端标准的强类型结构：
  ```json
  {
    "code": 403,
    "errorCode": 131401,
    "success": false,
    "msg": "CSRF 来源域名校验未通过",
    "data": null,
    "path": "/api/v1/user/profile"
  }
  ```

### 2.3 过滤器层与 Dispatcher 异常桥接 (`CsrfProtectionFilter`)
参考 [`JwtAuthFilter`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/jwt/JwtAuthFilter.java) 的设计模式：
- 注入 Spring MVC 异常解析器 `@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver`；
- 在校验逻辑中，将 `response.sendError` 转为主动抛出 `throw new CsrfOriginDeniedException()` 与 `throw new CsrfTokenMismatchException()`；
- 使用 `try-catch` 捕获异常，并统一交由 `resolver.resolveException(request, response, null, ex)` 分发给 `GlobalExceptionHandler`，实现 Filter 链向 Spring MVC 异常体系的无缝移交。

### 2.4 废弃兼容类物理删除与全链路包路径归一化
- 检索全平台所有依赖 `com.humolix.volur.framework.security.util.RequestUtil` 的调用处；
- 全量修改为唯一真实数据源 [`com.humolix.volur.framework.common.util.RequestUtil`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/util/RequestUtil.java)；
- 物理删除 `src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/util/RequestUtil.java`；
- 同步修正同包内的 [`SecurityContextUtil.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/util/SecurityContextUtil.java)，显式导入 common 包的 `RequestUtil` 并清除冗余注释。

---

## 3. 具体修改文件清单

### 3.1 异常定义与框架层 (`volur-framework`)
1. **新建异常类**：
   - `com.humolix.volur.framework.common.exception.authException.csrfException.CsrfValidationException`
   - `com.humolix.volur.framework.common.exception.authException.csrfException.CsrfOriginDeniedException`
   - `com.humolix.volur.framework.common.exception.authException.csrfException.CsrfTokenMismatchException`
2. **重构异常处理**：
   - [`GlobalExceptionHandler.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandler.java)：新增 `handleCsrfValidationException` 模式匹配分支。
3. **物理删除废弃类**：
   - 删除 `src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/util/RequestUtil.java`。
4. **内部工具修正**：
   - [`SecurityContextUtil.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/util/SecurityContextUtil.java)：引入 `common.util.RequestUtil` 并去除冗余注释。
   - `RequestUtilIpTest.java`、`RequestUtilTest.java`：修正导入路径至 `common.util.RequestUtil`。

### 3.2 认证安全层 (`volur-auth`)
1. [`CsrfProtectionFilter.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/CsrfProtectionFilter.java)：
   - 注入 `HandlerExceptionResolver`；
   - 抛出 `CsrfOriginDeniedException` 与 `CsrfTokenMismatchException`；
   - 通过 `resolver.resolveException(...)` 委派异常。
2. [`SecurityConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java)：修正 `RequestUtil` 导入至 `common.util`。
3. [`RiskIpFilter.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/RiskIpFilter.java)：修正 `RequestUtil` 导入至 `common.util`。
4. [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)：修正 `RequestUtil` 导入至 `common.util`。
5. **新建专项单元测试**：
   - [`CsrfProtectionFilterTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/security/filter/CsrfProtectionFilterTest.java)：包含 7 个独立测试场景，覆盖安全方法放行、未登录请求放行、非法 Origin 拦截阻断并移交 Resolver、缺失 Header 拦截阻断、Token 不一致拦截阻断、合法 Token 放行及备用 Header 携带。

### 3.3 验证风控层 (`volur-verification`)
将以下 7 个业务与管理类中的 `RequestUtil` 导入统一更新为 `com.humolix.volur.framework.common.util.RequestUtil`：
- `AdminRiskController.java`
- `MailTransportServiceImpl.java`
- `RiskEvaluationEngine.java`
- `CaptchaSDKImpl.java`
- `AdminEmailServiceImpl.java`
- `CaptchaServiceImpl.java`
- `EmailServiceImpl.java`

### 3.4 Spring Security 过滤器执行顺序锚点修复 (`SecurityConfig`)
在 Spring Security 架构中，`HttpSecurity.addFilterBefore` / `addFilterAfter` 的目标类必须为 Spring Security 内置在 `FilterOrderRegistration` 中的标准过滤器类。若将自定义类（如 `JwtAuthFilter.class`）作为目标类，将在应用启动构建 `SecurityFilterChain` 时抛出 `IllegalArgumentException: The Filter class ... does not have a registered order`。
- **锚点重构**：
  - `riskIpFilter`：挂载为 `.addFilterBefore(riskIpFilter, CsrfFilter.class)`；
  - `csrfProtectionFilter`：挂载为 `.addFilterAfter(csrfProtectionFilter, CsrfFilter.class)`；
  - `jwtAuthFilter`：挂载为 `.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`；
  - `maintenanceModeFilter`：挂载为 `.addFilterAfter(maintenanceModeFilter, UsernamePasswordAuthenticationFilter.class)`；
- **新建架构单测**：
  - [`SecurityFilterChainTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/security/SecurityFilterChainTest.java)：真实调用 `securityConfig.securityFilterChain(http)` 构建链路，并断言 `riskIpFilter < csrfProtectionFilter < jwtAuthFilter < maintenanceModeFilter` 严格递增顺序。

---

## 4. 验证与审计结果

1. **单元测试与全量套件验证**：
   - `CsrfProtectionFilterTest` 7 个单测全部通过；
   - `SecurityFilterChainTest` 验证 `SecurityFilterChain` 构建无异常且过滤器顺序严格递增；
   - 执行平台全模块测试 `mvn test`：涵盖 13 个子模块全部构建成功（`BUILD SUCCESS`），0 failure，0 error。
2. **规范遵从审计**：
   - 所有新建与修改的代码文件严格遵守“零行内注释”（Zero-Comment）铁律；
   - 彻底删除废弃兼容类，杜绝任何历史别名，保证全平台单一真实数据源。

