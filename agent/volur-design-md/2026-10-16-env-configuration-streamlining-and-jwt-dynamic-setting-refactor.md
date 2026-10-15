# 环境变量精炼与 JWT 时效平台动态化重构实施记录

## 1. 变更背景与设计目标
在系统演进过程中，原根目录环境配置文件 `.env.example` 和 `.env` 中混杂了大量业务运营策略参数、已由管理后台平台动态设置（`SystemSetting`）托管的冗余项，以及各组件固有的默认网络端口与库配置，导致部署模板臃肿，且参数职责边界模糊：
1. **JWT 生命周期参数**（`VOLUR_JWT_EXPIRE_SECONDS`、`VOLUR_JWT_REFRESH_EXPIRATION_MS`、`VOLUR_JWT_REMEMBER_ME_REFRESH_EXPIRATION_MS`）：属于业务运营与安全策略范畴，写死在环境变量中导致管理员无法在管理后台动态调优，且与底层敏感根密钥 `VOLUR_JWT_SECRET` 混杂。
2. **K3s 集群调度参数**（`VOLUR_K3S_NAMESPACE`、`VOLUR_K3S_KUBECONFIG_PATH`、`VOLUR_K3S_PUBLIC_HOST`）：平台早已通过 `SystemSettingKeys` 实现了数据库与缓存支持的统一动态配置，且 `VOLUR_K3S_PUBLIC_HOST` 本质是所有调度策略（Docker Swarm 与 K3s）通用的“动态靶机外部可访问主机”，带 `K3S_` 前缀具有误导性，在 `.env` 中继续声明属于重复冗余项。
3. **Redis 默认库**（`VOLUR_REDIS_DATABASE=0`）：Spring Data Redis 默认库即为 `0`，且 Volur 平台全系统采用基于键命名空间前缀（`vredis:<module>:<feature>:`）的数据隔离方案，显式暴露库序号为样板代码。
4. **服务端口参数**（`SERVER_PORT=8080`、`NGINX_HTTP_PORT=80`）：在生产容器编排网络中，后端容器不对宿主机直接暴露端口，仅在 Docker 内部网络监听 8080 并由 Nginx 反代，外部统一入口为 Nginx 标准 80/443 端口。

---

## 2. 核心改动点详述

### 2.1 JWT 时效迁移至平台动态设置（SystemSetting）
- **统一系统键名定义**（`SystemSettingKeys.java`）：
  - `system.auth.jwt_expire_seconds`（默认 `86400` 秒）
  - `system.auth.jwt_refresh_expiration_ms`（默认 `604800000` 毫秒，7天）
  - `system.auth.jwt_remember_me_refresh_expiration_ms`（默认 `2592000000` 毫秒，30天）
- **配置持久化与初始化**（`SystemSettingInitializer.java`）：
  - 在 `ensureDefaultSettings()` 中注册上述三项的默认值，分类归属为 `AUTH` 分组。
- **动态解析服务扩展**（`SystemSettingService.java` / `SystemSettingServiceImpl.java`）：
  - 接口扩展 `long getLong(String key, long defaultValue)` 方法，安全解析长整型毫秒/秒级配置，防止整型溢出。
- **鉴权工具类重构**（`JwtUtil.java` 与 `JwtUtilTest.java`）：
  - 注入 `SystemSettingService`，将原通过 `@Value` 静态注入的 Token 过期时间改为通过 `getExpireSeconds()`、`getRefreshTokenExpirationMs()`、`getRememberMeRefreshTokenExpirationMs()` 动态读取，实现管理员后台调优即时生效且具备严谨的内置兜底。
  - 同步更新单元测试 `JwtUtilTest.java`，采用 Mockito `lenient().when(...)` 保证单测覆盖完整且通过严格 Stubbing 检查。
- **配置文件瘦身**（`application.yml`）：
  - 移除 `volur.jwt` 下的三个时效参数环境变量绑定，仅保留核心安全私钥 `VOLUR_JWT_SECRET`。

### 2.2 K3s 配置精简与解耦
- **策略实现类重构**（`K3sRuntimeStrategy.java`）：
  - 移除 `@Value` 注入 `defaultNamespace`、`defaultPublicHost`、`defaultKubeconfigPath`；
  - 声明内置兜底常量 `DEFAULT_NAMESPACE`、`DEFAULT_PUBLIC_HOST`、`DEFAULT_KUBECONFIG_PATH`；
  - 全流程优先从 `SystemSettingKeys.CONTAINER_K3S_NAMESPACE`、`SystemSettingKeys.CONTAINER_K3S_KUBECONFIG_PATH` 及全局 `SystemSettingKeys.CONTAINER_PUBLIC_HOST` 动态读取，彻底解除对环境变量的依赖。
- **配置清理**（`application.yml`）：
  - 移除 `volur.k3s` 下对 `VOLUR_K3S_NAMESPACE`、`VOLUR_K3S_PUBLIC_HOST`、`VOLUR_K3S_KUBECONFIG_PATH` 的环境变量引用，仅保留集群 Master 地址 `VOLUR_K3S_MASTER_URL`。

### 2.3 Redis 数据库与应用端口清理
- **Redis 默认库**：
  - `application-dev.yml` 与 `application-prod.yml` 中的 `database: ${VOLUR_REDIS_DATABASE:0}` 统一固定为 `database: 0`。
- **服务器服务端口**：
  - `application.yml` 中的 `server.port: ${SERVER_PORT:8080}` 统一直接声明为 `8080`。

### 2.4 根配置模板 `.env.example` 与 `.env` 重构
- 彻底移除以下废弃与冗余项：
  - `SERVER_PORT=8080`
  - `NGINX_HTTP_PORT=80`
  - `VOLUR_REDIS_DATABASE=0`
  - `VOLUR_JWT_EXPIRE_SECONDS=86400`
  - `VOLUR_JWT_REFRESH_EXPIRATION_MS=604800000`
  - `VOLUR_JWT_REMEMBER_ME_REFRESH_EXPIRATION_MS=2592000000`
  - `VOLUR_K3S_NAMESPACE=volur-challenges`
  - `VOLUR_K3S_PUBLIC_HOST=127.0.0.1`
  - `VOLUR_K3S_KUBECONFIG_PATH=/etc/rancher/k3s/k3s.yaml`
- 模板聚焦为清晰的基础设施网络连接（MySQL / Redis / MongoDB / Docker / K3s）与根敏感私钥（`VOLUR_JWT_SECRET`）。

---

## 3. 影响范围与验证结果
- **后端各模块构建与测试**：
  - 执行 `mvn clean test`，全平台全部 13 个子模块构建成功，测试用例全部通过。
- **动态配置体系完备性**：
  - 管理员可在管理后台配置面板动态修改 JWT 访问令牌时效与刷新令牌时效，即时刷新生效，提升系统安全运维灵活性。
