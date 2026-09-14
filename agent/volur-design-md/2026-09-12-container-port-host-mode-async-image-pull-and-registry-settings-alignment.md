# Docker Swarm 宿主机端口映射模式、异步镜像拉取入库与集中 Registry 配置对齐实施报告

## 1. 变更背景与需求动因

本次迭代聚焦平台容器调度、Docker 镜像管理以及集群配置的核心行为与 API 一致性修复，主要解决以下三个问题：

1. **Docker Swarm 容器宿主机端口未显式暴露（`80/tcp` 问题）**：
   - 管理员在 `AdminContainerView.vue` 中启动容器（例如 `nginx:alpine`）并留空宿主机端口以期望系统自动分配，底层生成了 Swarm 服务并分配了端口（如 `22269`）。
   - 但在宿主机执行 `docker ps` 时，容器的 `PORTS` 列仅显示容器暴露端口 `80/tcp`，并未显示宿主机端口映射（`0.0.0.0:22269->80/tcp`），且由于 Docker Swarm 默认 `mode=ingress`（路由网格模式）在部分网络环境（尤其是 Docker Desktop 虚拟化网桥）存在 IPVS 转发或内部网络丢包，导致端口连接挂起。
2. **镜像拉取入库改造为异步任务机制**：
   - 在 `AdminImageView.vue` 中点击“拉取新镜像入库”原为同步阻塞 HTTP 请求。当拉取外部 Docker Hub / ACR 镜像（几百 MB 至数 GB）并打标推送至私有 Registry 时，耗时极长，容易导致前端请求超时或界面冻结。
   - 需对接平台现有的 `AdminAsyncTaskService`（`AsyncTaskType.IMAGE_PULL`），实现即刻返回任务凭据（`AdminAsyncTaskSubmitVO`），并在 `AdminTaskView.vue` 中以 SSE 实时状态流展现拉取与推送进度。
3. **彻底清理过时的集群扩展自动镜像迁移配置**：
   - 平台已全面采用集中的私有 Registry (`volur-registry`) 管理镜像，各节点运行容器时按需从 Registry 拉取镜像。
   - 原先在 `ParamsTab.vue` 及后端集群节点更新中残存的 `autoMigrateImagesEnabled`（`CONTAINER_AUTO_MIGRATE_IMAGES_ENABLED`）已无实际业务价值且易引起概念混淆，需从前后端模型、数据库系统配置及 UI 选项中彻底移除。

---

## 2. 核心问题深度剖析与解决方案

### 2.1 问题 1：Docker Swarm 端口映射机制（Ingress Routing Mesh vs Host Mode）

#### 根因剖析
- 原 `DockerSwarmRuntimeStrategy.java` 中构建服务创建命令时使用短语法：
  `command.add("--publish"); command.add(targetPort + ":" + containerPort);`
- 在 Docker Swarm 中，`--publish <hostPort>:<containerPort>` 默认等价于 `mode=ingress`（Ingress 路由网格）。
- 在 Ingress 模式下：
  - 宿主机端口由集群 Ingress 负载均衡网络统一接管（在 `docker service ls` 中可见 `*:22269->80/tcp`）。
  - 底层运行的物理容器仅挂载在 Overlay 网络中，其容器元数据 `NetworkSettings.Ports` 为 `{"80/tcp": null}`，因此在宿主机执行 `docker ps` 时只能看到 `80/tcp`，不会显示 `0.0.0.0:<hostPort>->80/tcp`。
  - 在单节点或多节点特定场景下，尤其是 Mac Docker Desktop，Ingress 路由网格与本地代理端口映射常因虚拟网卡与 IPVS 造成连接挂起（CLOSE_WAIT）。

#### 修复方案
- 将 Docker Swarm 端口映射显式指定为 `mode=host`：
  `published=<hostPort>,target=<containerPort>,protocol=<proto>,mode=host`
- 效果：
  1. 容器绑定的宿主机端口直接注册至容器实际运行节点的网络命名空间（`HostIp: 0.0.0.0, HostPort: <hostPort>`）。
  2. `docker ps` 显式呈现 `0.0.0.0:<hostPort>-><containerPort>/tcp`，直观明了。
  3. 绕过 Ingress 路由网格与跨宿主机 IPVS 转发开销，直连容器，避免连接超时。

---

### 2.2 问题 2：镜像拉取入库异步任务化与任务管理中心联动

#### 架构与实现方案
1. **服务接口变更 (`AdminImageService.java`)**：
   - `pullImage` 方法签名从 `void pullImage(AdminImagePullRequestDTO request)` 调整为返回 `AdminAsyncTaskSubmitVO pullImage(AdminImagePullRequestDTO request)`。
2. **服务实现重构 (`AdminImageServiceImpl.java`)**：
   - 在同步阶段（<10ms）：解析镜像 `repository` 与 `tag`，预先在数据库中创建或标记 `ClusterImage` 记录为 `status = "PULLING"`，即刻返回任务提交凭据。
   - 在异步线程池阶段：
     - `10%`：连接主控节点并准备拉取源镜像；
     - `25%`：执行 `docker pull` 拉取源镜像；
     - `55%`：拉取完成，打标并推送至中心私有 Registry（`publicRegistryHost/repository:tag`）；
     - `75%`：从 Registry 获取 Manifest、镜像层元数据及大小；
     - `85%`：更新数据库记录为 `status = "READY"`、`inRegistry = true`、更新大小及 SHA256 Digest；
     - `92%`：若指定了目标资源节点，预热同步；
     - `100%`：完成并触发完成广播。
   - 异常处理：捕获异常并将 `ClusterImage` 标记为 `FAILED`，抛出业务异常供任务状态机记录堆栈。
3. **控制器层规范对齐 (`AdminImageController.java`)**：
   - `POST /api/v1/admin/images/pull` 返回 `ApiResponse<AdminAsyncTaskSubmitVO>`。
4. **前端 API 与视图对齐 (`src/api/image/index.ts`, `AdminImageView.vue`)**：
   - `pullAdminImageApi` 返回类型对齐为 `Promise<ApiResponse<AdminAsyncTaskSubmitVO>>`；
   - `AdminImageView.vue` 提交后即刻提示任务已提交后台，关闭弹窗并刷新列表；
   - 任务在 `AdminTaskView.vue` 中通过 SSE 实时展示动态进度条、运行状态与日志。

---

### 2.3 问题 3：删除过时的集群扩展自动镜像迁移配置

#### 改动范围与清理细节
1. **前端清理**：
   - `ParamsTab.vue`：删除 `form.autoMigrateImagesEnabled` 字段、表单项 `<n-form-item :label="t('admin.clusters.form.auto_migrate')">` 以及对应 watch 同步逻辑；
   - `AdminClusterSettingsView.vue`：删除初始状态中的 `autoMigrateImagesEnabled`；
   - `src/types/cluster/index.ts`：删除 `AdminClusterSettingsVO` 与 `AdminClusterSettingsDTO` 中的 `autoMigrateImagesEnabled`；
   - `zh_CN` 与 `en_US` 语言包：清理 `admin.clusters.form.auto_migrate` 和 `auto_migrate_hint`。
2. **后端清理**：
   - `AdminClusterSettingsDTO.java` 与 `AdminClusterSettingsVO.java`：移除 `autoMigrateImagesEnabled` 字段；
   - `AdminClusterServiceImpl.java`：移除 `getClusterSettings` 和 `updateClusterSettings` 对该配置的读写；
   - `AdminClusterServiceImpl.java`：移除 `createNode` 与 `updateNode` 中的旧版自动迁移逻辑（`migrateMasterImagesToWorker`）；
   - `SystemSettingKeys.java` 与 `SystemSettingInitializer.java`：彻底删除 `CONTAINER_AUTO_MIGRATE_IMAGES_ENABLED` 常量及其系统默认配置初始化。

---

## 3. 修改文件清单

| 模块 | 文件路径 | 变更类型 | 说明 |
| :--- | :--- | :--- | :--- |
| **容器运行时** | `volur-container/.../strategy/DockerSwarmRuntimeStrategy.java` | 修改 | 将 Docker Swarm `--publish` 改为 explicit `mode=host` 模式 |
| **镜像服务** | `volur-container/.../service/admin/AdminImageService.java` | 修改 | `pullImage` 签名改为返回 `AdminAsyncTaskSubmitVO` |
| **镜像实现** | `volur-container/.../service/admin/impl/AdminImageServiceImpl.java` | 修改 | `pullImage` 异步化改造，并补充 `getStatistics` 视图字段 |
| **镜像控制器** | `volur-container/.../controller/v1/admin/AdminImageController.java` | 修改 | `POST /pull` 返回 `AdminAsyncTaskSubmitVO` |
| **镜像视图模型** | `volur-container/.../domain/vo/AdminClusterImageStatisticsVO.java` | 修改 | 新增 `totalSizeFormatted` 与 `registryPublicHost` 字段 |
| **集群配置** | `volur-container/.../domain/dto/AdminClusterSettingsDTO.java` | 修改 | 移除 `autoMigrateImagesEnabled` |
| **集群视图** | `volur-container/.../domain/vo/AdminClusterSettingsVO.java` | 修改 | 移除 `autoMigrateImagesEnabled` |
| **集群服务** | `volur-container/.../service/admin/impl/AdminClusterServiceImpl.java` | 修改 | 清理自动迁移读写与节点触发逻辑 |
| **系统设置** | `volur-system/.../constant/SystemSettingKeys.java` | 修改 | 移除 `CONTAINER_AUTO_MIGRATE_IMAGES_ENABLED` |
| **系统初始化** | `volur-system/.../init/SystemSettingInitializer.java` | 修改 | 移除对应默认值初始化 |
| **前端 API** | `src/volur-lite-view/src/api/image/index.ts` | 修改 | `pullAdminImageApi` 返回 `AdminAsyncTaskSubmitVO` |
| **前端类型** | `src/volur-lite-view/src/types/image/index.ts` | 修改 | 补充 `AdminClusterImageStatisticsVO` 字段 |
| **前端类型** | `src/volur-lite-view/src/types/cluster/index.ts` | 修改 | 移除 `autoMigrateImagesEnabled` |
| **前端视图** | `src/volur-lite-view/src/views/admin/image/AdminImageView.vue` | 修改 | 拉取镜像调用异步反馈优化与统计字段清理 |
| **前端集群配置** | `src/volur-lite-view/src/views/admin/cluster/tabs/ParamsTab.vue` | 修改 | 移除自动镜像迁移配置项 |
| **前端集群视图** | `src/volur-lite-view/src/views/admin/cluster/AdminClusterSettingsView.vue` | 修改 | 移除 `autoMigrateImagesEnabled` 初始值 |
| **前端多语言** | `src/volur-lite-view/src/locales/zh_CN/admin/clusters.json` 等 | 修改 | 清理无用翻译键值并增加镜像拉取提示 |

---

## 4. 验证与回归测试

1. **后端 Maven 编译验证**：
   - 执行 `mvn clean compile -f src/volur-lite-platform/pom.xml`。
   - 全部 12 个模块（`volur-framework`, `volur-system`, `volur-container`, `volur-server` 等）构建成功（`BUILD SUCCESS`）。
2. **前端 Vue 3 + TypeScript 编译验证**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）。
   - 类型检查通过，打包零错误构建完成。
3. **Docker Swarm 宿主机端口验证**：
   - 经实测验证，使用 `published=<hPort>,target=<cPort>,protocol=tcp,mode=host` 启动服务时，底层容器在 `docker ps` 中明确输出 `0.0.0.0:<hPort>-><cPort>/tcp`，直接映射至宿主机物理网卡。
