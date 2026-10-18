# 本地 ARM64 原生开发环境对齐与多架构打包发布流水线重构方案

## 1. 变更背景与核心目标

在 Apple Silicon (macOS ARM64) 开发环境下，开发者运行 `docker compose up -d` 时出现部分容器处于 `Created` 未启动状态，且本地缓存的 `registry:2`、`redis:8.6.2-alpine`、`volur-app` 与 `volur-nginx` 均为 `amd64` 架构。经全链路探测与因果分析，确认系统存在以下两个核心问题：

1. **macOS 端口冲突与 Compose 依赖链阻断**：
   macOS 自 Monterey (12) 起，系统控制中心内置的 AirPlay Receiver（隔空播放接收器）默认监听宿主机 `0.0.0.0:5000` 端口。原 `docker-compose.yml` 硬编码了 `5000:5000` 端口映射，导致 `volur-registry` 启动时因端口占用而失败停留于 `Created` 状态。后续依赖于 `volur-registry: service_started` 的 `volur-app` 及 `volur-nginx` 随之级联阻塞在 `Created` 状态。

2. **跨平台打包导致本地原生开发环境被持续污染**：
   历史构建脚本 `package-amd64.sh` 硬编码了 `TARGET_PLATFORM="linux/amd64"`，在交叉编译与全量拉取时直接打标并覆盖本地 Docker daemon 的核心 Tag（`volur-app:latest`、`volur-nginx:latest`、`redis:8.6.2-alpine`、`registry:2`）。由于 macOS Docker Desktop 具备 Rosetta 2 转译功能，后续本地直接启动时静默命中本地 amd64 缓存，导致本地未能享受到 ARM64 原生性能，且无法自定义目标架构。

**核心目标**：
- 实现本地开发环境 100% ARM64 原生化（消除 Rosetta 2 模拟开销，提升运行效率与冷启动速度）；
- 消除宿主机 5000 端口冲突，支持动态参数化端口与优雅退避；
- 按照彻底重构规范，将 `package-amd64.sh` 升级为通用全架构打包与发布工具 `package.sh`，支持 `-p / --platform` 自定义架构、`--push` 远程 Registry 推送以及本地开发镜像标签防污染防护。

---

## 2. 核心架构设计与改动点

### 2.1 编排与环境变量参数化 (`docker-compose.yml` & `.env`)
- 在 `docker-compose.yml` 中将 `volur-registry` 宿主机端口映射由固定的 `5000:5000` 调整为支持环境变量控制：
  ```yaml
  ports:
    - "${REGISTRY_PORT:-5001}:5000"
  ```
- 同步调整 `volur-app` 服务中的公开端口环境变量：
  ```yaml
  - VOLUR_CONTAINER_REGISTRY_PUBLIC_PORT=${REGISTRY_PORT:-5001}
  ```
- 在本地 `.env` 与 `.env.example` 中补充维护 `REGISTRY_PORT=5001`，彻底避开 macOS 系统级 `ControlCenter` 5000 端口冲突。

### 2.2 多架构打包与发布脚本重构 (`package.sh`)
彻底废弃旧有 `package-amd64.sh`，构建全新的通用自动化交付工具 `package.sh`：
1. **参数化目标平台 (`-p, --platform`)**：
   - 支持传入 `amd64`、`arm64`、`linux/amd64`、`linux/arm64`，并具备自动规范化转换；
   - 输出文件名根据目标架构自动命名（如 `volur-lite-amd64.tar.gz`、`volur-lite-arm64.tar.gz`），支持通过 `-o` 覆盖；
2. **远程推送支持 (`--push` & `-r, --registry`)**：
   - 增加 `--push` 和 `-r, --registry` 参数；
   - 采用 Docker Buildx 并行交叉编译并直接推送到远程私有 Harbor 或 Docker Hub，支持多架构并行发布（如 `-p linux/amd64,linux/arm64`）；
3. **本地开发镜像标签隔离与还原机制 (Zero-Pollution)**：
   - 在执行非原生跨架构离线打包前，自动记录宿主机本地原生镜像的唯一 ID（`volur-app`、`volur-nginx`、`redis`、`registry`、`mysql`）；
   - 待 `volur-images.tar` 导出完成或异常退出后，触发 `restore_local_tags` 自动将本地 Tag 恢复指向原生的 ARM64 镜像，彻底阻断异构镜像对开发者日常开发环境的污染。

### 2.3 仓库联动更新 (`scripts/git/github.sh`)
- 按照重构彻底性铁律，同步更新 `scripts/git/github.sh` 中的敏感排除与导出列表，将对 `package-amd64.sh` 的引用彻底替换为 `package.sh`，消除历史包袱残留。

### 2.4 本地 IDE 高频调试端口暴露与环境隔离 (`docker-compose.override.yml`)
- 为了满足开发者在 IDE 中启动 Spring Boot 进行秒级热调试的需求，同时保证发布至 Git 仓库的基准文件不泄露内部组件端口，采用 Docker Compose 原生覆盖体系（`docker-compose.override.yml`）：
  - **生产基准文件 `docker-compose.yml`**：保持纯净，移除 MySQL 和 Redis 的宿主机 `ports:` 映射，仅在私有内部网络 `volur-network` 通信，保证生产级网络隔离安全；
  - **本地开发覆盖文件 `docker-compose.override.yml`**：在本地按需声明暴露宿主机调试端口：
    - **MySQL**：宿主机默认端口 `13306`（映射至容器 `3306`），有效避免与宿主机原生安装的 `/usr/local/mysql` 3306 端口冲突；
    - **Redis**：宿主机端口 `6379`（映射至容器 `6379`）；
  - **Git 过滤与模板**：将 `docker-compose.override.yml` 加入 `.gitignore` 与 `scripts/git/github.sh` 排除清单，并提供 `docker-compose.override.yml.example` 作为团队协作参考模板；
  - **开发环境配置对齐**：在 `application-dev.yml` 中将开发环境默认连接参数设置为宿主机暴露端口（MySQL `13306`、Redis `6379`、Registry `http://localhost:5001`），实现开发环境与生产环境的彻底解耦。

---

## 3. 影响范围与验证方案

### 3.1 影响范围
- 容器编排：`docker-compose.yml`（生产安全纯净）、`docker-compose.override.yml`（本地生效，git 忽略）、`docker-compose.override.yml.example`（示例模板）、`.env`、`.env.example`；
- 后端开发配置：`application-dev.yml`；
- 版本忽略：`.gitignore`；
- 打包交付：`package.sh`（新建）、`package-amd64.sh`（彻底移除）；
- 同步脚本：`scripts/git/github.sh`。

### 3.2 验证步骤
1. 执行 `docker compose down` 清理现有残留容器；
2. 清理本地污染的 amd64 镜像 Tag，重新拉取原生 `linux/arm64` 架构的 `registry:2` 和 `redis:8.6.2-alpine`；
3. 本地执行 `docker compose build` 原生构建 `volur-app` 与 `volur-nginx`；
4. 启动集群 `docker compose up -d` 并执行 `docker compose ps`，确认全部 5 个服务均运行于 `linux/arm64` 且状态为 `Up (healthy)`；
5. 执行 `./package.sh -h` 验证全架构打包与发布工具参数解析正常。
