# 系统环境变量硬编码默认值治理与 Zero-Mock 凭据安全审计

## 1. 变更背景与演进动机

在现代容器化与微服务交付架构中，所有与基础设施连接及安全凭据相关的核心参数必须保持极简、精确并具备单一事实源（Single Source of Truth）。

此前系统中存在多处历史兼容遗留的**假值兜底（Mock/Fallback）、硬编码弱凭据以及完全零引用的幽灵死配置（Dead Config）**：
1. **公开硬编码 JWT 签名密钥**：`application.yml` 与 `SecurityPlaceholderResolver.java` 中硬编码了公开密钥字面量 `VolurLitePlatformSecretKeyForJwtAuthentication2026SecureString!` 作为 `${VOLUR_JWT_SECRET}` 的兜底默认值。
2. **基础设施主机名与容器编排不一致**：`application.yml` 中数据源 URL 默认回退为 `${VOLUR_MYSQL_HOST:volur-db}`，而 `docker-compose.yml` 中 MySQL 编排容器名实际为 `volur-mysql`；Redis 默认回退为 `localhost`。
3. **Redis 弱密码隐式兜底**：`application.yml`、`docker-compose.yml` 及 `package.sh` 中保留了 `${VOLUR_REDIS_PASSWORD:volur_redis_secret_2026}`。
4. **完全零引用的容器运行时死配置**：
   - `VOLUR_CONTAINER_DOCKER_HOST` 与 `VOLUR_K3S_MASTER_URL`：在全项目 Java 代码中**没有任何 Bean 或方法读取** `volur.container.docker-host` 或 `volur.k3s.master-url`。真实的运行时 API 地址由系统动态设置 `system.container.docker_swarm.api_url` 与 `system.container.k3s.master_url`（持久化于数据库）驱动，K3s 集群配置则直接挂载 `/etc/rancher/k3s/k3s.yaml` 卷；
   - `volur.container` 下的 `default-timeout-minutes`、`max-instances-per-user`、`network-name` 同样全项目零引用。
5. **容器网络内固定端口与宿主机端口混淆**：
   - `VOLUR_MYSQL_PORT` 与 `VOLUR_REDIS_PORT`：`docker-compose.yml` 中 `volur-app` 根本未接收这两个环境变量，在 Docker bridge 网络内部，MySQL 容器端口恒为 `3306`，Redis 容器端口恒为 `6379`。在 `.env.example` 中声明 `VOLUR_MYSQL_PORT=13306` 极易误导使用者认为应用连接的是宿主机端口；而在 `application.yml` 中使用 `${VOLUR_MYSQL_PORT:3306}` 纯属冗余间接层。
6. **历史技术栈遗留变量残留**：
   - `VOLUR_MONGO_HOST`、`ES_JAVA_OPTS` 与 `KAFKA_CLUSTER_ID` 等在 `.env` 与 `.env.example` 中残留多年，项目早已全量收敛于 MySQL + Redis。

---

## 2. 详细改动清单

### 2.1 核心配置治理 (`application.yml` & `application-dev.yml`)
- **JWT 签名密钥 (`volur.jwt.secret`)**：
  调整为 `"${VOLUR_JWT_SECRET}"`，彻底剔除硬编码默认密钥。
- **数据源与缓存连接端口固化**：
  - MySQL 数据源 URL 调整为字面量端口：`jdbc:mysql://${VOLUR_MYSQL_HOST}:3306/volur_db?...`，彻底移除 `${VOLUR_MYSQL_PORT:3306}` 与 `volur-db` 回退；
  - Redis 端口固化为字面量 `port: 6379`，密码声明为强制外部注入的 `${VOLUR_REDIS_PASSWORD}`。
- **全面剔除零引用死配置**：
  - 从 `application.yml` 与 `application-dev.yml` 中彻底删除 `volur.container.docker-host`、`default-timeout-minutes`、`max-instances-per-user`、`network-name` 以及 `volur.k3s.master-url`；
  - 彻底删除 `spring.mongodb` 死配置段。

### 2.2 领域组件与代码注解清理 (`volur-container`)
- **防作弊摘要解析器 (`SecurityPlaceholderResolver.java`)**：
  将 `@Value("${volur.jwt.secret:VolurLitePlatformSecretKeyForJwtAuthentication2026SecureString!}")` 调整为 `@Value("${volur.jwt.secret}")`，并清理类中解释性注释。
- **镜像仓库客户端 (`DockerRegistryClient.java`)**：
  清理嵌套 `${...:${...:...}}` 冗余回退反模式，直接绑定统一配置属性。

### 2.3 容器编排与发布脚本治理 (`docker-compose.yml` & `package.sh`)
- **`docker-compose.yml`**：
  - 移除无任何 Java 代码消费的 `- VOLUR_CONTAINER_DOCKER_HOST=unix:///var/run/docker.sock`；
  - 将 Redis 密码由 `${VOLUR_REDIS_PASSWORD:-volur_redis_secret_2026}` 改为严格必填的 `${VOLUR_REDIS_PASSWORD}`。
- **`package.sh`**：
  - 移除 `- VOLUR_CONTAINER_DOCKER_HOST`；
  - 清理所有的默认弱口令兜底（`${VOLUR_MYSQL_PASSWORD}`、`${VOLUR_REDIS_PASSWORD}`、`${VOLUR_JWT_SECRET}`）。

### 2.4 环境变量极致精简 (`.env` & `.env.example`)
将 `.env` 与 `.env.example` 精简至**仅保留由 `docker-compose.yml` 实际消费的最小必须变量集**：
```env
SPRING_PROFILES_ACTIVE=prod
VOLUR_MYSQL_PASSWORD=root123456
VOLUR_REDIS_PASSWORD=change_this_redis_password
VOLUR_JWT_SECRET=change_this_jwt_secret_must_be_at_least_32_characters_long
REGISTRY_PORT=5001
```
彻底清除了以下 7 个无效/误导变量：
- `VOLUR_MYSQL_HOST` / `VOLUR_REDIS_HOST`（Compose 已写死服务名，配置无效）
- `VOLUR_CONTAINER_DOCKER_HOST` / `VOLUR_K3S_MASTER_URL`（代码零引用死配置）
- `VOLUR_MYSQL_PORT` / `VOLUR_REDIS_PORT`（内部端口恒定 3306/6379，仅调试 override 需暴露）
- `VOLUR_MONGO_HOST` / `ES_JAVA_OPTS` / `KAFKA_CLUSTER_ID`（完全未引入的技术栈残留）

---

## 3. 安全与工程影响分析

1. **配置语义绝对清晰**：
   `.env.example` 中的每一个变量均与 `docker-compose.yml` 形成 100% 对应闭环，杜绝了配置漂移与假象。
2. **纯净单一真实源**：
   容器内部服务通信完全由容器服务网络及标准端口保障，消除了中间层参数传递与解析负担。
3. **安全防护快速失败 (Fail-Fast)**：
   任何敏感凭据一旦缺失立即在容器启动期暴露中断，杜绝静默回退弱密钥。
