# 修复容器运行状态准确性判定、交互终端真实会话环境及前端文案对齐

## 1. 变更背景

在管理端容器管理（`AdminContainerView.vue`）使用过程中，发现以下问题与体验缺陷：
1. **多语言词条遗漏与文案不统一**：
   - 搜索栏重置按钮使用了 `common.reset`，但在国际化语言包中未定义该词条，导致显示回退；
   - 原按钮文案“手动启动靶机”在自定义容器运行场景下表述狭隘，需统一修改为更专业的“启动容器”。
2. **容器启动状态误判为 RUNNING**：
   - 当管理员基于 Docker Swarm 启动容器（例如 `national_security_hsm:latest`）时，后端 `docker service create --detach=true` 立即返回 0，系统直接将其状态标记为 `RUNNING` 写入 MySQL 并返回给前端；
   - 实际上，Swarm 调度该任务仍处于 `Preparing`（准备环境、拉取镜像、分配网络）或 `Starting` 阶段；
   - 管理员在前端看到绿色的 `RUNNING` 状态后立即点击“调试终端”，在终端中执行命令（如 `ls`），此时底层容器并未就绪，终端报错提示 `Task State: Preparing 2 seconds ago`，导致操作与状态脱节。
3. **交互终端 Prompt 虚拟硬编码**：
   - 交互式 Web Shell 提示符此前硬编码为 `root@{{ containerId.substring(0, 12) }}:{{ workingDir }}#`；
   - 在 Swarm 架构下，`containerId` 存储的是 Swarm 服务名（如 `swarm-volur-admin-u...`），导致提示符显示为 `root@swarm-volur-:/app#`；
   - 而容器内部实际运行的用户并非 root（例如安全靶机实际以 `ctf` 非特权用户运行），实际容器 ID 也应为 Docker 真实的 12 位十六进制容器短 ID（如 `c0afab5eb70b`），普通用户提示符结尾应为 `$`（即 `ctf@c0afab5eb70b:/app$`）。

---

## 2. 核心改动点与技术实现

### 2.1 后端改动 (volur-container)

1. **终端交互会话详情模型 (`AdminContainerTerminalInfoVO`) 与执行结果模型扩展 (`AdminContainerExecResultVO`)**：
   - 创建 [`AdminContainerTerminalInfoVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminContainerTerminalInfoVO.java)：
     提供 `ready`, `containerShortId`, `currentUser`, `workingDir`, `prompt`, `taskState`, `message`。
   - 扩展 [`AdminContainerExecResultVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminContainerExecResultVO.java)：
     增加 `containerShortId`, `currentUser`, `prompt`。

2. **运行时策略接口扩展 (`ContainerRuntimeStrategy`)**：
   - 在 [`ContainerRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/ContainerRuntimeStrategy.java) 中增加：
     - `InstanceStartupStatus checkInstanceStatus(String containerId)`
     - `AdminContainerTerminalInfoVO getTerminalInfo(String containerId, String fallbackFlag)`

3. **Docker Swarm 准确状态等待与真实终端交互实现 (`DockerSwarmRuntimeStrategy`)**：
   - **启动就绪校验 (`waitForServiceReady`)**：
     在 `startContainer` 与 `startManualContainer` 中，执行 `docker service create` 后，不再直接返回 `RUNNING`，而是轮询等待并检测 `docker service ps` 任务状态（最长等待 3.5 秒）：
     - 若任务状态转换为以 `Running` 开头，返回 `InstanceStartupStatus.RUNNING`；
     - 若超时仍在 `Preparing` 或 `Starting`，如实返回 `InstanceStartupStatus.STARTING`；
     - 若任务异常或被拒绝（`Failed` / `Rejected`），返回 `InstanceStartupStatus.FAILED`。
   - **运行态动态巡检 (`checkInstanceStatus`)**：
     通过 `getServiceTaskStatus` 实时反映 Swarm 任务当前阶段（`Running` -> `RUNNING`、`Preparing`/`Starting` -> `STARTING`、`Failed` -> `FAILED`、`Shutdown` -> `STOPPED`）。
   - **真实环境探针与 Prompt 构造 (`getTerminalInfo` & `inspectContainerConfig`)**：
     - 通过 `docker ps --filter label=com.docker.swarm.service.name=...` 查出实际承载该任务的真实 Docker 容器（如 `c0afab5eb70b`）；
     - 通过 `docker inspect` 获取容器实际运行用户（`.Config.User`，如 `ctf`）与工作目录（`.Config.WorkingDir`，如 `/app`）；
     - 根据用户身份动态判定终端符：`root` 采用 `#`，普通用户（如 `ctf`）采用 `$`；
     - 生成真实的终端提示符：`ctf@c0afab5eb70b:/app$`。
   - **Shell 命令执行及工作目录状态保持 (`execCommand`)**：
     - 支持 `cd` 命令解析：若执行 `cd <dir>`，后端自动执行 `cd <dir> && pwd` 并将获取的绝对路径实时回填给 `workingDir`，动态更新下一次命令的终端提示符。

4. **K3s 运行时对齐 (`K3sRuntimeStrategy`)**：
   - 实现 `checkInstanceStatus`，通过 Kubernetes Pod Phase（`Running`, `Pending`, `Failed`, `Succeeded`）对齐状态；
   - 实现 `getTerminalInfo` 与 `execCommand`，返回一致的会话元数据与 Prompt。

5. **服务层与控制器层接口提供 (`AdminContainerService`, `AdminContainerController`)**：
   - 在 [`AdminContainerServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminContainerServiceImpl.java) 的 `listInstances` 中增加实时状态轻量同步逻辑，当发现数据库状态为 `STARTING` 但底层已变为 `RUNNING` 时，自动同步更新数据库并下发新状态；
   - 在 [`AdminContainerController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/controller/v1/admin/AdminContainerController.java) 提供接口 `GET /api/v1/admin/containers/{id}/terminal-info`。

---

### 2.2 前端改动 (volur-lite-view)

1. **类型与 API 对齐**：
   - 在 [`src/types/container/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/container/index.ts) 中增加 `AdminContainerTerminalInfoVO` 及 `AdminContainerExecResultVO` 字段；
   - 在 [`src/api/container/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/container/index.ts) 中导出 `getAdminContainerTerminalInfoApi`。

2. **国际化语言包更新**：
   - `src/locales/zh_CN/common.json` & `en_US/common.json`：补充 `"reset": "重置"` / `"reset": "Reset"`；
   - `src/locales/zh_CN/admin/containers.json` & `en_US/admin/containers.json`：
     - 增加 `"reset": "重置"` / `"Reset"`；
     - 将 `"manualStart"` 文案由“手动启动靶机”修改为“启动容器” / `"Start Container"`。

3. **界面与终端组件重构 ([`AdminContainerView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/container/AdminContainerView.vue))**：
   - **重置与操作按钮**：绑定 `admin.containers.search.reset`，修复 `common.reset` 报错；按钮文案切换为“启动容器”；
   - **容器启动中自动轮询 (Auto-polling)**：
     当列表中存在处于 `STARTING` 或 `DOWNLOADING_IMAGE` 状态的容器时，前端自动启动 3 秒轻量轮询，直至所有容器完全处于 `RUNNING`、`FAILED` 或 `STOPPED`，保证启动体验平滑无需手动重复刷新；组件卸载时自动注销定时器；
   - **真实终端交互环境**：
     - 打开终端弹窗时异步调用 `getAdminContainerTerminalInfoApi(row.id)`；
     - 若容器就绪，解析并展示真实的容器短 ID、当前用户（如 `ctf`）与工作目录，输出类似 `Interactive shell session established as ctf@c0afab5eb70b:/app$`；
     - 输入行与每条命令输出的 Prompt 均渲染真实的动态终端前缀（如 `ctf@c0afab5eb70b:/app$`）；
     - 若容器尚未就绪，友好输出 `[Docker Swarm 提示] 容器实例尚未处于运行就绪状态 (Task State: Preparing...)`，避免误操作。

---

## 3. 验证结果

1. **前端类型与构建**：
   - `npx vue-tsc --noEmit`：0 错误通过；
   - `npm run build`：构建成功（`✓ built in 3.83s`）。
2. **后端编译**：
   - `mvn test-compile -DskipTests`：全平台 12 个模块全部 `BUILD SUCCESS`。
