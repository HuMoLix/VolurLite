# Session 请求头对象化与全量请求头覆盖模型 RequestHeader 施工记录

## 1. 方案背景与目标

在 Volur Lite 统一安全认证流与分布式会话管理中，早期仅在会话中记录了单一的字符串标量 `userAgent`。然而在现代 Web 威胁对抗与多维设备环境特征指纹分析中，单一 `User-Agent` 存在明显局限：
1. **现代浏览器 Client Hints 演进**：Chrome / Chromium、Edge 及现代移动浏览器逐步冻结并弱化传统 `User-Agent` 字符串，转向采用结构化的 Client Hints 标头（如 `Sec-CH-UA`, `Sec-CH-UA-Mobile`, `Sec-CH-UA-Platform`, `Sec-CH-UA-Platform-Version`）；
2. **防 CSRF 导航与请求模式审计**：现代反爬与风控体系依赖 Fetch Metadata 标头（`Sec-Fetch-Site`, `Sec-Fetch-Mode`, `Sec-Fetch-Dest`）以及 `Origin`, `Referer`；
3. **多维特征与全量覆盖要求**：会话需要具备全面覆盖客户端所有标准与非标准自定义请求头（如 CDN 透传头、自定义设备凭证、跟踪链头等）的能力，同时提供对常用核心头的强类型结构化解析。

因此，本次重构的核心目标为：
- 在 `com.humolix.volur.security.domain.session` 下创建强类型且具备全量覆盖能力的 `RequestHeader` 模型；
- 将基础会话 `Session` 中的 `userAgent` 属性升级重构为 `requestHeader`；
- 全链路同步改造 `TransitionLockManager`、`FlowServiceImpl`、`SigninServiceImpl`、`SignupServiceImpl` 及相关单元测试；
- 保持 Jackson 序列化在 Redis 中的完全无缝双向兼容。

---

## 2. 核心架构设计与改动点

### 2.1 请求头模型设计 (`RequestHeader.java`)

位置：`src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/session/RequestHeader.java`

- **核心强类型标头**：
  - 客户端识别：`userAgent` (`User-Agent`)；
  - 内容协商：`accept`, `acceptLanguage`, `acceptEncoding`；
  - Client Hints：`secChUa`, `secChUaMobile`, `secChUaPlatform`, `secChUaPlatformVersion`；
  - 来源与隔离防护：`origin`, `referer`, `secFetchSite`, `secFetchMode`, `secFetchDest`；
  - 代理透传：`xForwardedFor`, `xRealIp`, `xRequestedWith`, `host`, `connection`, `cacheControl`, `upgradeInsecureRequests`。
- **全量动态覆盖字典**：
  - `@Builder.Default private Map<String, String> headers = new HashMap<>();`
  - 自动遍历 `HttpServletRequest.getHeaderNames()`，将传入的所有请求头以小写规范化 Key 完整保留，确保 100% 覆盖任何自定义请求头；
  - 提供不区分大小写的 `getHeader(String name)` 查询方法。
- **工厂与辅助方法**：
  - `RequestHeader.from(HttpServletRequest request)`：从 Servlet 请求实例提取全量标头与映射；
  - `RequestHeader.fromCurrentRequest()`：从当前 Spring Web 上下文中安全提取；
  - `RequestHeader.fromUserAgent(String userAgent)`：便捷适配纯 UA 构造场景。

### 2.2 基础会话模型更新 (`Session.java`)

位置：`src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/session/Session.java`

- 将 `private String userAgent;` 替换为 `private RequestHeader requestHeader;`；
- 继承 `Session` 的 `AuthFlowSession` 自动获得 `requestHeader` 的访问与 `@SuperBuilder` 构建能力。

### 2.3 全链路同步重构清单

1. **`TransitionLockManager.java`**：
   - 将 `createOrGetSession` 的最后一个入参由 `String userAgent` 更新为 `RequestHeader requestHeader`；
   - 比较与持久化更新 `requestHeader` 对象。
2. **`FlowServiceImpl.java`**：
   - 调用 `RequestHeader.fromCurrentRequest()` 提取当前环境请求头，并传递至 `createOrGetSession`。
3. **`SigninServiceImpl.java`**：
   - 替换 `RequestUtil.getUserAgent()` 为 `RequestHeader.fromCurrentRequest()`，并为 `session` 设置 `requestHeader`。
4. **`SignupServiceImpl.java`**：
   - 替换 `RequestUtil.getUserAgent()` 为 `RequestHeader.fromCurrentRequest()`，传入 `createOrGetSession`。
5. **单元测试落地**：
   - `TransitionLockManagerTest.java`：更新为传入 `RequestHeader`；
   - `SessionInheritanceTest.java`：验证 `RequestHeader` 的属性赋值、`getHeader` 大小写不敏感查找、全量请求头保留及 Jackson 双向序列化一致性。

---

## 3. 规范与纯粹性审计

1. **纯粹代码与零注释铁律**：
   - `RequestHeader.java`、`Session.java` 等源码严格杜绝任何解释性注释与行内冗余备注；
   - 设计考量与字段规范统一记录于本方案文档。
2. **重构彻底性原则**：
   - 彻底废除 `session.getUserAgent()` / `session.setUserAgent(...)`，无遗留废弃别名或兼容层；
   - 全链路（Security、Auth、Test）彻底同步。

---

## 4. 验证与回归测试

1. **模块单元测试**：
   - 执行 `mvn test "-Dtest=SessionInheritanceTest,TransitionLockManagerTest"`，10 个测试全部通过。
2. **平台全链路测试**：
   - 执行 `mvn test "-Dtest=SessionInheritanceTest,TransitionLockManagerTest,SigninServiceImplTest,SignupServiceImplTest,PasskeyServiceImplTest,RecaptchaServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"`；
   - 13 个子模块全部构建成功（`BUILD SUCCESS`），测试用例 100% 通过。
