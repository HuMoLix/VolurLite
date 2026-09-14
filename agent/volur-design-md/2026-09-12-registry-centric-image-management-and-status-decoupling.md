# 平台镜像管理全面中心 Registry 化重构与状态解耦审计文档

## 1. 变更背景与核心目标

在 CTF 靶场与容器编排体系升级中，平台全面引入私有镜像仓库（Docker Registry V2）作为中心资产库。此前版本的镜像管理存在以下核心架构与交互痛点：
1. **宿主机镜像混入平台**：原 `loadAndSyncClusterImages()` 调用底层 `docker images` 扫描本地宿主机 Docker CLI，导致宿主机部署的中间件（如 MySQL 8.0、Redis、Nginx、平台自研服务等）全部作为“赛题/系统镜像”持久化到了 MySQL `cluster_images` 表中；
2. **多节点拓扑残留**：原镜像管理依赖单机 Docker 时代的“节点分布列表”、“跨节点转移”、“多节点副本备份”等手动 SSH 分发逻辑，与当前中心私有 Registry 架构严重脱节；
3. **“0 在线”状态显示歧义**：原前端概况卡片采用 `{{ statistics.registryImagesCount }} ({{ statistics.registryOnline ? '在线' : '离线' }})`，当中心 Registry 服务正常运行（200 OK）但尚未推送镜像（镜像数为 0）时，界面显示为 `0 (在线)`，引起管理员对“0个节点在线”或“Registry 异常”的误解。

基于用户指令，本次施工彻底将镜像管理重构为 **100% 以中心私有 Registry 为单一事实来源（Single Source of Truth）** 的现代化架构。

---

## 2. 核心改动点说明

### 2.1 后端服务改造 (`volur-container`)

1. **`DockerRegistryClient.java`**:
   - **Docker Registry V2 规范删除支持**：在 Docker Registry V2 标准中，`DELETE /v2/<name>/manifests/<tag>` 不被支持，必须通过 Manifest Digest (`sha256:...`) 进行物理删除。新增 `deleteManifestByTag(String repository, String tag)`，自动查询目标 Tag 的 Manifest Digest 并执行安全物理清除；
   - **开发环境自动回退与平滑降级**：当内部配置的内部容器域名（`http://volur-registry:5000`）在宿主机开发调试时无法解析时，自动平滑回退至 `127.0.0.1:5000`。

2. **`AdminImageServiceImpl.java`**:
   - **剥离宿主机与 Worker 节点 Docker 探测**：在 `loadAndSyncClusterImages()` 中彻底废弃 `fetchLocalDockerImages()` 与 `fetchRemoteDockerImages()`；改为仅调用 `dockerRegistryClient.listRepositories()` 与 `listTags()` 遍历中心 Registry；
   - **孤儿与宿主机镜像强力核验清除**：在扫描中心 Registry 后，比对 MySQL 中的所有记录，对任何不在 Registry Catalog 中或 `inRegistry != true` 的历史宿主机记录（如 MySQL、Redis 等）直接调用 `clusterImageRepository.delete(dbImg)` 彻底清理；
   - **入库拉取流程纯 Registry 化 (`pullImage`)**：拉取源公开镜像至主控节点 Docker，自动打标为 `<publicHost>:5000/<repo>:<tag>` 并推送至中心 Registry，同步录入 `inRegistry = true` 的元数据；
   - **镜像删除流程优化 (`deleteImage`)**：先执行平台赛题容器依赖校验（防止误删正在被题目引用的镜像），随后调用 `deleteManifestByTag` 从中心 Registry 物理删除，并清理本地缓存与数据库记录；
   - **详情与列表精简 (`getImageDetail`, `listImages`)**：不再返回节点拓扑分布与节点脱机状态，状态恒定为就绪 `READY`，镜像列表强制 `cb.isTrue(root.get("inRegistry"))`。

### 2.2 前端视图与交互改造 (`volur-lite-view`)

1. **`AdminImageView.vue` 顶栏统计解耦**：
   - 将原歧义卡片与已废弃的“已覆盖节点数”卡片重构为 **5 个职责明确的统计卡片**：
     - **Registry 服务状态**：明确显示 `在线 (200 OK)`（绿色）或 `离线脱机`（红色），并带有 Registry 公开端点提示；
     - **托管镜像总数**：展示 `statistics.totalImages`；
     - **赛题题目镜像**：展示 `statistics.challengeImages`；
     - **基础与系统镜像**：展示 `statistics.baseImages`；
     - **总存储占用**：展示 `statistics.totalSizeFormatted`。
2. **表格与操作列全面瘦身**：
   - 移除“所在节点分布”与“状态”列；
   - 移除“跨节点转移”、“备份容灾”、“推送到仓库”、“同步所有镜像到资源节点”等过时按钮；
   - 删除操作简化为行内 `NPopconfirm`，确认后直接调用后端 Registry 物理删除 API。
3. **拉取与详情抽屉优化**：
   - 拉取弹窗移除复杂的“目标节点选择”与多节点拦截警告，提示信息明确告知“镜像将自动推送至中心私有 Registry”；
   - 分层详情抽屉移除“部署节点列表”标签，展示“托管仓库位置: 中心私有 Registry”。

### 2.3 实体与多语言定义 (`types` & `locales`)

1. **`src/types/image/index.ts`**:
   - `AdminClusterImageStatisticsVO` 补充 `registryPublicHost?: string`；
   - `AdminImagePullRequestDTO` 中 `targetNodeIp?: string` 调整为可选字段。
2. **`locales/zh_CN/admin/images.json` & `locales/en_US/admin/images.json`**:
   - 更新中英文文案，全面契合中心私有 Registry 托管语境。

---

## 3. 安全隐患防范与影响范围评估

1. **安全性校验**：
   - 镜像物理删除前，系统严格保留 `checkPlatformContainerDependencies(image, null)` 前置校验，若平台当前存在关联此镜像的赛题或运行中容器，立即抛出 `409 CONFLICT` 拦截，杜绝误删导致题目瘫痪；
2. **影响范围**：
   - 范围收敛于管理端镜像管理模块（`volur-container` 镜像管理服务与 `AdminImageView.vue`）；
   - 对靶场比赛、题目容器下发无破坏性变更；集群各节点（Docker Swarm / K3s）在启动赛题容器时均通过内网按需秒级拉取中心 Registry 镜像。

---

## 4. 编译与验证结果

- **后端工程**：执行 `mvn compile`，平台全部 12 个模块（`volur-framework`, `volur-container`, `volur-server` 等）构建通过（`BUILD SUCCESS`）；
- **前端工程**：执行 `npm run build`，`vue-tsc --noEmit` 强类型检查无任何错误，Vite 构建打包成功（`built in 4.84s`）。
