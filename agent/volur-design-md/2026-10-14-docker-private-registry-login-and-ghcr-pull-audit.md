# 镜像拉取私有仓库认证（方案 A：主控中继管道与 GitHub GHCR 认证集成）施工与审计文档

## 1. 变更背景与需求阐述
在 Volur Lite 容器集群架构中，各赛题容器镜像可以通过平台直接从远端镜像仓库拉取。然而在真实攻防演练或企业竞赛场景中，大量私有赛题镜像托管于受访问控制保护的私有仓库（例如 GitHub Packages / GitHub Container Registry `ghcr.io`、企业私有 Harbor 等）。
直接以公开匿名模式拉取私有镜像会导致 Docker Daemon 报 `unauthorized: authentication required` 或 `denied` 错误。

为解决此问题，系统采纳**方案 A（主控中继管道拉取与分发）**：
1. 前端在镜像拉取弹窗中提供可选认证凭据选项（用户名、密码 / GitHub PAT、可选认证服务器地址）；
2. 主控节点（Master Node）在执行拉取前，通过 `--password-stdin` 安全通道完成 `docker login`；
3. 主控节点完成镜像拉取后，将镜像打标并推入集群内建的中心私有 Registry（`volur-registry`）；
4. 在 `finally` 块中立即执行 `docker logout`，杜绝凭据滞留与泄露；
5. 各工作节点（Worker Nodes）后续按需从中心私有 Registry 高速局域网拉取镜像，无需持有外部凭据。

---

## 2. 核心改动点与实现细节

### 2.1 后端接口与 DTO 增强 (`volur-container`)
- **[AdminImagePullRequestDTO.java](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminImagePullRequestDTO.java)**：
  - 新增可选入参字段 `authServer`、`username`、`password`；
  - 标注 `@Trimmed` 防止首尾空格污染凭据；
  - 提供单参构造器 `AdminImagePullRequestDTO(String image)` 保障事件监听器 `ChallengeImagePullEventListener` 与其它系统组件的透明兼容。

### 2.2 镜像服务逻辑与 Docker CLI 执行通道 (`AdminImageServiceImpl.java`)
- **[AdminImageServiceImpl.java](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminImageServiceImpl.java)**：
  - **`resolveAuthServer(String image, String explicitAuthServer)`**：
    - 若管理员显式指定 `authServer`，则直接使用；
    - 若未指定，自动解析镜像前缀（遵循 Docker 规范：首段包含 `.`、`:` 或为 `localhost` 时识别为 Registry 域名，如 `ghcr.io`；否则默认 Docker Hub `docker.io`）。
  - **`executeDockerLogin(String nodeIp, String authServer, String username, String password)`**：
    - 组装 `docker login <authServer> -u <username> --password-stdin`；
    - 使用标准输入流（`stdin`）写入密码/Token 并刷新关闭，彻底避免在命令行参数或系统进程表（`ps aux`）中泄露明文敏感信息；
    - 采用独立的守护线程异步读取并排空 `stderr` 和 `stdout`，防止 OS 管道缓冲区（64KB）满载死锁；
    - 登录失败时抛出 `BusinessException(HttpStatus.UNAUTHORIZED, "Docker Registry 登录认证失败...")`，实现强类型快速失败（Fail-Fast）。
  - **`executeDockerLogoutQuietly(String nodeIp, String authServer)`**：
    - 组装 `docker logout <authServer>`；
    - 在异步任务的 `finally` 块中强制调用，确保生命周期结束时清理主控节点的认证会话，静默处理登出异常避免掩盖主链路错误。
  - **`executeDockerPull(String nodeIp, String imageName, int timeoutSeconds, AsyncTaskContext context)`**：
    - 替换原先无缓冲排空、硬编码 45 秒超时的 `triggerDockerPullOnNode`，支持长达 600 秒的大体积镜像拉取；
    - 轮询检测 `context.isCancelled()`，支持管理员随时取消任务并立即销毁 Docker 进程；
    - 拉取失败时提取错误输出并快速失败。

### 2.3 前端 TypeScript 类型与视图表单同步
- **[src/volur-lite-view-NEXT/src/types/image.ts](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/types/image.ts)** & **[src/volur-lite-view/src/types/image/index.ts](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/types/image/index.ts)**：
  - 为 `AdminImagePullRequestDTO` 新增 `authServer?`、`username?`、`password?` 严格类型定义。
- **[src/volur-lite-view-NEXT/src/views/admin/image/AdminImageView.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/admin/image/AdminImageView.vue)**：
  - 引入 `VSelectBox` 组件，提供“配置私有仓库认证凭据 (拉取私有镜像/GHCR 时勾选)”折叠选项；
  - 遵循 Google Workspace / Material Design 风格，暗色与亮色模式阶梯适配；
  - 密码输入框配置为 `type="password"`；
  - 校验启用认证时用户名与密码非空。
- **[src/volur-lite-view/src/views/admin/image/AdminImageView.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/image/AdminImageView.vue)**：
  - 在经典视图模态框中同步补充 `n-checkbox` 凭据展开项与表单字段，确保双端体验与功能完全一致。

---

## 3. 安全性与可靠性考量
1. **防凭据泄露**：绝不在命令行或日志中拼接密码与 PAT，统一通过标准输入通道传输并在使用完毕后立即 logout；
2. **防管道死锁**：所有外部进程均采用异步流排空（`drainStreamAsync`），杜绝因 Docker CLI 大量输出导致 JVM 线程阻塞；
3. **零 Mock 与快速失败**：凭据错误或网络不通时直接抛出强类型业务异常，不伪造成功响应；
4. **零注释铁律**：所有修改的业务代码文件严格遵循零注释规范，架构考量与审计信息全量外置于本文档。

---

## 4. 验证与编译结果
- **后端平台**：`mvn compile -DskipTests` 全部 13 个模块构建成功（100% SUCCESS，耗时 ~6.5s）；
- **新版前端 (NEXT)**：`npm run build` 编译成功，0 错误；
- **经典前端 (view)**：`npm run build` 编译成功，0 错误。
