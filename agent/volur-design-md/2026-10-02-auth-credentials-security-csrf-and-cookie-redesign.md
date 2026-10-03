# Volur Lite 凭证安全加固、双 Token Cookie 差异化存储与 CSRF 防护重构

## 1. 变更背景与安全风险剖析

在先前的实现中，系统的前端凭证管理与后端鉴权链路存在若干典型的 Web 架构安全隐患与缺陷：

1. **LocalStorage 存储令牌导致的 XSS 完全沦陷风险**：
   - 前端登录、注册成功后将 `access_token` 与 `refresh_token` 写入浏览器 `localStorage`；
   - 一旦前端存在任何形式的跨站脚本漏洞（XSS）或第三方恶意依赖脚本注入，攻击者即可通过 `window.localStorage` 瞬间窃取具有长期有效期的双令牌，彻底打破身份认证防线。

2. **多来源混杂读取令牌（Multi-Source Token Parsing）引发的混乱与泄露**：
   - 后端 `JwtUtil.parseToken` 及 WebSocket 拦截器此前同时支持从 `Authorization` 请求头、URL 请求参数（`?token=xxx`、`?access_token=xxx`）以及 Cookie 中读取 Token；
   - 将 Token 放置在 URL 查询参数中会导致敏感凭证被记录在反向代理（Nginx/HAProxy）、CDN 访问日志、浏览器历史记录或 `Referer` 头中，违背凭证保密原则，且给鉴权链路带来多入口维护隐患。

3. **缺乏系统性 CSRF 防御机制**：
   - 系统原先在 Spring Security 中禁用了默认 CSRF 保护（`.csrf(AbstractHttpConfigurer::disable)`），但未实现无状态应用的防御策略；若单纯将 Token 转存至 Cookie，浏览器跨站发送请求时将默认携带 Cookie，极易遭受跨站请求伪造攻击（CSRF）。

4. **HTTP 与 HTTPS 混合部署场景下的 Cookie 丢失隐患**：
   - 线下 CTF 竞赛、内网测试演练通常基于纯 HTTP（如 `http://192.168.x.x:8080`）环境运行；若强制写死 `Secure=true`，现代浏览器（Chrome、Firefox 等）会基于安全标准直接丢弃该 Cookie，导致用户无法登录。

5. **中间流转响应冗余与伪造兜底隐患**：
   - `SignUpFlowResponseVO` 与 `SignInFlowResponseVO` 在多步注册或多步挑战过程中，若处于中间未决状态（非终端状态），仍可能序列化空字段或默认的 `tokenType = "Bearer"`，违背 Zero-Mock 与精简响应原则。

---

## 2. 核心架构设计与重构方案

### 2.1 凭证传输通道安全性分析（WebSocket 与 SSE）
- **原生浏览器 EventSource (SSE)**：
  - W3C 规范中原生 `EventSource` 不支持自定义请求头（无法添加 `Authorization: Bearer`）；
  - 支持 `withCredentials: true` 自动通过 Cookie 传输鉴权凭据，与 Cookie 存储方案天然契合。
- **原生浏览器 WebSocket**：
  - 原生 `WebSocket(url)` 构造器同样不支持注入自定义 HTTP 请求头；
  - 浏览器在发起 HTTP 101 Switching Protocols 握手请求时，会自动携带同源（或符合 CORS 策略跨域）的 Cookie。
- **架构决策**：全面废除 URL 参数传递 Token 的危险方式，统一使用 Cookie 作为 WebSocket 握手与 SSE 订阅的标准身份承载通道。

### 2.2 双 Token 差异化存储矩阵设计
在安全加固方案中，针对不同生命周期与敏感程度的凭据，采取差异化的存储与隔离策略：

| 凭证名称 | 存储介质 | HttpOnly | Path 范围 | SameSite | Secure 策略 | 用途与安全考量 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`access_token`** | Cookie | **true** | `/` | **Lax** | 动态（HTTPS 为 true，HTTP 为 false） | 短期访问凭证（默认 1 小时），彻底禁止 JavaScript 读取，免疫 XSS 窃取 |
| **`refresh_token`** | Cookie | **true** | `/api/v1/auth/refresh` | **Lax** | 动态（HTTPS 为 true，HTTP 为 false） | 长期刷新凭证（默认 7 天），**Path 严格限定在刷新接口**，普通业务请求不会携带，降低泄露面 |
| **`logged_in`** | Cookie | **false** | `/` | **Lax** | 动态（HTTPS 为 true，HTTP 为 false） | 轻量登录指示标志（布尔标记），允许前端 Vue Router 路由守卫读取状态 |
| **`XSRF-TOKEN`** | Cookie | **false** | `/` | **Lax** | 动态（HTTPS 为 true，HTTP 为 false） | 双重提交 CSRF 令牌（UUID），供前端读取并通过自定义 Header 回传 |

### 2.3 深入的 HTTP 与 HTTPS 动态自适应策略
为兼容生产 HTTPS 与内网纯 HTTP 部署环境，后端在写入 Cookie 时统一调用 `AuthCookieService.isSecure(HttpServletRequest)` 进行动态探查：
- 探查逻辑：
  1. 检查底层 Servlet 协议：`request.isSecure()` 为 true 即为 HTTPS；
  2. 检查反向代理协议头：`X-Forwarded-Proto` 或 `X-Forwarded-Scheme` 头匹配 `https`（不区分大小写）；
  3. 仅当探测为安全连接时设置 `Secure=true`；在纯 HTTP 环境下置为 `false`，确保浏览器正常收录与携带 Cookie。

### 2.4 CSRF 双重防御矩阵（SameSite + Origin / Header 双提交）
在后端引入前置过滤器 [`CsrfProtectionFilter`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/CsrfProtectionFilter.java)：
1. **安全动词免检**：`GET`, `HEAD`, `OPTIONS`, `TRACE` 属于幂等只读方法，予以放行；
2. **公开与白名单接口免检**：登录、刷新 Token、公开只读接口等直接放行；
3. **针对受保护的有状态请求（`POST`, `PUT`, `DELETE`, `PATCH`）执行双重校验**：
   - **第一道防线（Origin / Host 校验）**：提取请求中的 `Origin` 头（若缺失回退至 `Referer` 解析出的 Origin），使用 `RequestUtil.isOriginAllowed` 进行同源/自适应白名单核验；若为非法跨站 Origin，直接阻断并返回 `403 Forbidden`；
   - **第二道防线（Double Submit Cookie 校验）**：检查前端通过 `X-XSRF-TOKEN`（或 `X-CSRF-TOKEN`）自定义请求头回传的随机防伪令牌，必须与请求携带的 `XSRF-TOKEN` Cookie 值完全一致。由于浏览器的同源策略（SOP），跨站攻击站点既无法读取用户 Cookie 中的 `XSRF-TOKEN`，也无法向受保护域跨域注入该自定义 Header。

---

## 3. 具体落地与改动清单

### 3.1 后端工程改造 (`src/volur-lite-platform`)

1. **`JwtUtil.java` 严格单一源改造**：
   - 彻底废除 `parseTokenFromHeader`，废除对 `Authorization` 头和 URL 查询参数 `token` / `access_token` 的探测；
   - `parseToken(HttpServletRequest request)` 严格仅通过 `RequestUtil.getCookieValue(request, "access_token")` 提取凭据；若 Cookie 不存在则直接抛出 `TokenMissingException`。
   - 补充暴露 `getAccessTokenValidity()` 与 `getRefreshTokenValidity()`，供 Cookie 响应管理服务读取精确过期时长。

2. **新增 `AuthCookieService.java`**：
   - 封装集中化的凭证 Cookie 写入与销毁机制：`setAuthCookies` 与 `clearAuthCookies`；
   - 统一写入 4 组差异化 Cookie：`access_token` (HttpOnly), `refresh_token` (HttpOnly, 限定 Path), `logged_in` (非 HttpOnly), `XSRF-TOKEN` (非 HttpOnly)；
   - 登出时统一通过 MaxAge = 0 清除全部凭证与关联标识。

3. **新增 `CsrfProtectionFilter.java` 并接入 `SecurityConfig.java`**：
   - 实现无状态 CSRF 防护过滤器，挂载在 `SecurityFilterChain` 的 `JwtAuthFilter` 前方；
   - 在 `SecurityConfig.corsConfigurationSource()` 中显式启用 `configuration.setAllowCredentials(true)`，为跨端口/跨源调试提供合法凭据跨域支持。

4. **控制器层集成与流程适配**：
   - `AuthController.java`：用户常规账号密码登录成功后，通过 `AuthCookieService.setAuthCookies` 注入凭证 Cookie；
   - `TokenController.java`：登出接口调用 `clearAuthCookies`；刷新令牌接口优先从 Cookie 读取 `refresh_token`，并在刷新后覆写新 Cookie；
   - `SigninController.java`：在 Google 风格挑战完成（`status == "SUCCESS"`）发放 Token 时，通过 `AuthCookieService.setAuthCookies` 写入 Cookie；
   - `SignupController.java`：在多步注册最终步骤（`password` 或直接完成）颁发 Token 时，注入凭据 Cookie。

5. **WebSocket 握手安全改造**：
   - `CompetitionChatHandshakeInterceptor.java`：移除原先从 URL Query（`?token=`）与请求头提取 Token 的降级逻辑，严格仅从 WebSocket 握手请求 Cookie 中解析 `access_token` 进行鉴权与身份绑定。

6. **VO 响应层精简与防伪兜底防护**：
   - `SignUpFlowResponseVO.java` 与 `SignInFlowResponseVO.java`：标注 `@JsonInclude(JsonInclude.Include.NON_NULL)`；
   - 移除 `tokenType = "Bearer"` 的手写默认值，防止在未决步骤反序列化出伪造数据，杜绝隐式 Mock 违规。

### 3.2 新一代前端改造 (`src/volur-lite-view-NEXT`)

1. **强类型 Axios 实例改造 (`src/utils/request.ts`)**：
   - 开启全局凭证携带：`withCredentials: true`；
   - 移除从 `localStorage` 读取 `access_token` 并拼接 `Authorization: Bearer` 的历史逻辑；
   - 封装 `getCookie` 工具函数；在请求拦截器中，对变更型操作（`POST`, `PUT`, `DELETE`, `PATCH`）自动提取 `XSRF-TOKEN` Cookie 并附带至 `X-XSRF-TOKEN` 请求头；
   - 401 自动无感刷新机制：在刷新接口调用时不再手动传递 body/header，由浏览器自动携带限定路径的 `refresh_token` Cookie 进行静默置换。

2. **状态机与认证流程改造 (`src/store/modules/flow/`)**：
   - `signinFlow.ts` 与 `signupFlow.ts`：彻底移除针对 `access_token` 与 `refresh_token` 的 `localStorage.setItem` 操作，交由浏览器内核与后端 Set-Cookie 统一托管。

3. **前端路由守卫改造 (`src/router/index.ts`)**：
   - 移除对 `localStorage.getItem('access_token')` 的依赖；
   - 改为通过正则匹配 `document.cookie` 中的 `logged_in=true` 指示标识，实现轻量、安全的未登录状态重定向拦截。

---

## 4. 验证与审计结果

1. **测试套件全量通过**：
   - `volur-framework` 与 `volur-auth` 模块单测套件编译通过并全量执行：`Tests run: 230, Failures: 0, Errors: 0`；
   - 针对 `JwtUtilTest` 进行了专项测试更新，断言验证了仅当 Cookie 存在 `access_token` 时成功解析，来自 `Authorization` 头或 URL 查询参数的传入均严格抛出 `TokenMissingException`；
   - 针对 `SecurityConfigCorsTest` 校验了 `allowCredentials = true` 及动态同源推导规则。
2. **前端工程构建通过**：
   - `src/volur-lite-view-NEXT` 运行 `vue-tsc --noEmit && vite build`，编译零错误零警告，打包无类型冲突。
3. **规范遵从审计**：
   - 所有新增与重构的 `.java`、`.ts` 文件均严格遵循 Zero-Comment（零注释）铁律；
   - 严格遵循 Fast-Fail 与 Zero-Mock 铁律，不伪造、不默认兜底；
   - 严格遵循单一真实源原则，彻底清除多源 Token 隐患与过时别名桥接。
