# Redis 固定用户绑定与 MySQL 密码必填强制治理文档

## 1. 变更背景与演进动机

1. **统一用户治理收敛**：此前数据源层已完成 `VOLUR_MYSQL_USER` 的清理并强制固化为 `volur` 用户；Redis 侧仍保留 `${VOLUR_REDIS_USER:volur}` 占位符并在容器环境变量中显式传递 `VOLUR_REDIS_USER=volur`，存在冗余的间接层。应直接在基底配置中固定 `username: volur`，消除无效的环境变量传递。
2. **严防弱口令与强制凭据注入（Fail-Fast & Zero-Mock 原则）**：生产配置中存在 `${VOLUR_MYSQL_PASSWORD:-volur_mysql_secret_2026}` 这一历史兼容默认值范式。当外部环境未定义 `VOLUR_MYSQL_PASSWORD` 时，容器会隐式兜底使用预设密钥，存在严重的生产安全隐患。必须彻底取消保留兜底值，强要求外部显式传入 `VOLUR_MYSQL_PASSWORD`。

---

## 2. 施工与修改详细清单

### 2.1 Redis 用户名硬编码固化
- **基底数据配置 (`application.yml`)**：
  将 `spring.data.redis.username` 由 `${VOLUR_REDIS_USER:volur}` 改为字面量 `volur`，彻底固化业务账号；
- **容器与打包环境变量精简**：
  - [`docker-compose.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/docker-compose.yml)：从 `volur-app` 的 `environment` 中剔除 `- VOLUR_REDIS_USER=volur`；
  - [`package.sh`](file:///Users/humolix/Documents/Dev/Volur-Lite/package.sh)：从应用服务模板中剔除 `- VOLUR_REDIS_USER=volur`。

### 2.2 MySQL 密码强制注入（消除兜底默认值）
- **容器运行配置 (`docker-compose.yml`)**：
  - 将 `volur-app` 环境变量由 `VOLUR_MYSQL_PASSWORD=${VOLUR_MYSQL_PASSWORD:-volur_mysql_secret_2026}` 调整为 `VOLUR_MYSQL_PASSWORD=${VOLUR_MYSQL_PASSWORD}`；
- **打包套件模板 (`package.sh`)**：
  - 清理 MySQL 初始环境变量 `MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD`、健康检查 `healthcheck` 以及 `volur-app` 中所有的 `${VOLUR_MYSQL_PASSWORD:-volur_mysql_secret_2026}` 默认兜底，全部统一改为 `${VOLUR_MYSQL_PASSWORD}`。

---

## 3. 架构分析：`VOLUR_CONTAINER_REGISTRY_ENABLED` 机制与作用

在 Volur Lite 容器与集群子系统架构中，`VOLUR_CONTAINER_REGISTRY_ENABLED`（映射为 `volur.container.registry.enabled`）是控制**私有镜像仓库（Docker Registry V2）生命周期与调度集成**的核心主控开关：

1. **驱动底层组件**：
   在 [`DockerRegistryClient.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/client/DockerRegistryClient.java) 中注入该参数，作为 `isRegistryEnabled()` 与 `isAvailable()` 的前置守卫：
   ```java
   public boolean isAvailable() {
       if (!isRegistryEnabled()) {
           return false;
       }
       String resolved = resolveInternalRegistryUrl();
       return checkRegistryUrl(resolved);
   }
   ```
2. **靶机镜像管理 (Admin Image Lifecycle)**：
   - 当 `enabled: true` 时，管理员在后台导入 `.tar` 镜像包或构建 CTF 靶机镜像后，平台会自动将镜像推送到内部私有仓库（`volur-registry:5000`），并提供镜像 Catalog、Tag 以及 Manifest 层级元数据的实时同步与清理能力；
   - 当 `enabled: false` 时，平台仅在本地 Docker Daemon 内部保留镜像，不会触发跨网络推送与镜像仓库探测。
3. **K3s/Kubernetes 分布式集群靶机拉取 (Cluster Runtime Strategy)**：
   - 在 K3s 分布式靶机场景下，多个 Worker 节点无法直接共享管理节点的本地 Docker 存储；
   - 当 `dockerRegistryClient.isAvailable()` 为 `true` 时，[`K3sRuntimeStrategy`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java) 在动态生成 Pod 声明时，会自动将题目镜像引用重定向至私有仓库地址（如 `127.0.0.1:5001/...` 或内网 Registry Host），从而使集群各节点能够秒级内网拉起题目靶机，实现离线/内网环境下的快速分发；
   - 若关闭该开关，系统将直接使用原镜像 Tag 向外部公开仓库（如 Docker Hub、GHCR）发起拉取，适用于无需本地私有 Registry 的单机轻量化测试。
