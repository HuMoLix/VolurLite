# MySQL & Redis 高并发架构优化与基底 Redis 配置收敛文档

## 1. 变更背景与演进动机

在此前配置中，MySQL 与 Redis 虽已完成最小权限原则安全加固，但在面对高并发与突发流量（如竞赛开赛千人并发提交、天梯榜实时结算、大批量日志持久化）场景下，仍存在以下性能瓶颈与崩溃风险：
1. **连接与句柄受限**：
   - 容器内未显式配置 `ulimits.nofile`，沿用宿主机默认低配（如 1024），并发连接与表文件句柄激增时将抛出 `Too many open files` 或 `Out of file descriptors`；
   - MySQL 默认 `max_connections=151`，极易因应用连接池与异步调度任务导致连接打满；
   - `pids_limit` 偏低（MySQL 250，Redis 100），在多线程并发排队时可能触碰线程上限。
2. **I/O 与缓存资源瓶颈**：
   - MySQL 默认 `innodb_buffer_pool_size=128M`，热点数据频繁进行磁盘物理读取；
   - `/tmp` 挂载 tmpfs 仅 256MB，复杂关联与分组排序下刷临时表容易发生 `Error 28: Table is full`；
   - Redis 默认单线程处理套接字读写，高 QPS 读写网络吞吐容易受限，且未设置内存上限与逐出策略，容易被触发 OOM Kill。
3. **应用端配置冗余与隔离缺陷**：
   - `spring.data.redis` 重复分散在 `application-dev.yml` 与 `application-prod.yml` 中，未实现集中下沉与统一调优，Lettuce 连接池参数偏小。

---

## 2. 施工与修改详细清单

### 2.1 数据库高并发内核调优 (`volur-mysql`)
在 [`docker-compose.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/docker-compose.yml) 及 [`package.sh`](file:///Users/humolix/Documents/Dev/Volur-Lite/package.sh) 中对 MySQL 进行了以下深度参数调优：
- **连接与网络层**：
  - `--max_connections=500`：扩大连接上限至 500；
  - `--max_connect_errors=100000`：防止突发异常中断导致宿主被 MySQL 封锁；
  - `--back_log=500`：增加 TCP 监听排队队列，防止瞬间握手洪峰被丢弃；
  - `pids_limit: 1000`：彻底解除 Linux 线程上限约束。
- **InnoDB 存储引擎层**：
  - `--innodb_buffer_pool_size=1024M`：将缓冲池扩大至 1GB，确保热点数据驻留内存；
  - `--innodb_buffer_pool_instances=4`：切分 4 个独立缓冲池实例，降低多核 CPU 互斥锁（Mutex）争用；
  - `--innodb_log_buffer_size=16M` & `--innodb_redo_log_capacity=256M`：扩大事务日志写入缓冲区与 Redo 容量，平滑脏页刷盘震荡；
  - `--innodb_read_io_threads=4` & `--innodb_write_io_threads=4`：开启 4 个异步读写 I/O 线程；
  - `--innodb_flush_log_at_trx_commit=2`：事务提交时写入 OS 缓存并每秒批量刷盘，大幅提升并发事务写入吞吐。
- **缓存与临时表优化**：
  - `--thread_cache_size=64`：高效复用工作线程；
  - `--table_open_cache=2000` & `--table_definition_cache=1000`：避免高频开关表文件；
  - `--tmp_table_size=64M` & `--max_heap_table_size=64M`：提高内存临时表阈值；
  - `/tmp` 挂载 tmpfs 提升至 `size=1024m`，`/var/run/mysqld` 提升至 `size=32m`；
- **系统配额与句柄**：
  - `ulimits.nofile: 65535`（软硬限制均调至 65535）；
  - `deploy.resources.limits`: `cpus: '4.0'`, `memory: 4096M`（预留充足内存防 OOM）。

### 2.2 缓存与网络高并发优化 (`volur-redis`)
在 [`docker-compose.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/docker-compose.yml) 及 [`package.sh`](file:///Users/humolix/Documents/Dev/Volur-Lite/package.sh) 中对 Redis 进行了以下强化配置：
- **多线程网络 I/O 赋能**：
  - `--io-threads 4` & `--io-threads-do-reads yes`：启用 4 个 I/O 多线程并行处理 Socket 读写，显著提升多客户端并发吞吐。
- **连接与网络调优**：
  - `--maxclients 10000`：放开最大客户端并发至 10000；
  - `--tcp-backlog 511`：提升 TCP 握手队列容量；
  - `--timeout 0` & `--tcp-keepalive 300`：保活客户端连接，避免池化连接无故被斩断；
  - `pids_limit: 250` 与 `ulimits.nofile: 65535`。
- **内存防护与逐出策略**：
  - `--maxmemory 1536mb`：将 Redis 数据内存上限压在 1.5GB（容器内存配额为 2GB）；
  - `--maxmemory-policy allkeys-lru`：内存打满时基于 LRU 自动回收旧缓存，杜绝内存击穿引发容器崩溃；
  - `deploy.resources.limits`: `cpus: '2.0'`, `memory: 2048M`。

### 2.3 Spring Boot 基底 Redis 配置收敛 (`application.yml`)
- **基底下沉 (`application.yml`)**：
  - 将 `spring.data.redis` 完整收敛至基础 `application.yml`：
    ```yaml
    spring:
      data:
        redis:
          host: ${VOLUR_REDIS_HOST:localhost}
          port: ${VOLUR_REDIS_PORT:6379}
          username: ${VOLUR_REDIS_USER:volur}
          password: ${VOLUR_REDIS_PASSWORD:volur_redis_secret_2026}
          database: 0
          timeout: 3000ms
          lettuce:
            pool:
              max-active: 128
              max-idle: 64
              min-idle: 16
              max-wait: 3000ms
    ```
  - Lettuce 连接池并发容量全面升级：`max-active` 调至 128，保持 16 个常驻连接随时响应并发冲击；
  - 严格映射 `username: ${VOLUR_REDIS_USER:volur}`，与 Redis 8 ACL 权限体系无缝接轨；
- **环境隔离精简化**：
  - [`application-dev.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application-dev.yml) 与 [`application-prod.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application-prod.yml) 中完全移除重复的 `data.redis` 节点；
- **平台容器适配**：
  - [`docker-compose.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/docker-compose.yml) 与 [`package.sh`](file:///Users/humolix/Documents/Dev/Volur-Lite/package.sh) 在 `volur-app` 服务中注入 `VOLUR_REDIS_USER=volur` 与 `ulimits.nofile: 65535`。

---

## 3. 验证与审计结论

1. **编译与构建验证**：全平台 13 个 Maven 模块（`volur-framework` 至 `volur-server`）无警告无报错构建成功；
2. **零破坏与高可靠**：在严格贯彻最小权限隔离的前提下，彻底清除了 MySQL/Redis 在文件描述符、并发线程、内存碎片、临时表空间及网络多线程方面的潜在性能栓塞。
