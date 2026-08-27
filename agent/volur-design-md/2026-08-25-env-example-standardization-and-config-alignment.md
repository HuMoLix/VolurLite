# 环境变量配置模版精简与系统动态设置治理规范

## 1. 变更背景与目标

为进一步降低系统配置复杂度、清晰界定“基础设施环境变量”与“动态业务策略”的职责边界，本次对 [`.env.example`](file:///H:/HuMoLix/github/Volur-Lite/.env.example)、[`application.yml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application.yml)、[`application-dev.yml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application-dev.yml) 及 [`docker-compose.yml`](file:///H:/HuMoLix/github/Volur-Lite/docker-compose.yml) 进行了精简与收敛治理：

1. **澄清 Context Path 与规范路由职责**：移除冗余的 `server.servlet.context-path=/api`，统一由控制器 `@RequestMapping("/api/v1/...")` 提供清晰直观的 API 路由；
2. **基础设施环境变量精简化**：
   - **MySQL**：仅保留 `VOLUR_MYSQL_HOST`、`VOLUR_MYSQL_USER`、`VOLUR_MYSQL_PASSWORD`（端口 3306、数据库 `volur_db` 硬编码/默认内置）；
   - **Redis**：仅保留 `VOLUR_REDIS_HOST`、`VOLUR_REDIS_PASSWORD`、`VOLUR_REDIS_DATABASE`（端口 6379 硬编码/默认内置）；
   - **MongoDB**：仅保留 `VOLUR_MONGO_HOST`（端口 27017、数据库 `volur_db` 硬编码/默认内置）；
   - **JWT**：仅保留核心凭证与时效参数 `VOLUR_JWT_SECRET`、`VOLUR_JWT_EXPIRE_SECONDS`、`VOLUR_JWT_REFRESH_EXPIRATION_MS`、`VOLUR_JWT_REMEMBER_ME_REFRESH_EXPIRATION_MS`（Header 与 Prefix 内部硬编码）；
3. **分布式雪花 ID 生成器硬编码**：取消外部 env 注入，使用类内稳定默认值（`enableRedisWorker=true`、`workerId=0`、`workerTtlSeconds=30`）；
4. **动态业务策略与安全配置统一收敛至 MySQL 系统设置**：
   - 动态 Flag 格式（`FLAG_PREFIX`、`FLAG_SUFFIX`）接入 `SystemSettingService` 动态读取；
   - 用户注册开关、邮箱验证强制开关、邮箱后缀白名单、默认角色、最大登录失败次数、前端访问 URL；
   - 验证码开关（`ENABLE_CAPTCHA`）、Cloudflare Turnstile Key、邮件 SMTP / 阿里云 DirectMail 配置全部由数据库系统设置中心统一持久化与缓存管理，彻底杜绝从环境变量散落配置。

---

## 2. 详细改动点

### 2.1 根目录 [`.env.example`](file:///H:/HuMoLix/github/Volur-Lite/.env.example) 精简
精简后的环境变量仅包含核心基础设施连接与敏感身份令牌凭证：
- **基础环境**：`SPRING_PROFILES_ACTIVE=dev`, `SERVER_PORT=8080`, `NGINX_HTTP_PORT=80`
- **MySQL**：`VOLUR_MYSQL_HOST=localhost`, `VOLUR_MYSQL_USER=root`, `VOLUR_MYSQL_PASSWORD=root123456`
- **Redis**：`VOLUR_REDIS_HOST=localhost`, `VOLUR_REDIS_PASSWORD=volur_redis_secret_2026`, `VOLUR_REDIS_DATABASE=0`
- **MongoDB**：`VOLUR_MONGO_HOST=localhost`
- **JWT**：`VOLUR_JWT_SECRET`, `VOLUR_JWT_EXPIRE_SECONDS=86400`, `VOLUR_JWT_REFRESH_EXPIRATION_MS=604800000`, `VOLUR_JWT_REMEMBER_ME_REFRESH_EXPIRATION_MS=2592000000`
- **CTF 容器调度**：`VOLUR_CONTAINER_DOCKER_HOST`, `VOLUR_K3S_NAMESPACE`, `VOLUR_K3S_PUBLIC_HOST`, `VOLUR_K3S_KUBECONFIG_PATH`, `VOLUR_K3S_MASTER_URL`
- **可选大数据**：`ES_JAVA_OPTS`, `KAFKA_CLUSTER_ID`

### 2.2 [`CtfInstanceServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/impl/CtfInstanceServiceImpl.java) 动态 Flag 支持
- 引入 [`SystemSettingService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/service/SystemSettingService.java) 依赖；
- 动态靶机生成 Flag 时，优先读取系统设置中的 `FLAG_PREFIX`（默认 `flag{`）与 `FLAG_SUFFIX`（默认 `}`），实现免重启在线热修改。

### 2.3 Spring Boot 配置与 Docker Compose 对齐
- [`application.yml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application.yml)：移除 `context-path` 与 `snowflake` / `challenge` 的冗余 env 映射；
- [`application-dev.yml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application-dev.yml)：数据库与缓存仅对齐 `VOLUR_MYSQL_HOST`、`VOLUR_REDIS_HOST`、`VOLUR_MONGO_HOST` 等必要变量；
- [`docker-compose.yml`](file:///H:/HuMoLix/github/Volur-Lite/docker-compose.yml)：数据库名为固定 `volur_db`，清理了无效的容器依赖与命名卷。

---

## 3. 验证与影响范围

- **编译与构建验证**：执行 `mvn clean compile -DskipTests`，全工程 12 个子模块全部成功通过编译。
- **架构解耦性**：彻底明确了环境变量（部署期基础设施连接）与系统配置（运行期业务热修改）的分工边界。
