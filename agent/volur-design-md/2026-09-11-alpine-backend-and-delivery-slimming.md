# 后端切换 Alpine JRE 与交付包瘦身 (摘除 MySQL/Redis)

## 1. 变更背景与需求
- 原交付包 `volur-images.tar.gz` 体积达到 521 MB，分析发现：
  - `mysql:9.6.0` 镜像占用约 280 MB (gzip, 55%)；
  - `volur-app` 基于 Ubuntu JRE 镜像占用约 190 MB (gzip, 35%)；
  - `redis:8.6.2-alpine` 占用约 50 MB (gzip, 10%)。
- 用户要求：
  1. 将公共基础设施镜像（MySQL 与 Redis）摘出交付包；
  2. 将 `volur-app` 后端容器基础镜像切换为 Alpine 版本（`eclipse-temurin:25-jre-alpine`）。

---

## 2. 改动点汇总

### 2.1 后端 Dockerfile (`src/volur-lite-platform/Dockerfile`)
- 运行阶段（Stage 3）基础镜像从 `eclipse-temurin:25-jre` 切换为 `eclipse-temurin:25-jre-alpine`；
- 镜像未压缩体积从 **507 MB** 降至 **349 MB**（缩减 158 MB，降幅 31.2%）；
- 保持 JDK 25 的 Generational ZGC 参数（`-XX:+UseZGC`）与亚洲时区设置（`TZ=Asia/Shanghai`）。

### 2.2 交付打包脚本 (`scripts/package_delivery.py`)
- 将打包模式 `--mode` 默认值设为 `slim`（轻量交付模式）；
- 在 `slim` 模式下，仅导出业务镜像 `volur-app:latest` 与 `volur-nginx:latest`，彻底排除通用公共镜像 `mysql:9.6.0` 与 `redis:8.6.2-alpine`；
- 保留 `--mode full` 供纯断网/离线机房环境使用；
- 更新生成的 `README.md` 与启动脚本提示，指引目标服务器由 Docker Compose 自动在线拉取官方 MySQL / Redis，或配置外部自建数据库。

---

## 3. 验证与瘦身成果

### 3.1 容器运行与系统健康检查
- 执行 `docker compose build volur-app` 成功构建 Alpine 镜像；
- 执行 `docker compose up -d` 重新部署 `volur-app` 容器；
- 服务启动无报错，数据库连接池 HikariCP、Redis Lua 脚本、雪花 ID 生成器、JPA 实体映射全部正常；
- 验证 `/api/v1/system/settings/public` 接口返回 HTTP 200。

### 3.2 交付包瘦身对比
| 指标项 | 优化前 (Full / Ubuntu JRE) | 优化后 (Slim / Alpine JRE) | 缩减幅度 |
| :--- | :--- | :--- | :--- |
| `volur-app` 镜像未压缩体积 | 507 MB | **349 MB** | -31.2% (-158 MB) |
| 交付镜像包 `volur-images.tar.gz` | 521 MB | **200 MB** | -61.6% (-321 MB) |
| 最终 ZIP 交付压缩包 | 521.82 MB | **198.57 MB** | **-61.9% (-323.25 MB)** |

经过双重瘦身，整个交付压缩包从 522MB 断崖式降至 198MB，极大提升了分发效率与部署便利性。
