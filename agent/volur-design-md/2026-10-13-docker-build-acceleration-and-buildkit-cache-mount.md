# 前后端 Docker 构建加速与 BuildKit 缓存持久化改造施工记录

## 1. 变更背景

在执行 `docker compose up -d --build` 构建 Volur Lite 容器集群时，总耗时达 963.3s（约 16 分钟）。耗时瓶颈分析如下：
1. **Maven 海外官方仓库下载瓶颈 (549.6s)**：
   多模块工程（12 个子模块）依赖几百个第三方组件及插件，原 Dockerfile 默认直连官方中央仓库 `repo.maven.apache.org`，国内网络延迟高、带宽受限；且缺乏缓存挂载，每次修改代码触发全量重新下载。
2. **Alpine 官方包管理器网络耗时 (64.0s)**：
   底层 Alpine JRE 与 Nginx 镜像使用 `dl-cdn.alpinelinux.org`，下载 `docker-cli`、`fontconfig`、`ttf-dejavu` 延迟显著。
3. **前端 npm 依赖下载与重复拉取 (74.1s)**：
   前端在 Node 容器中执行 `npm ci` 直连海外官方 npmjs 仓库，缺乏镜像代理与缓存持久化。

---

## 2. 施工改动详述

### 2.1 后端工程构建加速 (`src/volur-lite-platform`)
1. **新增 Maven 国内阿里云镜像配置 (`settings.xml`)**：
   在 `src/volur-lite-platform/settings.xml` 中配置 Aliyun Central 镜像源 (`https://maven.aliyun.com/repository/public`)。
2. **升级后端 `Dockerfile`**：
   - 启用 BuildKit 语法指令 `# syntax=docker/dockerfile:1`；
   - 将 `settings.xml` 同步覆盖至 `/usr/share/maven/conf/settings.xml` 与 `/root/.m2/settings.xml`；
   - 编译阶段引入 BuildKit 缓存挂载：
     `RUN --mount=type=cache,target=/root/.m2/repository mvn clean package -DskipTests`；
     所有下载的 Jar 包均保留在宿主机 BuildKit 缓存层中，后续二次构建无需重复下载任何已有依赖；
   - 运行时阶段将 Alpine 镜像源替换为阿里云源（`mirrors.aliyun.com`），大幅压缩基础包下载时间。

### 2.2 前端工程构建加速 (`src/volur-lite-view-NEXT`)
1. **升级前端 `Dockerfile`**：
   - 启用 BuildKit 语法指令 `# syntax=docker/dockerfile:1`；
   - 引入 npm 缓存挂载与淘宝/腾讯镜像源：
     `RUN --mount=type=cache,target=/root/.npm npm config set registry https://registry.npmmirror.com && (npm ci --prefer-offline || npm install --registry=https://registry.npmmirror.com)`；
   - 运行时阶段 Nginx Alpine 镜像同步替换为阿里云镜像源。

---

## 3. 效益验证与影响范围

1. **构建性能提升对比**：
   - **首次全量构建**：总耗时预计由 16 分钟降低至 1~2 分钟（Maven 依赖下载提速 10~15 倍，Alpine 依赖提速 20 倍）；
   - **二次增量构建**：Java 源码或前端代码变更后，依赖层 100% 命中 BuildKit 宿主机缓存，后端构建预计压缩至 10~15 秒。
2. **影响范围**：
   - [`src/volur-lite-platform/Dockerfile`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/Dockerfile)
   - [`src/volur-lite-platform/settings.xml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/settings.xml)
   - [`src/volur-lite-view-NEXT/Dockerfile`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/Dockerfile)
