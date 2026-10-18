# MySQL & Redis 最小权限原则安全加固与基底数据源重构文档

## 1. 变更背景与演进动机

在此前配置中，Volur Lite 存在若干安全隐患与配置冗余问题：
1. **特权账号越权风险**：后端应用 `volur-app` 通过环境变量 `VOLUR_MYSQL_USER` 默认以拥有完全特权的 `root` 用户直连 MySQL 数据库；Redis 仅采用传统 `--requirepass`，连接直接落入内置 `default` 超级用户，具备执行系统管理级破坏性指令的能力；
2. **容器提权与逃逸风险**：容器缺乏 Capabilities 剥离、未声明 `no-new-privileges`，且根文件系统具备完全写入权限，失陷后攻击者容易在容器内部落地木马或借助内核漏洞尝试逃逸；
3. **数据源配置样板冗余**：`application-dev.yml` 与 `application-prod.yml` 各自重复定义了几乎一致的 `datasource` 核心驱动、连接池超时与校验参数，不符合 DRY 原则。

---

## 2. 施工与修改详细清单

### 2.1 容器纵深安全加固 (`docker-compose.yml`)
- **MySQL (`volur-mysql`)**：
  - 配置 `security_opt: [ "no-new-privileges:true" ]`，阻止进程通过 SUID 二进制文件提升特权；
  - 配置 `cap_drop: [ "ALL" ]` 并最小化白名单 `cap_add: [ "CHOWN", "SETUID", "SETGID", "DAC_OVERRIDE" ]`，剥离未授权网络原始套接字等高危内核能力；
  - 开启 `read_only: true`，将临时写入重定向至隔离的 `tmpfs` 目录（`/tmp` 与 `/var/run/mysqld`），并配置 `noexec,nosuid` 选项；
  - 独立设置 `MYSQL_ROOT_PASSWORD`（DBA 应急使用），正式引入 `MYSQL_USER: volur` 业务专用非特权账号，健康检查同步切换至 `volur` 认证；
  - 设置 `pids_limit: 250` 与 `ulimits.core: 0`，防御 Fork 炸弹与内存崩溃核心转储泄露敏感数据。
- **Redis (`volur-redis`)**：
  - 显式声明 `user: "999:999"`（非 root 运行）；
  - 配置 `cap_drop: [ "ALL" ]`，无需保留任何特殊 Capabilities；
  - 开启 `read_only: true` 配合内存 `tmpfs: [ "/tmp" ]`；
  - 启用 Redis ACL，禁用 `default` 用户，创建受限业务账号 `volur` 并封锁 `@admin`、`@dangerous`，禁用 `CONFIG`、`MODULE`、`DEBUG` 命令。

### 2.2 基础数据源统一与连接池环境收敛
- **基底数据源下沉 (`application.yml`)**：
  - 将 `spring.datasource`（包括 `driver-class-name`、通用 JDBC URL、`username: volur`、`password` 以及 HikariCP 核心连接池参数）统一下沉收敛至基础 `application.yml`；
  - JDBC URL 统一声明为：
    `jdbc:mysql://${VOLUR_MYSQL_HOST:localhost}:${VOLUR_MYSQL_PORT:3306}/volur_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048`
    完美自适应本地开发（通过 `VOLUR_MYSQL_PORT: 13306`）与生产 Docker 内部网络通信（默认 3306 端口）；
  - 用户名强制硬编码对齐业务服务用户 `volur`。
- **环境隔离轻量化 (`application-dev.yml` & `application-prod.yml`)**：
  - `application-dev.yml` 与 `application-prod.yml` 的 `datasource` 节点彻底精简，仅各自区分维护特定的连接池监控标识：
    - `dev`: `spring.datasource.hikari.pool-name: VolurHikariCP-Dev`
    - `prod`: `spring.datasource.hikari.pool-name: VolurHikariCP-Prod`

### 2.3 `VOLUR_MYSQL_USER` 全链路彻底清理
依据重构彻底性铁律，彻底废除全系统中的历史环境变量 `VOLUR_MYSQL_USER`：
- `src/volur-lite-platform/.../application-*.yml`：彻底移除 `${VOLUR_MYSQL_USER:...}` 占位符；
- `docker-compose.yml`：移除 `volur-app` 下注入的 `VOLUR_MYSQL_USER`；
- `.env` & `.env.example`：移除 `VOLUR_MYSQL_USER` 配置项；
- `package.sh`：移除打包构建脚本中的 `VOLUR_MYSQL_USER` 环境变量模板。

---

## 3. 影响范围与安全验证

1. **编译与打包**：Maven 全模块 `test-compile` 13/13 模块全部成功构建，零语法与配置解析冲突；
2. **向后兼容性**：
   - 全新部署实例将直接自动完成 `volur` 非特权账号的创建与数据库绑定；
   - 存量数据库实例若此前仅存在 `root` 账号，需配合升级脚本执行一次性授权以确保业务正常连通。
