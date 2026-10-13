# Web 界面原生导入 Docker 镜像归档 (.tar / .tar.gz) 与私有 Registry 自动入库施工记录

## 1. 变更背景与架构动机

在 Volur Lite 原镜像管理体系中，平台将自建 Docker Registry V2 (`registry:2`) 作为整个集群镜像资产的单一事实来源（Single Source of Truth），但此前管理端仅支持通过公开 Docker Hub / 远端源在线拉取（`pull`），或从已存在的本地 Docker 节点进行推送（`push-registry`）。
针对离线靶场环境部署、自定义赛题镜像交付等核心 CTF 竞赛运维场景，管理员通常持有打包好的本地镜像离线归档（如 `image.tar.gz` 或 `image.tar`）。此前管理员必须登录宿主机终端手动执行 `docker load`、`docker tag`、`docker push`，最后再进入 Web 端点击同步，操作链路繁琐且对运维人员具备较高的 CLI 门槛。

为了实现平台级的一键式镜像资产纳管闭环，本次施工在系统中原生拓展了 **Web 端直接上传导入 Docker 镜像归档并全自动推送到中心私有 Registry** 的端到端功能。

---

## 2. 施工改动详述

### 2.1 后端工程核心改造 (`volur-lite-platform`)

1. **异步任务枚举扩展 (`volur-system/domain/enums/AsyncTaskType.java`)**：
   - 新增 `IMAGE_IMPORT("IMAGE_IMPORT", "镜像文件导入")` 异步任务类别，纳入全平台异步任务管理与进度追踪体系。

2. **管理端镜像控制器 (`volur-container/controller/v1/admin/AdminImageController.java`)**：
   - 新增接口端点：`POST /api/v1/admin/images/import`；
   - 接收参数：`@RequestParam("file") MultipartFile file`、`@RequestParam(value = "repository", required = false) String repository`、`@RequestParam(value = "tag", required = false) String tag`；
   - 注解声明：配置 `@Syslog(module = "IMAGE", value = "导入 Docker 镜像文件")` 进行审计记录；
   - 响应模型：统一返回 `ApiResponseEntity<AdminAsyncTaskSubmitVO>`，支持流式异步任务进度跟踪。

3. **管理端镜像服务层规范与实现 (`volur-container/service/admin`)**：
   - **`AdminImageService.java`**：定义 `AdminAsyncTaskSubmitVO importImage(MultipartFile file, String repository, String tag)` 接口签名；
   - **`AdminImageServiceImpl.java`**：
     - **快速失败校验**：校验文件非空、合法后缀（必须为 `.tar`, `.tar.gz`, `.tgz`）以及中心私有 Registry 存活状态；
     - **异步安全落盘**：在同步接收阶段先将上传的数据流落盘至临时目录安全文件（`Files.createTempFile("volur-image-import-", suffix)`），并在异步任务执行完毕（无论成功或失败）的 `finally` 块中确保 `Files.deleteIfExists` 物理清理临时文件，杜绝磁盘泄漏；
     - **流式输入管道与无挂载依赖 (`executeDockerLoadFromFile`)**：通过进程标准输入管道（`process.getOutputStream()`）将落盘文件流式泵入 `docker load` 进程。彻底规避了 `volur-app` 容器内外部路径不一致的问题，容器内无需关心宿主机物理路径映射；
     - **标准输出智能解析**：异步捕获并解析 `docker load` 命令的输出（如 `Loaded image: repo:tag` 或 `Loaded image ID: sha256:...`），智能识别出加载完成的镜像原始标识；
     - **打标并推送至私有 Registry**：依据用户指定或解析出的仓库名与标签，组合为 `<publicRegistryHost>/<targetRepo>:<targetTag>`，依次调用 `tagDockerImageOnNode` 和 `triggerDockerPushOnNode` 推送至中心私有 Registry；
     - **Manifest 解析与数据库入库**：调用 `dockerRegistryClient.getManifest` 获取分层 Digest 与实际体积，持久化至 `cluster_images` 表，状态标记为 `READY`，`inRegistry = true`。

4. **全局上传大小与网关放行优化**：
   - **`application.yml`**：将 `spring.servlet.multipart.max-file-size` 和 `max-request-size` 从 1024MB 提升至 2048MB（2GB）；
   - **Nginx 反向代理配置**：同步更新 `volur-lite-view-NEXT/nginx/conf/nginx.conf` 和 `volur-lite-view/nginx/conf/nginx.conf` 中的 `client_max_body_size 2048M;`，避免大镜像归档被 Nginx 413 (Payload Too Large) 拦截。

---

### 2.2 前端工程双工程协同扩展

1. **新一代前端工程 (`volur-lite-view-NEXT`)**：
   - **`src/api/image.ts`**：新增 `importAdminImageApi(formData: FormData): Promise<ApiResponse<AdminAsyncTaskSubmitVO>>`；
   - **`src/views/admin/image/AdminImageView.vue`**：
     - 在工具栏中添加【导入镜像】操作按钮，并配置 Google 风格 `CloudUploadOutline` 线性图标；
     - 新增【导入本地 Docker 镜像归档】模态框，提供虚线边框拖拽/选择交互组件、仓库名称与标签可选覆盖输入框以及温馨说明；
     - 点击确认后组装 `FormData` 并调用接口，成功后给出 Naive UI 提示并无感刷新资产统计和镜像分页列表。

2. **经典前端工程 (`volur-lite-view`)**：
   - **`src/api/image/index.ts`**：新增 `importAdminImageApi` 请求方法；
   - **`src/views/admin/image/AdminImageView.vue`**：
     - 在工具栏新增“导入镜像”按钮；
     - 集成 Naive UI `NUpload` + `NUploadDragger` 拖拽上传模态框，支持文件选择、参数输入与异步加载状态反馈。

---

## 3. 安全隐患防范与影响范围

1. **流式处理防死锁与 OOM**：
   - 在执行 `docker load` 时，采用独立守护线程读取 `stderr` 避免管道死锁；流式缓冲区限制为 64KB，有效规避将多 GB 文件全部读入 JVM 堆内存导致 OOM。
2. **生命周期安全与磁盘保护**：
   - 临时文件上传落盘后，全流程严格包裹于 `try-finally` 中，保证即使 Docker 加载或推送抛出异常，临时文件也会被即时销毁。
3. **架构影响范围**：
   - 变更完全向后兼容，不破坏已有题目运行与容器分发逻辑；
   - 镜像进入中心 Registry 后，Docker Swarm 与 K3s 集群的所有 Worker 节点均能即时透明调度。

---

## 4. 构建与验证结果

1. **后端构建**：
   - `mvn compile -DskipTests` -> 平台 13 个子模块构建全绿，`BUILD SUCCESS`。
2. **新一代前端构建**：
   - `npm run build` in `src/volur-lite-view-NEXT` -> `built in 14.78s`，强类型检查完全通过。
3. **经典前端构建**：
   - `npm run build` in `src/volur-lite-view` -> `built in 13.65s`，构建完全通过。
