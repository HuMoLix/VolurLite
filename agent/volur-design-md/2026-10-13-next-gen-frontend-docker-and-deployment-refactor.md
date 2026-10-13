# 新一代前端 volur-lite-view-NEXT 生产 Docker 容器化与部署编排切换方案

## 1. 变更背景与目标
为推进 Volur Lite 平台新一代前端工程（`volur-lite-view-NEXT`）上线，需完成该工程的自包含 Dockerfile 容器化构建、配套生产级 Nginx 反向代理配置，以及容器编排套件（`docker-compose.yml`）与交付构建脚本（`package-amd64.sh`）的平滑切换，彻底替代原有经典前端镜像构建路径。

## 2. 关键设计与改动清单

### 2.1 创建 `src/volur-lite-view-NEXT/Dockerfile`
- **构建环境与基准**：基于 `node:22-alpine` 作为编译阶段，支持 `--platform=$BUILDPLATFORM` 多架构交叉编译；
- **内存防溢出配置**：引入 `ENV NODE_OPTIONS="--max-old-space-size=4096"`，保障 `vue-tsc --noEmit` 与 Vite 生产构建在容器受限内存环境下稳定完成，避免 OOM 崩溃；
- **依赖缓存与产物打包**：优先拷贝 `package.json` 与 `package-lock.json` 执行 `npm ci` 缓存，随后执行 `npm run build`，产物统一输出至 `/app/dist`（含 `plugins/publicResourceMapping.ts` 导出的 `/app/dist/r/static`）；
- **生产镜像**：基于 `nginx:alpine`，挂载 Asia/Shanghai 时区，将 `/app/dist` 拷贝至 `/usr/share/nginx/html`，并拷贝专属 `nginx.conf`。

### 2.2 创建 `src/volur-lite-view-NEXT/nginx/conf/nginx.conf`
- **SPA 路由支持**：配置 `try_files $uri $uri/ /index.html`，彻底解决 Vue Router 4 深层语义路由（如 `/v1/signin/identifier` 等）刷新 404 问题；
- **入口文件防缓存策略**：针对 `/index.html` 精确匹配并注入 `no-cache, no-store, must-revalidate` 响应头，杜绝热更新上线后用户浏览器强缓存导致 Chunk 加载失效；
- **静态资源长效缓存**：对哈希静态资源（CSS、JS、SVG、字体等）设置 30 天缓存与 `public, no-transform`；
- **反向代理与防御**：
  - 透传与自适应客户端真实 IP；
  - 针对敏感认证路由（`/api/v1/auth/login`、`/api/v1/auth/register`、`/api/v1/captcha` 等）执行速率限制（`rate=5r/s`，`burst=10`）；
  - `/api/v1/realtime/stream` 长连接与关闭缓冲（`proxy_buffering off`，`proxy_read_timeout 86400s`）；
  - `/api/` 泛反向代理至 `volur-app:8080`，并开启 WebSocket 升级支持。

### 2.3 根目录 `docker-compose.yml` 协同更新
- 将服务 `volur-nginx` 的 `build.context` 由 `./src/volur-lite-view` 切换为 `./src/volur-lite-view-NEXT`。

### 2.4 离线交付构建脚本 `package-amd64.sh` 协同更新
- 将前端交叉编译步骤中的 `-f` 和构建上下文路径同步指向 `src/volur-lite-view-NEXT/Dockerfile` 与 `src/volur-lite-view-NEXT`。

## 3. 影响范围与验证要求
1. **本地与生产镜像构建**：执行 `docker compose build volur-nginx` 或 `./package-amd64.sh` 时，将自动拉起新前端工程执行 `vue-tsc` 与 Vite 编译；
2. **路由连通性验证**：容器拉起后，访问根路径 `/`、登录流 `/v1/signin/identifier` 以及竞赛大厅 `/competitions` 均能正常由前端路由状态机接管并无刷新加载；
3. **API 与实时推送验证**：API 请求及 SSE 实时事件流可顺畅直达后端平台容器 `volur-app`。
