# 前端多阶段自动化构建与全容器化编排审计文档

## 1. 变更背景与需求
用户提出在前端模块 `volur-lite-view` 中同样引入 `Dockerfile` 实现“**前端源码自动打包编译 + 内置 Nginx 反向代理与静态托管**”。
使得目标运行主机上既无需安装 Node.js、npm，也无需预先在宿主机生成 `dist` 目录，完全依靠 Dockerfile 多阶段构建完成一键交付。

---

## 2. 核心改动点与实现细节

### 2.1 创建前端 `.dockerignore` (`src/volur-lite-view/.dockerignore`)
- 排除宿主机的 `node_modules`、`dist`、`.git`、日志及临时文件，确保容器内部在纯净隔离的沙箱中执行依赖安装与构建。

### 2.2 前端多阶段构建 Dockerfile (`src/volur-lite-view/Dockerfile`)
- **阶段一 (`builder`)**：
  - 基础镜像采用官方 `node:22-alpine`；
  - 优先拷贝 `package.json` 与 `package-lock.json`，执行 `npm ci --prefer-offline || npm install` 极大化利用 Docker 缓存；
  - 拷贝前端全部源码，执行 `npm run build`（调用 `vue-tsc --noEmit && vite build`）进行严格类型检查与生产静态构建。
- **阶段二 (`runner`)**：
  - 基础镜像采用官方 `nginx:alpine`；
  - 配置时区 `Asia/Shanghai`；
  - 从 `builder` 阶段提取编译生成的 `dist` 目录注入 `/usr/share/nginx/html`；
  - 注入已配置支持 SSE 实时长连接流与 1GB 请求体的 `nginx.conf`；
  - 声明暴露 80 端口，CMD 启动 Nginx。

### 2.3 根目录 `docker-compose.yml` 调整
- 将 `volur-nginx` 的定义从原本的宿主机镜像与 `dist` 目录挂载，直接调整为指向 `./src/volur-lite-view` 的 `build:`：
  ```yaml
  volur-nginx:
    build:
      context: ./src/volur-lite-view
      dockerfile: Dockerfile
    container_name: volur-nginx
    depends_on:
      - volur-app
    ports:
      - "${NGINX_HTTP_PORT:-80}:80"
    networks:
      - volur-network
    restart: always
  ```

---

## 3. 实机全量编译与联调验证
- **构建测试**：执行 `docker compose build volur-nginx`，Vite 编译在 6.29s 内顺利完成（4276 个模块转换完毕），产出独立的 `volur-lite-volur-nginx` 镜像；
- **运行测试**：执行 `docker compose up -d` 重新拉起前端容器，四项核心服务全量就绪：
  - `volur-mysql`: Up (healthy)
  - `volur-redis`: Up (healthy)
  - `volur-app`: Up (healthy)
  - `volur-nginx`: Up (0.0.0.0:80->80)
- **连通性校验**：
  - `curl -sI http://localhost` -> `HTTP/1.1 200 OK`，成功返回 Volur Lite 前端 SPA HTML；
  - `curl -s http://localhost/api/v1/auth/public/settings` -> 接口连通正常，反向代理无缝转发。

## 4. 目录精简与单一真实配置源 (SSOT) 收敛
- **清理动作**：彻底删除历史冗余目录 `src/volur-lite-service/`；
- **路径规范化**：将配置文件统一规范放置于 `src/volur-lite-view/nginx/conf/nginx.conf`；
- **Dockerfile 路径对齐**：更新 `src/volur-lite-view/Dockerfile` 的注入命令为 `COPY nginx/conf/nginx.conf /etc/nginx/nginx.conf`，并重新构建镜像验证通过。


