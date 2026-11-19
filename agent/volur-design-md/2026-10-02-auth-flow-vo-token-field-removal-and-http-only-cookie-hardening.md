# 认证流响应视图 (Flow VO) 敏感凭证字段彻底剥离与 HttpOnly Cookie 架构加固方案

## 1. 变更背景与安全隐患溯源

在现代 Web 安全规范与本项目的双 Token 架构升级规划中，新一代认证流（包括登录 `/api/v1/auth/signin/...` 与注册 `/api/v1/auth/signup/...`）确立了以 `HttpOnly + SameSite=Lax` Cookie 为唯一权威凭证载体的安全基准。

然而，在审计 `/api/v1/auth/signin/challenge` 响应数据时发现：
```json
{
    "status": 200,
    "errorCode": 0,
    "success": true,
    "message": "挑战核验处理完成",
    "data": {
        "status": "SUCCESS",
        "identifier": "HuMoLix",
        "accessToken": "eyJraWQiOiJYbHR...",
        "refreshToken": "eyJraWQiOiJYbHR...",
        "tokenType": "Bearer"
    },
    "timestamp": "1790892509654",
    "path": "/api/v1/auth/signin/challenge"
}
```

### 1.1 核心问题剖析
1. **凭证二次暴露与 XSS 攻击面未收敛**：
   服务端虽然通过 `Set-Cookie` 写入了 `access_token` 和 `refresh_token` 并设置了 `HttpOnly`，但由于响应体 `data` 中依然显式反序列化输出了明文 `accessToken` 与 `refreshToken`，攻击者一旦在前端页面注入恶意脚本（XSS），即可通过劫持 `fetch`/`XMLHttpRequest` 响应数据直接窃取完整 Token，使得 `HttpOnly Cookie` 的防御机制彻底形同虚设。
2. **废弃字段残留**：
   在先前的无状态化演进中已明确废除 `tokenType: "Bearer"` 的显式传输需求，但在 `SignInFlowResponseVO` 与 `SignUpFlowResponseVO` 中依然声明了 `tokenType`，并在业务层进行了硬编码赋值。
3. **控制器职责倒挂 (Thin Controller 反模式)**：
   `SignInController` 与 `SignUpController` 此前通过检查 `response.getAccessToken()` 来调用 `authCookieService.writeAuthCookies(httpRequest, httpResponse, ...)`。控制器被迫注入 `AuthCookieService` 并解析领域视图内部的敏感令牌，违背了轻量控制器与关注点分离原则。

---

## 2. 架构重构与改造实施

### 2.1 领域模型 (VO) 敏感凭证彻底抹除
从响应视图中彻底删除 `accessToken`、`refreshToken` 和 `tokenType` 属性，切断序列化泄漏链路：
- **`com.humolix.volur.auth.domain.vo.SignInFlowResponseVO`**：
  移除 `accessToken`、`refreshToken`、`tokenType`。保留状态机推进核心字段：`dsh`, `tl`, `cid`, `nextStep`, `status`, `identifier`, `captchaType`, `errorMessage`。
- **`com.humolix.volur.auth.domain.vo.SignUpFlowResponseVO`**：
  移除 `accessToken`、`refreshToken`、`tokenType`。保留状态机推进核心字段：`dsh`, `tl`, `cid`, `nextStep`, `status`, `username`, `email`, `errorMessage`。
- **前端强类型定义 (`src/volur-lite-view-NEXT/src/types/auth/flow.ts`)**：
  同步移除 `SignInFlowResponseVO` 与 `SignUpFlowResponseVO` 中的 `accessToken?: string`、`refreshToken?: string`、`tokenType?: string` 字段，保持前后端契约 100% 镜像一致。

### 2.2 上下文自适应 Cookie 服务扩展 (`AuthCookieService`)
为解耦控制器对 Servlet API 的强制依赖，在 `AuthCookieService` 中新增基于 Spring MVC 全局请求上下文的重载方法：
```java
public void writeAuthCookies(String accessToken, String refreshToken, boolean rememberMe) {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null || attributes.getResponse() == null) {
        return;
    }
    writeAuthCookies(attributes.getRequest(), attributes.getResponse(), accessToken, refreshToken, rememberMe);
}

public void clearAuthCookies() {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null || attributes.getResponse() == null) {
        return;
    }
    clearAuthCookies(attributes.getRequest(), attributes.getResponse());
}
```
该设计支持直接在服务层安全提取当前请求与响应管道，且在单测场景下具备安全的空指针防御与易 Mock 性。

### 2.3 业务服务层凭证下发收敛 (`SignInServiceImpl` & `SignUpServiceImpl`)
将 Cookie 写入动作下沉至业务完成的终端节点：
1. **`SignInServiceImpl`**：
   - 依赖注入 `AuthCookieService`；
   - 在 `pwd` 挑战成功终态及 `2fa`/`totp` 验证成功终态中，生成 JWT 后立即调用 `authCookieService.writeAuthCookies(accessToken, refreshToken, rememberMe)`；
   - 构造返回的 `SignInFlowResponseVO` 仅包含 `status: "SUCCESS"` 与 `identifier`，不携带任何敏感 Token。
2. **`SignUpServiceImpl`**：
   - 依赖注入 `AuthCookieService`；
   - 在 `password` 方法创建用户后，立即下发会话 Cookie `authCookieService.writeAuthCookies(accessToken, refreshToken, false)`；
   - 在 `complete` 完成所有安全选项配置终态时，若存在有效会话凭证亦触发写入并销毁会话；
   - 返回的 `SignUpFlowResponseVO` 严格杜绝 Token 字段。

### 2.4 控制器瘦身与契约纯粹化 (`SignInController` & `SignUpController`)
- 彻底移除 `SignInController` 与 `SignUpController` 中的 `AuthCookieService` 依赖注入；
- 从方法签名中剔除 `HttpServletRequest` 与 `HttpServletResponse` 入参；
- 移除所有手动判断 `response.getAccessToken()` 并补发 Cookie 的业务胶水代码，控制器回归 100% 纯粹转发。

---

## 3. 测试验证与审计结果

### 3.1 单元测试与端到端断言
- **`SignInServiceImplTest`**：
  - 注入 `AuthCookieService` Mock 对象；
  - 新增 `challenge_pwd_success_shouldWriteAuthCookiesAndReturnSuccess()` 专项用例，严格断言密码挑战验证成功后通过 `authCookieService.writeAuthCookies(...)` 下发 Cookie，且断言返回的 `SignInFlowResponseVO` 仅包含 `SUCCESS` 状态及账号标识。
- **全平台编译与测试矩阵**：
  - `mvn test -pl volur-framework,volur-auth`：52 个测试用例全部通过（0 Failures, 0 Errors）；
  - `mvn test`（全平台 13 个模块）：全模块构建成功（`BUILD SUCCESS`）。

### 3.2 前端编译与契约校验
- **`src/volur-lite-view-NEXT`**：
  - 运行 `npm run build`（`vue-tsc --noEmit && vite build`）；
  - TypeScript 类型检查 0 错误，生产打包顺利构建。
  - 确认前端 `signinFlow.ts` 与 `signupFlow.ts` 在登录/注册成功后仅依赖 HTTP Cookie（结合 Axios `withCredentials: true` 及 `X-XSRF-TOKEN` 双提交头），完全脱离前端对明文 Token 的接触与持久化，达成最高级别防 XSS 窃取安全设计。
