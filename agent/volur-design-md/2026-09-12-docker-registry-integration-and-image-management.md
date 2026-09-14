# Docker Registry 集成与集群镜像管理中心改造施工记录

## 1. 变更背景与架构演进

在 Volur Lite 原架构中，集群镜像的加载与分发主要依赖于节点间通过 SSH 执行 `docker save | docker load` 流式管道传输，或者从外部 Docker Hub 拉取。随着系统支持多集群模式（Docker Swarm 与 K3s / containerd）：
1. **多节点与多运行时镜像分发痛点**：在多节点或混合集群部署场景下，子节点无法快速统一获取主节点中已有或离线导入的题目镜像；
2. **K3s 与 containerd 镜像拉取保障**：K3s 无论底层采用 Docker 还是 containerd，都需要一个网络可达的镜像仓库以下发镜像；
3. **元数据生命周期保护**：若所有工作节点的本地缓存临时清空或节点离线，保存在中心 Registry 中的题目镜像不应被系统对账误判为孤儿镜像而彻底删除；
4. **内部容器网络自动互联**：通过 Docker Compose 启动的私有 Registry 应与平台后端服务（`volur-app`）共享同一容器网络（`volur-network`），后端可通过内网 REST API（`http://volur-registry:5000`）进行自动探测、catalog 编目检索及清单管理，而各子节点与宿主机则通过公开端口访问。

---

## 2. 施工改动详述

### 2.1 基础设施编排 (`docker-compose.yml`)
1. **新增 `volur-registry` 服务**：
   - 镜像：`registry:2`，容器名 `volur-registry`；
   - 端口映射：`${REGISTRY_PORT:-5000}:5000`；
   - 数据持久化：挂载宿主机物理路径 `./data/registry:/var/lib/registry`，确保容器销毁重建数据不丢失；
   - 网络集成：加入 `volur-network` 网络；
   - 环境变量配置：开启清单删除能力 `REGISTRY_STORAGE_DELETE_ENABLED: "true"`。
2. **`volur-app` 服务依赖与环境注入**：
   - 添加 `depends_on: - volur-registry`；
   - 注入环境变量：
     - `VOLUR_CONTAINER_REGISTRY_ENABLED=true`
     - `VOLUR_CONTAINER_REGISTRY_INTERNAL_URL=http://volur-registry:5000`
     - `VOLUR_CONTAINER_REGISTRY_PUBLIC_PORT=${REGISTRY_PORT:-5000}`

### 2.2 系统配置字典与初始化 (`volur-system`)
1. **`SystemSettingKeys.java`**：
   - 新增三个系统级配置常量：
     - `CONTAINER_REGISTRY_ENABLED = "system.container.registry.enabled"`
     - `CONTAINER_REGISTRY_INTERNAL_URL = "system.container.registry.internal_url"`
     - `CONTAINER_REGISTRY_PUBLIC_PORT = "system.container.registry.public_port"`
2. **`SystemSettingInitializer.java`**：
   - 在启动初始化中注册默认配置项，默认开启私有 Registry，默认内部地址 `http://volur-registry:5000`，默认公开端口 `5000`。

### 2.3 容器与镜像领域模型升级 (`volur-container`)
1. **`ClusterImage` 实体**：
   - 新增 `@Column(name = "in_registry") private Boolean inRegistry = false;` 字段，持久化镜像是否保存在中心镜像仓库中。
2. **DTO / VO 模型对齐**：
   - `AdminClusterImageVO` 与 `AdminClusterImageDetailVO`：增加 `Boolean inRegistry` 属性；
   - `AdminClusterImageStatisticsVO`：增加 `Boolean registryOnline`（Registry 连通状态）与 `Long registryImagesCount`（托管镜像总数）；
   - `AdminClusterSettingsDTO` 与 `AdminClusterSettingsVO`：增加 `registryEnabled`、`registryInternalUrl`、`registryPublicPort` 及 `registryOnline`。
3. **`ClusterImageRepository.java`**：
   - 新增 `long countByInRegistry(Boolean inRegistry);` 统计方法。

### 2.4 Docker Registry 专用客户端开发 (`DockerRegistryClient.java`)
在 `com.humolix.volur.container.client` 下新增 `DockerRegistryClient`，基于 Java 25 原生 `HttpClient` 实现 Docker Registry HTTP API V2：
- `isAvailable()`：探测 `GET /v2/` 返回状态码 `200` 或 `401` 判断 Registry 是否在线；
- `listRepositories()`：调用 `GET /v2/_catalog` 获取所有仓库名称；
- `listTags(String repository)`：调用 `GET /v2/{repository}/tags/list` 获取仓库全部标签；
- `getManifest(String repository, String tag)`：带 Docker V2 Schema 2 / OCI 头获取 Manifest 摘要（`Docker-Content-Digest`）；
- `deleteManifest(String repository, String tag)`：调用 `DELETE /v2/{repository}/manifests/{digest}` 清除仓库版本；
- `getPublicRegistryHost()`：自动将配置的靶机公网 Host 与公开端口组合为 `<publicHost>:<registryPublicPort>`，便于子节点直接 `docker pull`；
- `resolveInternalRegistryUrl()`：自动处理从内部容器网络地址到本地宿主机回环地址的容错回退。

### 2.5 集群与镜像服务层核心逻辑集成 (`volur-container/service/admin`)
1. **`AdminClusterServiceImpl.java`**：
   - 构造注入 `DockerRegistryClient`；
   - `getSettings()`：读取 `registryEnabled`、`registryInternalUrl`、`registryPublicPort`，并通过 `dockerRegistryClient.isAvailable()` 获取实时在线状态；
   - `updateSettings()`：持久化保存 Registry 相关参数。
2. **`AdminImageServiceImpl.java`**：
   - 构造注入 `DockerRegistryClient`；
   - `getStatistics()`：设置 `registryOnline` 与 `registryImagesCount`；
   - `loadAndSyncClusterImages()`：
     - **步骤 0（Registry 探测与元数据同步）**：当 Registry 可用时，调用 API 扫描 catalog，自动注册与同步镜像元数据，标记 `inRegistry = true`；
     - **步骤 3（对账核验保护）**：核对 MySQL 与节点物理镜像时，若某镜像在所有受检节点的本地缓存中均不存在，但 `Boolean.TRUE.equals(dbImg.getInRegistry())`，则予以豁免保护（将 `nodeIps` 置空），严禁将其作为孤儿镜像物理清理；
   - `deleteImage(Long id, String targetNodeIp)`：
     - 当执行全量删除（`isFullDelete = true`）且镜像在 Registry 中时，自动通过 Registry API 物理删除 Manifest；
     - 当从单个或多个节点剔除镜像后导致本地节点列表为空时，若镜像存在于 Registry 中，则保留数据库元数据记录；
   - `ensureImageOnNode(String targetNodeIp, String fullImageName)`：
     - 若该镜像标记在中心 Registry 中，优先从 `<publicRegistryHost>/<imageName>` 下发并打标，拉取成功后直接记录拓扑，避免跨节点 SSH 管道传输开销；拉取失败时自动平滑降级为节点间复制；
   - **新增 `pushImageToRegistry(Long id)`**：
     - 允许管理员显式将已有镜像推送至中心私有镜像仓库（打标为 `<publicRegistryHost>/<fullImageName>` 并执行 `docker push`）；
   - **新增镜像打标与推送工具方法**：
     - `tagDockerImageOnNode(String nodeIp, String sourceImage, String targetImage)`；
     - `triggerDockerPushOnNode(String nodeIp, String registryImageName)`。
3. **`AdminImageController.java`**：
   - 新增 API 路由：`POST /api/v1/admin/images/{id}/push-registry`，便于前端一键触发入库。
4. **`K3sRuntimeStrategy.java`**：
   - 将 `startContainer` 与 `startManualContainer` 的镜像拉取策略显式指定为 `withImagePullPolicy("IfNotPresent")`，防止使用 `:latest` 镜像时因本地/Registry 存在镜像却因策略默认 `Always` 导致拉取报错。

### 2.6 前端界面与交互扩展 (`src/volur-lite-view`)
1. **TypeScript 类型对齐**：
   - `@/types/cluster/index.ts`：增加 Registry 字段；
   - `@/types/image/index.ts`：增加 `inRegistry`、`registryOnline`、`registryImagesCount` 字段。
2. **API 模块扩展 (`@/api/image/index.ts`)**：
   - 增加 `pushAdminImageToRegistryApi(id)` 请求函数。
3. **集群参数设置页 (`ParamsTab.vue`)**：
   - 新增“私有镜像仓库管理 (Docker Registry)”设置面板，包含“启用 Registry 中心”开关、内部地址、对外公开端口及其实时在线状态徽标。
4. **镜像管理主视图 (`AdminImageView.vue`)**：
   - 顶部统计卡片中增加“Registry 托管”卡片，动态展示托管镜像数与在线/离线状态；
   - 镜像列表中对于已入库的镜像添加 `Registry` 标签指示；
   - 表格操作列中新增“推送仓库 / 同步仓库”一键操作按钮；
   - 镜像分层详情抽屉的“基础元数据”中展示“中心 Registry 状态”。

---

## 3. 影响范围与审计验证

1. **后端编译验证**：
   - `mvn compile -pl volur-container -DskipTests` -> `BUILD SUCCESS`；
   - `mvn compile -DskipTests`（全模块聚合编译）-> `BUILD SUCCESS`。
2. **前端编译验证**：
   - `npm run build` -> `built in 4.82s`，无类型错误，生产静态资源构建成功。
3. **兼容性与回退保证**：
   - 若用户未启动 Registry 容器或将其关闭（`registryEnabled = false` 或探针不通），系统自动回退至既有的节点间 SSH 流式复制机制，不影响现有题目的拉取与分发。
