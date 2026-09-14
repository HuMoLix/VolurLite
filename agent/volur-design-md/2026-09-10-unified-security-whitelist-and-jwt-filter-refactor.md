# 统一安全白名单单一真实源 (SSOT) 与 JWT 过滤器优雅降级重构

## 1. 变更背景与问题剖析

### 问题背景
前端在调用 `/api/v1/auth/logout` 登出接口时被拦截受限，无法正常退出登录。

### 根本原因剖析
1. **多重白名单分散维护（违反 SSOT 原则）**：
   - 在 [`SecurityConfig.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java) 中，`authorizeHttpRequests` 显式声明了一批 `permitAll()` 路径，但**遗漏了 `/api/v1/auth/logout`**，导致未携带 Token 或匿名请求被末尾的 `.anyRequest().authenticated()` 捕获并返回 401。
   - 在 [`JwtAuthFilter.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/jwt/JwtAuthFilter.java) 中，私有维护了一个静态集合 `WHITE_LIST`，不仅同样缺失 `/api/v1/auth/logout`，且使用 `Set.contains` 精确字符串匹配，无法兼容 Ant 风格通配符（如 `/**`）。
2. **认证过滤器的越权强拦截缺陷**：
   - `JwtAuthFilter` 在未命中其硬编码白名单的接口上，一旦遇到 Token 过期（`ExpiredJwtException`）或非法签名，会直接在 Filter 阶段提前截断请求并输出 401 响应。
   - 当客户端在 Token 已经过期的情况下点击登出或浏览某些公开资源（如公开比赛列表）时，请求在到达 Controller 之前便被过滤器强行掐断，导致用户“既无法使用旧 Token，也无法正常退出登录”的死锁状态。

---

## 2. 核心架构设计与改动点 (方案 A + 方案 B 深度融合)

### 2.1 抽取单一真实数据源白名单组件 (方案 A: SSOT)
1. **新建 [`SecurityWhitelistProperties.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityWhitelistProperties.java)**：
   - 统一收敛系统中所有默认公开的 API 路由（包含认证、验证码、邮件、公开比赛、排行榜、广播、WebSocket、文档、静态文件以及 `/api/v1/auth/logout`）。
   - 支持通过 Spring 配置属性 `volur.security.whitelist.additional-paths` 进行环境级自定义扩展。
2. **新建 [`SecurityWhitelistMatcher.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/util/SecurityWhitelistMatcher.java)**：
   - 统一封装基于 Spring `AntPathMatcher` 的通配符匹配引擎，支持对 `HttpServletRequest`、`ServletPath` 及 `RequestURI` 进行一致性判定。
   - 提供 `getPatterns()` 方法供 Spring Security 注册使用。

### 2.2 重构 Spring Security 安全过滤链配置
- 修改 [`SecurityConfig.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java)：
  - 移除此前散落、硬编码的几十行路由声明，统一注入 `SecurityWhitelistMatcher`。
  - 配置 `.requestMatchers(whitelistMatcher.getPatterns()).permitAll()`，确保 Spring Security 授权层与白名单组件 100% 保持单一数据源对齐。

### 2.3 重构 JWT 认证过滤器：实现认证与授权职责解耦与优雅降级 (方案 B)
- 修改 [`JwtAuthFilter.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/jwt/JwtAuthFilter.java)：
  - 彻底移除原先孤立的硬编码常量 `private static final Set<String> WHITE_LIST` 及 `shouldNotFilter` 精确匹配逻辑。
  - 注入 `SecurityWhitelistMatcher`。
  - **优雅降级策略**：
    - 当请求**未携带 Token**：直接 `filterChain.doFilter(request, response)` 放行，后续权限由 Spring Security 最终裁决。
    - 当请求携带有效 Token：解析并注入 `SecurityContextHolder`（即使是白名单公开接口也能识别登录身份，满足 `@CurrentUser(required = false)` 的用户画像场景）。
    - 当请求携带**已过期、已拉黑、非法格式或已被强制下线的 Token**：
      - 若当前请求命中**白名单接口**（如 `/api/v1/auth/logout` 或公开比赛列表）：不截断报错，记录 debug 日志，优雅降级为匿名身份继续执行后续过滤器链，确保登出与公开浏览顺畅。
      - 若当前请求为**受保护的业务接口**：维持细粒度 401 拦截返回（"令牌已过期" / "令牌已失效" / "账号已在其他位置强制登出"），无缝驱动前端 Axios 响应拦截器触发无感 Refresh Token 换票重试。

### 2.4 补齐维护模式白名单与依赖注入规范
- 修改 [`MaintenanceModeFilter.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/MaintenanceModeFilter.java)：在 `EXEMPT_PATHS` 中补齐 `"/api/v1/auth/logout"`，确保在系统维护时用户亦可正常发起登出。
- 规范化 [`SecurityAuthenticationEntryPoint.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/exception/SecurityAuthenticationEntryPoint.java)：消除手动编写的构造方法，全面对齐 Lombok `@RequiredArgsConstructor` 规范。

---

## 3. 影响范围与验证结果

- **影响模块**：`volur-auth`
- **兼容性**：完全向下兼容。前端 Axios 的 JWT 鉴权与 Refresh Token 刷新协议无需变动。
- **配置属性类规范保障 (Spring Boot ConfigurationProperties Record Binding)**：
  - 在将 `SecurityWhitelistProperties` 设计为 Java Record 时，必须保证其为**唯一紧凑型构造器 (Single Compact Constructor)** 模式，只保留 `paths` 与 `additionalPaths` 作为属性组件。避免由于声明多个重载构造器而导致 Spring Boot 无法推断构造函数绑定进而回退至 JavaBean 无参构造（引发 `NoSuchMethodException: <init>()`）的问题。
- **构建测试验证**：
  - 新增 [`SecurityWhitelistMatcherTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/security/SecurityWhitelistMatcherTest.java) 自动化测试，覆盖配置属性 Binder 绑定、白名单与通配符匹配及受限路径校验，测试 100% 通过。
  - 执行 `mvn clean test-compile`：全平台 12 个模块全部编译通过，0 错误，0 告警（`BUILD SUCCESS`）。
