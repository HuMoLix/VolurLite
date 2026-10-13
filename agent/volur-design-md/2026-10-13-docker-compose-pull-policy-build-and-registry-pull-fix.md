# Docker Compose 本地业务镜像拉取策略与私有镜像构建隔离施工记录

## 1. 变更背景与问题溯源

在通过 `docker compose up -d` 启动 Volur Lite 生产/集成环境时，控制台抛出如下镜像拉取失败错误：
```text
! Image volur-... unknown: failed to resolve reference "docker.io/library/volur-nginx:latest": unexpected status from HEAD request to https://docker.m.daocloud.io/v2/library/volur-nginx/manifests/latest?ns=docker.io: 403 Forbidden
! Image volur-... unknown: failed to resolve reference "docker.io/library/volur-app:latest": unexpected status from HEAD request to https://docker.m.daocloud.io/v2/library/volur-app/manifests/latest?ns=docker.io: 403 Forbidden
```

### 1.1 根因分析
1. **Compose 默认镜像拉取策略 (Missing-Pulling 陷阱)**：
   在 `docker-compose.yml` 中，`volur-app` 与 `volur-nginx` 同时声明了 `image: volur-app:latest` / `image: volur-nginx:latest` 以及本地构建上下文 `build:`。在未显式传递 `--build` 参数执行 `docker compose up -d` 时，Docker Compose 的默认策略检测到本地缓存不存在对应镜像，会误将业务镜像名称当做 Docker Hub 官方或公开镜像进行远端拉取。
2. **加速源拦截导致 403 Forbidden**：
   `volur-app` 与 `volur-nginx` 属于当前代码工程本地编译产物，远端不存在此镜像。宿主机配置的国内镜像加速代理（如 `docker.m.daocloud.io`）在处理未知的私有镜像 Manifest 查询时直接返回 `403 Forbidden`，导致部署中断。

---

## 2. 施工改动详述

### 2.1 编排文件拉取策略约束 (`docker-compose.yml`)
针对 `volur-app` 与 `volur-nginx` 两项基于本地工程源码构建的服务，显式声明 `pull_policy: build`：
- **`volur-app`**：添加 `pull_policy: build`，强制 Docker Compose 使用 `./src/volur-lite-platform/Dockerfile` 进行就地编译打包，杜绝向公网 Docker Hub 发起拉取请求；
- **`volur-nginx`**：添加 `pull_policy: build`，强制 Docker Compose 使用 `./src/volur-lite-view-NEXT/Dockerfile` 进行前端静态资源编译及 Nginx 镜像就地打包。

---

## 3. 安全隐患说明与影响范围

1. **环境稳定性**：彻底阻断了本地业务镜像向外部未受信任公共镜像仓库查询的非预期网络外联，防止敏感镜像名暴露，并避免因公共镜像代理故障或限流阻断整个系统的拉起。
2. **命令一致性**：开发者与运维人员无论是执行 `docker compose up -d` 还是 `docker compose up -d --build`，系统均能始终准确判定业务镜像为本地源码构建，保障一键交付的幂等性。
3. **影响范围**：
   - 基础设施编排：`docker-compose.yml` 中的 `volur-app` 与 `volur-nginx` 服务声明。
