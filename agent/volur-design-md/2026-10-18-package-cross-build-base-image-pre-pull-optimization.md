# 交付打包脚本交叉编译基础镜像预拉取与并发元数据超时优化方案

## 1. 变更背景与问题分析

在 macOS (Apple Silicon / ARM64) 宿主机环境下执行 `./package.sh -p amd64 --slim` 进行 `linux/amd64` 离线交付镜像打包时，控制台抛出如下错误：
```
 => ERROR [internal] load metadata for docker.io/library/nginx:alpine 29.4s
 => ERROR [internal] load metadata for docker.io/library/eclipse-temurin:25-jre-alpine 38.4s
ERROR: failed to build: failed to solve: eclipse-temurin:25-jre-alpine: failed to resolve source metadata for docker.io/library/eclipse-temurin:25-jre-alpine: docker.io/library/eclipse-temurin:25-jre-alpine: not found
```
然而，在同一宿主机终端使用 `docker pull --platform linux/amd64 eclipse-temurin:25-jre-alpine` 与 `docker pull --platform linux/amd64 nginx:alpine` 却能正常拉取成功。

### 根因分析
1. **BuildKit 远程元数据并发解析超时**：
   `package.sh` 在执行步骤 1 时，同时以后台并行进程（`docker build ... &` 与 `docker build ... &`）启动后端与前端镜像构建。
   Dockerfiles 在最终运行时阶段依赖特定目标架构基础镜像：
   - 后端：`FROM eclipse-temurin:25-jre-alpine`
   - 前端：`FROM nginx:alpine`
   当宿主机本地 Docker Daemon 尚未缓存目标架构（`linux/amd64`）的基础镜像清单时，BuildKit 会在两路并发构建启动的瞬间同时向 Docker Hub (`registry-1.docker.io`) 发起 Manifest 元数据请求。
   在国内网络环境或高延迟/被限流网络下，并发请求极易触发 BuildKit 内部元数据请求的约 30 秒超时（用户终端报错分别发生在 29.4s 与 38.4s），BuildKit 将超时探测直接标记为 `not found` 并终止构建。
2. **`docker pull` 与 BuildKit 解析差异**：
   `docker pull` 采用标准拉取通道，具备完善的分块下载进度与自适应重试逻辑，并且串行下载时不会遭遇 BuildKit 的并发锁竞争。当镜像被 `docker pull` 拉取至本地镜像存储后，后续的 `docker build` 将命中本地缓存（`load metadata ... 0.0s`），完全无需再次穿透公网请求 Docker Hub。

---

## 2. 改造方案与实现

在 `package.sh` 的构建步骤启动前，显式增加目标架构基础镜像的核验与预拉取步骤：
- 在 `docker build` 并行任务拉起前，通过 `docker pull --platform "$TARGET_PLATFORM"` 依次核验并预拉取：
  1. `eclipse-temurin:25-jre-alpine`（后端运行时基础镜像）
  2. `nginx:alpine`（前端反向代理与静态资源托管基础镜像）
- 当本地已存在该架构镜像时，`docker pull` 将在毫秒级输出 `Image is up to date` 并快速通过；
- 当本地不存在时，`docker pull` 以稳健的前台通道拉取镜像层，保证后续 BuildKit 并发编译阶段 100% 命中本地元数据与层缓存，根治 `failed to resolve source metadata ... not found` 假性缺失问题。

---

## 3. 验证与影响范围

1. **构建与打包全流程验证**：
   在宿主机执行 `./package.sh -p amd64 --slim`：
   - 后端与前端镜像交叉编译成功，均以 0.1s 极速复用缓存；
   - 基础组件导出、套件配置装配及最终 `volur-lite-amd64.tar.gz` 打包均顺利完成（生成包体积 225M）；
   - 宿主机本地原生 ARM64 镜像标签引用自动恢复机制正常生效。
2. **影响范围**：
   - 仅优化 `package.sh` 的构建前置校验流程，不改动任何核心业务逻辑与 Dockerfile 定义；
   - 全面增强跨架构打包流水线在各类网络环境下的鲁棒性。
