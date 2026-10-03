# TokenController 规范重构、Token 生命周期下沉、前端主动静默刷新及沙盒 iframe CSRF 评估

## 1. 变更背景与架构动因

在 Volur Lite 平台的无状态双 Token 认证安全演进中，系统已经确立了以 `HttpOnly + SameSite=Lax` Cookie 作为唯一权威凭据载体的安全架构（先后落地于 `SignInFlowResponseVO` 与 `SignUpFlowResponseVO`）。

然而，在对 Token 控制器与认证全链路进行全面审计时，发现了以下架构不一致与设计痛点：

1. **`TokenController` 仍停留在旧范式并违背零凭证泄漏原则**：
   - 依赖注入 `AuthCookieService`，入参强耦合 `HttpServletRequest` 与 `HttpServletResponse`；
   - `refreshToken` 方法签名返回 `ApiResponseEntity<AuthenticateResponseVO>`，在 HTTP 响应报文 Body 中显式返回了明文的 `accessToken` 与 `refreshToken`。这使得前端 XSS 攻击者依然能通过劫持网络响应获取凭证，导致 HttpOnly 防御机制功亏一篑；
   - 请求体依旧声明接收 `RefreshTokenRequestDTO`，未完全收敛到浏览器限定 Path（`/api/v1/auth/refresh`）的 Cookie 传输模式。

2. **职责划分倒挂（SRP 违背）**：
   - `TokenController` 的职责是管理令牌的生命周期（刷新轮转、吊销拉黑）；
   - 但此前的实现中，`refreshToken` 和 `logout` 挂载在 `AuthService`（负责用户注册、登录密码校验、账号注销等账号域服务）下，而非 `TokenService`。

3. **双 Token 体系下前端“静默提前刷新”的架构矛盾**：
   - 目前的前端 401 被动刷新机制会在凭证过期后产生一次请求失败抖动；
   - 用户设想在前端解析 JWT 的 `exp` 时间戳进行提前刷新；但因 `access_token` 已被置为 `HttpOnly`，前端 JavaScript 无法通过 `document.cookie` 读取到 JWT 字符串，无法直接在前端 `jwt_decode`。

4. **Spring Security CSRF 防护与前端沙盒 `<iframe>` (`recaptcha.vue`) 的安全性评估需求**：
   - 评估后端 `SecurityConfig` 开启 CSRF 防护后是否会对前端基于 `<iframe>` 的人机验证组件产生阻断或副作用。

---

## 2. 核心架构重构与落地实施

### 2.1 令牌管理职责彻底收敛至 `TokenService`

1. **解耦 `JwtUtil` 对 `TokenService` 的反向依赖**：
   - `JwtUtil` 专注于无状态密码学处理（HS256 编解码、Claims 提取、签名校验）；
   - 移除 `JwtUtil` 中的 `TokenService` 成员变量与 `validateTokenWithBlacklist`，使 `JwtUtil` 保持零外部业务依赖；
   - 彻底避免 `TokenServiceImpl -> JwtUtil -> TokenService` 潜在的 Spring Bean 循环依赖。

2. **`TokenService` 增强与实现**：
   - 接口新增命令式方法：`void refreshToken()` 与 `void logout()`；
   - 在 `TokenServiceImpl` 中依赖 `JwtUtil`、`UserRepository`、`AuthCookieService` 与 `StringRedisTemplate`；
   - **`refreshToken()` 业务闭环**：
     - 从请求上下文 Cookie 提取 `refresh_token`；
     - 校验有效性、JTI 黑名单、用户有效性与 `isUserForceLoggedOut` 强制下线时间戳；
     - 将旧 JTI 加入 Redis 黑名单；
     - 签发新 JTI 及新双 Token；
     - 自动调用 `authCookieService.writeAuthCookies(...)` 下发最新 Cookie。
   - **`logout()` 业务闭环**：
     - 从请求上下文 Cookie 提取 `access_token` 并将其 JTI 加入 Redis 黑名单；
     - 执行 `SecurityUtils.logout()`；
     - 自动调用 `authCookieService.clearAuthCookies()` 清除全部凭证 Cookie。

3. **`AuthService` 接口与实现瘦身**：
   - 移除 `AuthService` 中的 `refreshToken` 与 `logout` 方法；
   - `AuthServiceImpl.deleteAccount` 中原先调用的 `logout` 统一收敛至 `tokenService.logout()`。

### 2.2 `TokenController` 彻底改造为 Thin Controller

重构后的 `TokenController` 达到 100% 极简纯粹：
- 仅注入 `TokenService`；
- 移除所有 Servlet API 原生对象入参；
- 移除 `RefreshTokenRequestDTO` 请求体；
- 统一返回 `ApiResponseEntity<Void>`，方法直接返回 `ApiResponseEntity.success("...")`；
- 所有 Cookie 写入与清除均在 Service 层内聚，并在 Controller 构造响应实体时由 `CookieContextHolder` 自动聚合到 `Set-Cookie` 响应头中。

### 2.3 双 Cookie 元数据模式：前端“主动静默提前刷新”落地方案

针对“既要 HttpOnly 防 XSS，又要前端感知过期时间戳”的核心诉求，确立了**双 Cookie / 元数据分离标准方案**：

1. **后端写入非敏感过期时间戳 Cookie (`access_exp`)**：
   - 在 `AuthCookieService.writeAuthCookies` 中，写入 HttpOnly 的 `access_token` 的同时，写入 `access_exp` Cookie：
     - `httpOnly: false`（允许 JavaScript 读取）；
     - `value: String.valueOf(System.currentTimeMillis() + accessExp * 1000L)`；
     - `path: "/"`，`sameSite: "Lax"`，`secure: dynamic`；
   - 在 `clearAuthCookies` 时一并置空清理；
   - 安全性保证：`access_exp` 仅为一个纯数字毫秒时间戳，不携带任何用户信息、雪花 ID、角色或签名，攻击者窃取无任何利用价值。

2. **前端落地收敛至根组件唯一生命周期 (`src/volur-lite-view-NEXT/src/App.vue`)**：
   - **设计收敛原则**：不在 `request.ts` 拦截器中混杂刷新定时器或拦截并发阻塞，确保 HTTP 请求拦截器保持纯粹与零竞态；
   - **根组件定时器机制 (`App.vue`)**：
     - 在 `onMounted` 根组件初始化完成后，启动高敏心跳定时器（每 15 秒探查一次）；
     - 读取 Cookie 中的 `logged_in` 与 `access_exp`；
     - 若 `logged_in === 'true'` 且 `remain = exp - Date.now() < 300000`（进入 5 分钟安全阈值）：
       - 使用互斥状态标志 `isTokenRenewing` 防重入；
       - 主动触发 `refreshTokenApi()` 执行静默续期；
       - 续期成功后，服务端透明下发新 `Set-Cookie`，重置 `access_exp` 与凭证；
       - 若续期发生异常，且检查最新 `remain <= 0`（凭证已彻底失效），自动通过 `router.push` 将用户重定向至登录入口 `/v1/signin/identifier`，并附带 `redirect` 原页面查询参数；
     - 在 `onUnmounted` 卸载时严格销毁 `clearInterval`，防止内存泄露。

### 2.4 CSRF 防护与沙盒 `<iframe>` (`recaptcha.vue`) 兼容性深度评估

经全方位架构审计，**后端开启 CSRF 防护与 iframe 防护对 `recaptcha.vue` 没有任何负面影响**，原因如下：

1. **非远程加载，而是纯本地内联 `srcdoc` 虚拟沙盒**：
   - `recaptcha.vue` 采用 `:srcdoc="sandboxHtml"`，浏览器在内存中直接解析并渲染，不向后端请求任何 HTML 页面；
   - 后端 `SecurityConfig` 显式配置了 `.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)`，且即便开启亦仅约束从后端加载的 HTML 页面，对内联 `srcdoc` 完全无作用。

2. **沙盒内部零 HTTP 外发**：
   - `sandboxHtml` 内部仅采集鼠标轨迹、WebGL / Canvas / Audio 指纹，并通过 `window.parent.postMessage` 向 Vue 宿主传递事件，内部绝不直接调用 `fetch` 或 `XMLHttpRequest`。

3. **宿主网络请求完全符合 CSRF 双重防御规范**：
   - 人机验证的实际接口交互（`getRecaptchaChallengeApi`、`verifyRecaptchaChallengeApi`）全部在宿主页面的主执行上下文中由 Axios 统一发出；
   - Axios 全局配置了 `withCredentials: true`，且在请求拦截器中对 `POST` 请求自动从 Cookie 读取 `XSRF-TOKEN` 并挂载至 `X-XSRF-TOKEN` 请求头；
   - 请求发起时浏览器自动附带同源 `Origin` 头，完全满足 `CsrfProtectionFilter` 的双重核验（Origin 白名单校验 + Double Submit Header 核验）。

---

## 3. 测试验证与审计结果

1. **后端测试全量通过**：
   - 编写新增 `TokenServiceImplTest.java` 单元测试，覆盖 Cookie 提取、有效性校验、双 Token 轮转写 Cookie 及登出黑名单逻辑；
   - 运行 `mvn test -pl volur-framework,volur-auth`，54 个测试用例全部通过（0 Failures, 0 Errors）。
2. **前端工程构建通过**：
   - 针对 `src/volur-lite-view-NEXT` 运行 `npm run build`（`vue-tsc --noEmit && vite build`）；
   - TypeScript 类型推导 0 错误，生产资源构建顺利完成。
3. **架构规范遵从**：
   - 严格遵循 Thin Controller 原则；
   - 严格遵循 Zero-Comment（零注释）铁律；
   - 严格遵循 Fail-Fast & Zero-Mock 铁律；
   - 严格遵循单一真实源原则，彻底清除明文 Token 泄漏隐患。
