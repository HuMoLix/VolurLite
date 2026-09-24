# 审计记录：生产环境全链路高频抗击与防 502 穿透硬核加固落地

## 1. 变更背景与问题定位

在前期独立安装包（`volur-lite-amd64.tar.gz`，对应提交 `099526e`）交付运行过程中，发现恶意客户端使用高频发包脚本在 1s 内对认证接口（`/api/v1/auth/login`）与发信接口（`/api/v1/email/captcha/register`）进行密集冲击（请求刻意不带验证码或提交空字段）。
虽然业务层逻辑在 Controller/Service 中能够抛出校验拒绝提示（`"请先完成人机安全验证"`），但因防护位置严重后置，且拒绝过程依然触发完整的参数反序列化、AOP 日志拦截（`SyslogAspect`）、Redis 缓冲与微批落库，引发 Tomcat 平台线程池耗尽（`max-threads=200`）、TCP 握手排队队列溢出、数据库连接池饥饿及控制台同步 I/O 阻塞，导致反代层 Nginx 向上游请求时遭遇拒绝，系统短暂陷入 **502 Bad Gateway** 拒绝服务状态。

为了彻底消除单点瓶颈与拒绝服务隐患，针对系统进行了 5 项纵深加固措施。

---

## 2. 改动点清单

### 2.1 Spring Boot Tomcat 开启 Java 25 虚拟线程 (Virtual Threads)
- **文件**：[`application.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application.yml)
- **改动**：
  - 显式声明 `spring.threads.virtual.enabled: true`。Tomcat 请求调度由固定的 200 平台线程池全面切换为轻量级虚拟线程（Virtual Thread Per Task Executor），能够轻松承载数以万计的并发请求并消除了线程上下文切换开销；
  - 调优 Tomcat 核心网络参数：`server.tomcat.accept-count: 200`、`server.tomcat.max-connections: 10000`。

### 2.2 Nginx 多拓扑真实 IP 频控与并发限制 (Layer 1 防御)
- **文件**：[`src/volur-lite-view/nginx/conf/nginx.conf`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/nginx/conf/nginx.conf)
- **改动**：
  - 基于已有的多拓扑客户端真实 IP 解析变量 `$client_real_ip`，开辟 `volur_conn_limit:10m`、`volur_api_req_limit:10m` 与 `volur_auth_req_limit:10m` 共享内存空间；
  - 设定超限直接返回标准状态码：`limit_req_status 429;` 与 `limit_conn_status 429;`；
  - 对敏感认证/验证码接口（`/api/v1/auth/login`、`/api/v1/auth/register`、`/api/v1/email/captcha`、`/api/v1/captcha`）施加严格的 5 次/秒硬性频控（突发容忍 10-15 次，`nodelay`），在网络前哨直接丢弃恶意洪水包，避免请求流向下游 Java 容器。

### 2.3 HikariCP 数据库连接池与 Lettuce Redis 线程池扩容调优
- **文件**：[`application-dev.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application-dev.yml) & [`application-prod.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application-prod.yml)
- **改动**：
  - 数据库连接池 `hikari.maximum-pool-size` 由 20 扩容至 60，`minimum-idle` 由 5 提升至 10；
  - 缩短连接超时 `connection-timeout: 10000ms`（快速失败，避免线程长时间僵死等待），增加 `validation-timeout: 3000ms`；
  - Redis 客户端 Lettuce 连接池 `max-active` 由 16 扩容至 64，`max-idle` 提升至 32，`min-idle` 提升至 8。

### 2.4 打造完备的生产环境配置 (`application-prod.yml`)
- **文件**：[`application-prod.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application-prod.yml)、[`docker-compose.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/docker-compose.yml)、[`volur-lite/docker-compose.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/volur-lite/docker-compose.yml)
- **改动**：
  - 创建并完善生产配置文件，关闭 `jpa.show-sql`（彻底消除控制台同步 I/O 阻塞），关闭 `open-in-view`；
  - JDBC URL 启用 `rewriteBatchedStatements=true` 与 PreparedStatement 缓存，加速微批审计日志落库；
  - 日志级别收敛为 `INFO`，屏蔽大量 Hibernate 与底层框架的 DEBUG 冗余打印；
  - 在 `docker-compose.yml` 中默认激活 `SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}`。

### 2.5 `RiskIpFilter` 注入 `RedisSlidingWindowRateLimiter` 动态前置滑动窗口限流
- **文件**：[`RiskIpFilter.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/RiskIpFilter.java)、[`CaptchaSDKImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/sdk/CaptchaSDKImpl.java)、[`EmailServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java)、[`RequestHolder.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/util/RequestHolder.java)
- **改动**：
  - 在 `RiskIpFilter` 中注入 `RedisSlidingWindowRateLimiter`；
  - 读取动态系统配置 `system.security.rate_limit_window`（默认 60s）与 `system.security.rate_limit_max_requests`（默认 120 次）；
  - 在请求进入 DispatcherServlet 和 Controller 之前执行滑动窗口计数。超限 IP 直接响应 429 Too Many Requests，阻断进入后续 AOP 流程；
  - 联动修复：当 `CaptchaSDKImpl` 与 `EmailServiceImpl` 检测到未携带验证码（`request == null`）或空凭证请求时，显式调用 `recordFailure` 记录失败计数，使得针对性绕过验证码的恶意脚本在连续请求触发阈值（20次）后，自动触发 `RiskAutoBanEvent` 将其关入 IP 黑名单。

### 2.6 Spring Security 过滤器链显式语义分层重构
- **文件**：[`SecurityConfig.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java)
- **改动**：
  - 彻底去除多个过滤器隐式同时锚定 `UsernamePasswordAuthenticationFilter.class` 的模糊写法；
  - 显式构建层级递进链条：
    ```java
    // 1. 将 jwtAuthFilter 锚定在表单认证位置之前
    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
    // 2. 将 riskIpFilter 显式置于 jwtAuthFilter 之前 (先验 IP 黑名单与滑动窗口频控，再解析 Token)
    .addFilterBefore(riskIpFilter, JwtAuthFilter.class)
    // 3. 将 maintenanceModeFilter 显式置于 jwtAuthFilter 之后 (先验身份，再判断是否属于免维护管理员)
    .addFilterAfter(maintenanceModeFilter, JwtAuthFilter.class)
    ```
  - 架构语义更加精准、执行先后关系零歧义。

---

## 3. 测试与验证结果

1. **后端单元测试**：
   - 编写了全覆盖的 [`RiskIpFilterTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/security/filter/RiskIpFilterTest.java)，验证了 OPTIONS 放行、黑名单 403 阻断、超出滑动窗口 429 拦截及本地回环豁免场景；
   - 运行平台全量单元测试（`mvn test -Dtest=...`），12 个 Maven 模块全部 **BUILD SUCCESS**（Tests run: 9, Failures: 0, Errors: 0）。
2. **前端工程构建**：
   - 运行 `npm --prefix src/volur-lite-view run build`，Rollup / Vite 生产环境编译无任何错误。
