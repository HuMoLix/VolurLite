# Docker Compose 构建与全栈服务启动失败根因分析及修复审计报告

## 1. 问题背景与现象描述

在开发环境中执行 `docker-compose up -d --build` 时，出现容器编排失败，具体报错表现为：
```text
dependency failed to start: container volur-redis is unhealthy
```
由于核心业务容器 `volur-app` 配置了强依赖：
```yaml
depends_on:
  volur-mysql:
    condition: service_healthy
  volur-redis:
    condition: service_healthy
```
当 `volur-redis` 处于异常状态无法通过健康检查时，Docker Compose 直接阻断了依赖容器 `volur-app` 与 `volur-nginx` 的后续启动与运行。

深入排查后，发现并修复了全链路中阻碍全栈服务启动的三个连锁致命问题。

---

## 2. 根因剖析与详细排查清单

### 2.1 第一主因：Redis 8 架构下 `--rename-command CONFIG ""` 引发段错误崩溃 (SIGSEGV / Exit Code 139)
- **故障排查**：
  查看 `volur-redis` 运行状态与错误日志：
  ```text
  volur-redis Restarting (139)
  1:M 18 Oct 2026 08:25:07.683 * Module 'bf' loaded from /usr/local/lib/redis/modules//redisbloom.so
  1:M 18 Oct 2026 08:25:07.685 # Redis 8.6.2 crashed by signal: 11, si_code: 1
  1:M 18 Oct 2026 08:25:07.685 # Accessing address: 0x24
  ```
- **代码级根因推导**：
  1. 当前基础镜像采用 `redis:8.6.2-alpine`。Redis 8 官方镜像内置默认自动加载各类核心扩展模块（RediSearch、RedisBloom、RedisJSON、RedisTimeSeries 等）；
  2. 在 `docker-compose.yml` 及 `package.sh` 的 `volur-redis` 启动参数中配置了：
     ```yaml
     - --rename-command
     - CONFIG
     - ""
     ```
  3. 当 RediSearch 模块在启动时尝试通过 Redis 模块 API 注册系统配置变更监听（`RedisModule_SubscribeToConfigChanges`）时，由于 `CONFIG` 命令已被重命名禁用（命令注册表中对应指针置空或移除），RediSearch 模块在访问底层 `CONFIG` 命令结构体时缺少非空断言，直接解引用偏移地址 `0x24`（36 字节偏移量），触发了空指针异常与内核内存访问违规（`signal: 11` 对应 `SIGSEGV`，进程以退出码 `139` 崩溃）；
  4. 进程死循环重启，导致配置的 `redis-cli ping` 健康检查不断超时超时，最终向 Docker Compose 抛出 `unhealthy` 异常并阻断流程。
- **治理策略**：
  Redis 官方自 Redis 6 起已明确将 `--rename-command` 标记为过时（Deprecated），并强烈推荐通过 Redis ACL 原生机制实施最小权限隔离。
  当前配置中已配置完善的 ACL 规则：
  ```yaml
  - --user
  - default
  - "off"
  - --user
  - volur
  - "on"
  - ">${VOLUR_REDIS_PASSWORD}"
  - "+@all"
  - "-@admin"
  - "-@dangerous"
  ```
  内置 `default` 账号已被关闭，业务账号 `volur` 显式排除了 `-@admin` 与 `-@dangerous` 分类，本身已完全不具备执行 `CONFIG`、`MODULE`、`DEBUG` 等危险命令的权限。
  因此，直接剔除破坏性的 `--rename-command CONFIG ""`，不仅消除了 RediSearch 模块空指针段错误，同时完备保持了最小权限原则的安全隔离水位。

### 2.2 第二主因：Nginx 网关语法错误致使容器崩溃循环 (Exit Code 1)
- **故障排查**：
  修复 Redis 后推进启动，发现 `volur-nginx` 状态处于 `Restarting (1)`。查看日志：
  ```text
  [emerg] 1#1: unexpected ";" in /etc/nginx/nginx.conf:233
  nginx: [emerg] unexpected ";" in /etc/nginx/nginx.conf:233
  ```
- **根因推导**：
  在 `src/volur-lite-view-NEXT/nginx/conf/nginx.conf` 尾部预留的 HTTPS 配置段中，误使用了分号 `;`（如 `;     server { ... }`）替代井号 `#` 作为单行注释符。Nginx 词法解析器将行首分号识别为缺少前置指令的非法语法，导致 Nginx 容器无法启动。
- **治理策略**：
  将 `src/volur-lite-view-NEXT/nginx/conf/nginx.conf` 中第 233-266 行的 `;` 全部修正为符合 Nginx 规范的 `#` 注释符。

### 2.3 第三隐患：存量 MySQL 数据卷缺失 `volur` 业务账号导致 `volur-app` 认证拒绝
- **故障排查**：
  查看 `volur-app` 日志：
  ```text
  Caused by: java.sql.SQLException: Access denied for user 'volur'@'172.18.0.5' (using password: YES)
  Caused by: org.hibernate.HibernateException: Unable to determine Dialect without JDBC metadata
  ```
- **根因推导**：
  近期架构演进中，后端 `volur-app` 数据库连接由超级用户 `root` 切换为专用业务用户 `volur`，以践行最小特权安全防护。
  但在官方 MySQL Docker 镜像中，`MYSQL_USER` 和 `MYSQL_PASSWORD` 环境变量**仅在初始化全新数据目录（即 `/var/lib/mysql` 为空）时才会触发创建**。
  若本地或宿主机已存在此前生成的存量卷 `volur-mysql-data`，MySQL 容器启动时会跳过初始化流程，导致数据库内仅存在 `root`，不存在 `volur` 用户。
- **治理策略**：
  对存量数据库实例补齐 `volur` 用户及数据库授权：
  ```sql
  CREATE USER IF NOT EXISTS 'volur'@'%' IDENTIFIED BY '${VOLUR_MYSQL_PASSWORD}';
  GRANT ALL PRIVILEGES ON volur_db.* TO 'volur'@'%';
  FLUSH PRIVILEGES;
  ```

---

## 3. 修改文件与实施清单

1. [`docker-compose.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/docker-compose.yml)：
   - 从 `volur-redis` 的 `command` 参数列表中移除 `--rename-command CONFIG ""`；
2. [`package.sh`](file:///Users/humolix/Documents/Dev/Volur-Lite/package.sh)：
   - 同步从打包脚本模板中移除 `--rename-command CONFIG ""`；
3. [`src/volur-lite-view-NEXT/nginx/conf/nginx.conf`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/nginx/conf/nginx.conf)：
   - 将行首分号 `;` 规范化修正为 `#` 注释符；
4. `volur-mysql` 存量实例：
   - 为本地存量数据卷补齐并授权了 `volur` 用户。

---

## 4. 验证结论

重新执行构建与全栈启动验证：
```bash
docker-compose up -d --build
```
1. **全容器就绪状态**：
   - `volur-mysql`：Up (healthy)
   - `volur-redis`：Up (healthy)
   - `volur-registry`：Up
   - `volur-app`：Up (Tomcat 8080 正常就绪，Spring Boot 上下文耗时 ~8.5s 初始化完成)
   - `volur-nginx`：Up (80 / 443 端口正常监听)
2. **端到端网络通信验证**：
   - `curl -i http://localhost/` 返回 `200 OK`，成功响应前端单页应用；
   - `curl -i http://localhost/api/v1/system/settings/public` 成功经由 Nginx 反向代理至 `volur-app` 业务层，全链路验证通过。
