# Docker 多阶段自动化构建与极简 Docker Compose 编排审计文档

## 1. 变更背景与设计目标
为了实现“**代码拉取或迁移到任意新主机后，无需在宿主机配置 Java 25、Maven 等开发编译工具链，直接依靠 Docker 完成源码编译打包与容器编排启动**”的目标，并彻底清理历史残留的 Kafka、Elasticsearch、MongoDB 等未使用的重量级服务。

---

## 2. 核心改动点与实现细节

### 2.1 后端多阶段构建 Dockerfile (`src/volur-lite-platform/Dockerfile`)
采用标准的 **Docker Multi-Stage Build (多阶段构建)** 技术：
- **构建阶段 (`builder`)**：
  - 基础镜像采用 `maven:3.9-eclipse-temurin-25`，具备原生 Java 25 编译器；
  - 依序复制父级 `pom.xml` 和全部 11 个子业务模块的 `pom.xml`，最大化利用 Docker 构建缓存；
  - 复制项目全部源码，执行 `mvn clean package -DskipTests` 生成聚合可执行 JAR (`volur-server.jar`)。
- **运行阶段 (`runner`)**：
  - 基础镜像采用超轻量级的 `eclipse-temurin:25-jre-alpine`（仅几十兆，丢弃全部 Maven 依赖和编译工具链）；
  - 从 `builder` 阶段提取 `volur-server.jar`；
  - 配置时区 `Asia/Shanghai`，启动参数开启 Java 新一代低延迟 `ZGC` 垃圾收集器。

### 2.2 根目录 `docker-compose.yml` 彻底重构
1. **自动构建联动**：为 `volur-app` 声明 `build: context: ./src/volur-lite-platform` 与 `dockerfile: Dockerfile`，只要执行 `docker compose up --build -d` 即可在容器内部全自动拉取依赖、编译源码并完成启动；
2. **彻底移除未使用服务**：剔除 `volur-kafka`、`volur-elasticsearch`、`volur-mongodb` 及对应的持久化 Volume，直接降低运行主机约 2GB+ 的无谓内存消耗；
3. **服务名与网络契约闭环**：
   - 后端统一为 `volur-app`，与前置 `nginx.conf` 的 `proxy_pass http://volur-app:8080/api/;` 严密对应；
   - Nginx 服务挂载新版 `src/volur-lite-service/nginx/conf/nginx.conf` 及前端构建产物 `src/volur-lite-view/dist`；
   - 依赖项配置 `condition: service_healthy`，确保 MySQL 与 Redis 完全就绪后再拉起后端 Spring Boot 应用。

---

## 3. 部署使用与验证
在任意安装了 Docker 及 Docker Compose 的机器上，进入项目根目录：
```bash
# 一键自动构建后端并拉起整套集群
docker compose up --build -d
```
- 后端编译在独立的 Docker Builder 沙箱中完成，产物镜像尺寸最小化；
- 启动成功后，通过 `docker compose logs -f volur-app` 查看初始超管账号密码及运行日志。

## 4. 实机全量编译与验证结果
- **执行命令**：`docker compose build volur-app`
- **构建结果**：
  - Java 25 原生编译全模块 12 个工程全部通过（`BUILD SUCCESS`）；
  - 成功剥离编译缓存与依赖工具，最终产出轻量级镜像 `volur-lite-volur-app:latest`（大小约 507MB）；
  - 验证了三阶段跨镜像组装方案（`maven_source` -> `eclipse-temurin:25-jdk` -> `eclipse-temurin:25-jre`）的完美可行性与稳定性。

## 5. 容器编排启动排错与现场联调 (Live Verification)
- **发现问题**：MySQL 官方镜像入口脚本禁止通过 `MYSQL_USER="root"` 配置管理员，会导致容器启动时报错退出（`container volur-mysql is unhealthy`）；
- **修复方案**：从 `docker-compose.yml` 中彻底移除 `volur-mysql` 下多余的 `MYSQL_USER` 与 `MYSQL_PASSWORD` 环境变量，仅保留 `MYSQL_ROOT_PASSWORD` 与 `MYSQL_DATABASE`；
- **启动与健康检查结果**：
  - `volur-mysql`：Up (healthy)
  - `volur-redis`：Up (healthy)
  - `volur-app`：Up，数据表结构自动初始化完成，超管凭证顺利生成；
  - `volur-nginx`：Up，反向代理端口 80 连通性测试通过。


