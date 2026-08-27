# 修复无状态 JWT 模式下并发会话冲突拦截（ConcurrentSessionFilter）记录

**施工日期**：2026-08-27  
**施工模块**：`src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java`  
**责任主体**：Antigravity AI Agent

---

## 1. 异常现象与问题背景

在前端多接口并发请求（例如页面加载时同时请求 `/api/v1/admin/users/statistics`、`/api/v1/notifications/unread-count` 等）或直接访问管理端接口时，Spring Security 抛出拦截异常并响应：
```
This session has been expired (possibly due to multiple concurrent logins being attempted as the same user).
```

---

## 2. 根本原因分析 (Root Cause)

1. **架构模式冲突**：
   - Volur Lite 采用的是纯**无状态 JWT 鉴权体系（Stateless JWT Architecture）**，会话策略配置为 `SessionCreationPolicy.STATELESS`，每次请求均通过 HTTP 请求头中的 `Authorization: Bearer <token>` 进行身份认证，服务端不创建也不依赖 Tomcat `HttpSession`。
2. **配置反模式**：
   - 在 [`SecurityConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java) 中，误添加了 `.maximumSessions(1)`：
     ```java
     .sessionManagement(session -> session
             .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
             .maximumSessions(1) // <-- 冲突配置
     )
     ```
   - 当配置了 `.maximumSessions(1)` 时，Spring Security 会在过滤器链中强行挂载 `ConcurrentSessionFilter` 和 `ConcurrentSessionControlAuthenticationStrategy`。
   - 由于在无状态模式下没有有效的 Servlet Session 管理机制，并发请求到达时，`ConcurrentSessionFilter` 误认为同一用户的多个会话发生了冲突，从而主动将后续请求标记为过期并直接阻断拦截。

---

## 3. 修复方案

在 [`SecurityConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java) 中移除 `.maximumSessions(1)`，保持标准的无状态 Session 管理策略：

```java
.sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

---

## 4. 验证与审计

- **编译构建**：执行 `mvn compile -pl volur-auth -am`，编译通过（`BUILD SUCCESS`）。
- **影响消除**：彻底移除了 `ConcurrentSessionFilter` 对无状态并发 HTTP 请求的误拦截，所有并发 API 请求均恢复正常。
