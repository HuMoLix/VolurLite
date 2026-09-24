# 生产环境自适应同源与 Host 动态校验 CORS 跨域防御加固

## 1. 变更背景与问题分析

在原先的 `SecurityConfig.java` 中，CORS 配置如下：
```java
CorsConfiguration configuration = new CorsConfiguration();
configuration.setAllowedOriginPatterns(List.of("*"));
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
configuration.setAllowedHeaders(List.of("*"));
configuration.setAllowCredentials(true);
```

### 1.1 安全与架构隐患
1. **通配符 + 凭据滥用**：在 Spring Security / Spring Web 体系中，`setAllowedOriginPatterns(List.of("*"))` 搭配 `setAllowCredentials(true)` 会对任意来源站点的请求动态反射 `Access-Control-Allow-Origin: <origin>` 与 `Access-Control-Allow-Credentials: true`。虽然在现有无状态 Bearer Token 体系下，第三方恶意站点无法跨域直接读取 `localStorage` 窃取受保护接口的数据，但其打破了浏览器的同源限制边界；
2. **内网与公共接口嗅探（Intranet Pivoting）**：若系统部署于内网或本地调试环境，外部恶意页面可通过受害者浏览器直接跨域探测公开端点（如 `/api/v1/system/settings/public`、`/api/v1/competitions/**`、Swagger 文档等）并捕获返回信息；
3. **架构隐性回归风险**：若后续架构演进中引入了基于 `HttpOnly Cookie` 的 Token 存储方案，该全局通配配置将瞬间转化为致命的跨域凭证窃取高危漏洞。

### 1.2 拓扑特点与约束
- **Docker Compose + Nginx 反向代理拓扑**：生产环境下，平台通过 `docker-compose.yml` 统一部署，外部流量由 `volur-nginx`（80/443）统一接管，前端 Vue 3 静态页面与 `/api/` 路由同域反代至 `volur-app:8080`。因此，**自身合法前端在生产环境下属于 100% 同源请求（Same-Origin），根本不需要后端开启通配 CORS**；
- **域名动态不确定性**：平台支持私有化开箱即用部署，用户既可能直接通过 IP 地址（如 `http://192.168.1.100`）访问，也可能绑定自建域名（如 `https://ctf.domain.com`），无法在静态配置中提前写死固定的单一定制域名。

---

## 2. 改动点与方案设计 (方案 A)

采用**自适应同 Host 动态校验与最小权限凭据收敛策略（Adaptive Host-Matching CORS Policy）**：

### 2.1 动态自适应 Host 提取与比对机制
1. **非跨域直接放行**：当请求不包含 `Origin` 请求头（常规同源 GET / 页面访问）时，直接返回 `null`，不生成多余的 CORS 响应头，由浏览器默认同源机制处理；
2. **双端 Host 提取与对齐**：
   - 从 `Origin` 解析出请求发起方的 `originHost`（剔除协议与端口，安全支持 IPv4/IPv6 与 null origin 防护）；
   - 从 `X-Forwarded-Host`、`Host` 请求头或 `getServerName()` 解析当前服务实际访问的目标主机名 `requestHost`；
3. **智能白名单判定规则**：
   - **本地开发环境**：放行 `localhost`、`127.0.0.1`、`::1`；
   - **自适应生产环境**：当 `originHost.equalsIgnoreCase(requestHost)` 时放行，无论用户使用公网 IP、内网 IP 还是绑定自定义域名，均能自动识别并允许同 Host 跨域；
   - **可扩展性支持**：预留 `@Value("${volur.security.cors.allowed-origins:}")` 属性，支持管理员在统一门户或特殊跨域场景下额外追加受信任域名；
4. **收敛凭证与授权头**：
   - 明确将 `setAllowCredentials(false)`，严禁对外暴露跨域 Cookie / 凭证交互通道；
   - 放行成功时仅回显经校验的具体 `Origin` 字符串，杜绝全局 `*` 通配模式。

---

## 3. 修改文件与代码审计

### 3.1 架构下沉与通用工具类重命名重构：[`RequestUtil.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/util/RequestUtil.java)
- 将原有 `RequestHolder` 重构并规范化重命名为 `RequestUtil`，符合平台 `*Util` 命名规范（与 `JwtUtil`、`PageSortUtil`、`PageTokenUtil` 对齐）；
- 将 `getOrigin`、`extractOriginHost`、`getRequestHost`、`extractRequestHost`、`extractHost` 与 `isOriginAllowed` 沉淀为平台级公用静态方法；
- 全局清理并移除 `RequestHolder.java`，将全平台模块中全部调用点（`ApiResponseEntity`、`RiskIpFilter`、`AuthServiceImpl`、`AdminRiskController`、`RiskEvaluationEngine`、`CaptchaSDKImpl`、`AdminEmailServiceImpl`、`CaptchaServiceImpl`、`EmailServiceImpl`）统一原子化重构迁移至 `RequestUtil`。

### 3.2 配置类瘦身：[`SecurityConfig.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java)
- 重构 `corsConfigurationSource()` Bean，直接委托调用 `RequestUtil.isOriginAllowed(origin, request, additionalAllowedOrigins)`；
- 移除所有私有解析辅助方法，严格遵守 Thin Configuration 设计原则。

### 3.3 自动化测试用例
- **[`RequestUtilTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/security/RequestUtilTest.java)**：测试 `extractOriginHost`、`extractHost`、`extractRequestHost` 与 `isOriginAllowed` 的纯函数正确性；
- **[`RequestUtilIpTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/security/RequestUtilIpTest.java)**：测试客户端 IP 提取与多级反代穿透回溯逻辑；
- **[`SecurityConfigCorsTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/security/SecurityConfigCorsTest.java)**：全链路验证 Spring Security CORS 过滤链路的放行与阻断逻辑。

---

## 4. 影响范围与验证结果

1. **影响范围**：
   - 彻底封死公网第三方恶意站点跨域探测平台数据的安全风险；
   - 用户无论是直连 IP 还是绑定任意域名，均无需修改任何额外配置即可正常开箱即用；
   - 彻底移除了有歧义的 `RequestHolder`，全平台请求工具类语义高度统一。
2. **测试验证**：
   - `mvn clean test` 全平台 12 个模块全量单元测试一次性全部通过（BUILD SUCCESS，0 Failures, 0 Errors）。
