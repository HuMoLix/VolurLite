# Redis 连接认证失败深度排查与配置差异分析

## 1. 现象背景与问题描述
在本地开发与调试环境下，应用程序或数据库客户端连接本地运行的 Redis 服务时，提示密码错误（认证失败）：
- 报错表现一（Spring Boot / Lettuce / ACL 认证）：
  `AUTH failed: WRONGPASS invalid username-password pair or user is disabled.`
- 报错表现二（常规 Redis 客户端 / 单密码认证）：
  `AUTH failed: ERR AUTH <password> called without any password configured for the default user. Are you sure your configuration is correct?`

---

## 2. 核心原因深度排查

### 2.1 项目配置基准 (`.env` 与 `application.yml`)
1. **`.env` 环境变量定义**：
   ```properties
   VOLUR_REDIS_HOST=localhost
   VOLUR_REDIS_PASSWORD=volur_redis_secret_2026
   ```
2. **Spring Boot 后端配置 (`application.yml`)**：
   ```yaml
   spring:
     data:
       redis:
         host: ${VOLUR_REDIS_HOST}
         port: 6379
         username: volur
         password: ${VOLUR_REDIS_PASSWORD}
         database: 0
   ```
3. **架构安全规范**：
   在本项目设计中（参考 `docker-compose.yml` 与最小特权规范），Redis 采用了 Redis 6+ 的 ACL 用户体系：
   - 禁用了 `default` 缺省用户（`--user default off`）；
   - 显式创建了专用业务用户 `volur`（`--user volur on >${VOLUR_REDIS_PASSWORD} ...`），并仅赋予业务必要权限。

### 2.2 本地运行的 `redis-server` 真实状态
通过诊断命令对当前 macOS 系统下运行的 Redis 进程进行探测：
1. **进程状态**：
   - 进程为 Homebrew 安装的 Redis 7.2.6（PID 26386，监听 `*:6379`）；
   - 该进程为直接通过终端执行裸 `redis-server` 启动，未指定任何带密码或 ACL 规则的 `.conf` 配置文件。
2. **ACL 与密码配置检测**：
   - `redis-cli acl list`：仅存在 `user default on nopass sanitize-payload ~* &* +@all`；
   - `redis-cli config get requirepass`：返回空字符串 `""`；
   - **系统中完全不存在 `volur` 用户**。

### 2.3 认证冲突机制剖析
| 场景 | 连接发起方所带参数 | Redis 服务端行为 | 报错信息 |
| :--- | :--- | :--- | :--- |
| **Spring Boot 启动** | `username: volur`<br>`password: volur_redis_secret_2026` | 查找 `volur` 用户，发现不存在该用户 | `WRONGPASS invalid username-password pair or user is disabled` |
| **普通客户端单密码认证** | `AUTH volur_redis_secret_2026`<br>(隐式使用 `default` 用户) | `default` 用户处于 `nopass` 状态，拒绝任何密码校验 | `ERR AUTH <password> called without any password configured for the default user` |

---

## 3. 修复方案与操作建议

### 3.1 运行时热修复（当前实例已执行验证）
已针对当前正在运行的 Redis 实例通过 `redis-cli` 执行动态注入：
```bash
# 1. 创建 volur 用户并配置对应密码与读写权限（满足 Spring Boot ACL 需求）
redis-cli acl setuser volur on ">volur_redis_secret_2026" "~*" "&*" "+@all"

# 2. 为 default 用户配置 requirepass（兼容普通客户端/可视化工具单密码连接）
redis-cli config set requirepass volur_redis_secret_2026
```
验证结果：双向认证均已成功返回 `PONG`。

### 3.2 长期持久化配置建议

#### 方案 A：配置本地 Homebrew Redis（推荐本地单机常驻开发）
编辑 Homebrew 配置文件 `/opt/homebrew/etc/redis.conf`：
```conf
# 兼容普通单密码认证
requirepass volur_redis_secret_2026

# 支持 Spring Boot ACL 用户
user volur on >volur_redis_secret_2026 ~* &* +@all
```
随后重启 Homebrew Redis 服务：
```bash
brew services restart redis
```

#### 方案 B：使用项目自带 Docker Compose 编排（与生产/预发严格一致）
启动 Docker Desktop 后运行：
```bash
docker compose up -d volur-redis
```
该容器会自动加载环境变量并初始化具备完整权限限制的 `volur` 用户。
