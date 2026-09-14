# 彻底移除 ClusterImage 的 imageType 与 nodeIps 字段及统计卡片精简施工记录

- **执行日期**：2026-09-12
- **施工模块**：`volur-container`（服务端） / `volur-lite-view`（前端管理界面）
- **涉及文件**：
  - 前端界面：
    - `src/volur-lite-view/src/views/admin/image/AdminImageView.vue`
    - `src/volur-lite-view/src/types/image/index.ts`
  - 后端领域实体与模型：
    - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/entity/ClusterImage.java`
    - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminImagePageRequestDTO.java`
    - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminClusterImageVO.java`
    - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminClusterImageDetailVO.java`
    - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminClusterImageStatisticsVO.java`
  - 数据访问与对象转换：
    - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/repository/ClusterImageRepository.java`
    - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/mapper/ClusterImageMapper.java`
  - 核心业务服务：
    - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminImageServiceImpl.java`
    - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminClusterServiceImpl.java`
    - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminContainerServiceImpl.java`
    - `src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/impl/CtfInstanceServiceImpl.java`

---

## 一、变更背景与设计动机

1. **镜像架构由“各节点孤立缓存”全面演进为“中心私有 Registry 托管与下发”**：
   - 在早期架构中，系统通过 `ClusterImage.nodeIps`（逗号分隔字符串）记录镜像分布在哪些宿主机节点上。但随着中心私有 Registry 的建立，平台所有镜像均托管于私有 Registry 中，集群节点在容器调度与创建时均可按需从 Registry 秒级拉取并运行镜像；
   - 依赖数据库中的字符串字段 `nodeIps` 来维护镜像拓扑不仅存在数据不同步隐患（例如节点 Docker 手动清理或重启导致元数据漂移），还会造成代码中大量耦合的字符串分割与 IP 拼凑逻辑。
2. **废除无业务实际约束的 `imageType`（分类属性）**：
   - 此前将镜像标记为 `CHALLENGE` / `BASE` / `SYSTEM` 仅用于粗粒度展示与过滤，在容器运行、赛题构建及私有镜像仓库管理中并无实质调度差异；
   - 管理端既已在前序重构中删除了修改镜像分类属性的功能与拉取时指定分类的需求，因此决定在实体层、VO 层及统计卡片中彻底剔除该字段，只保留核心指标“托管镜像总数”与“总存储占用”。

---

## 二、具体改动点与实现细节

### 1. 前端（Vue 3 / TypeScript）
1. **统计面板精简 (`AdminImageView.vue`)**：
   - 移除 `stat-ribbon` 顶部的“赛题题目镜像”（`challengeImages`）与“基础与系统镜像”（`baseImages`）两张统计卡片；
   - 仅保留“Registry 状态”、“托管镜像总数”（`totalImages`）与“总存储占用”（`formattedTotalSize`）；
   - 在 `statistics` 响应式对象中移除 `challengeImages` 与 `baseImages` 初始属性。
2. **过滤检索栏与抽屉清理**：
   - 移除搜索栏中的 `filterType` 下拉选择框（`n-select`）与对应 `typeOptions` 计算属性，并清理未使用的 `NSelect`、`CubeOutline` 与 `ServerOutline` 图标导入；
   - 在镜像详情抽屉中移除顶部的 `imageType` 标签以及“基础属性”选项卡中的“镜像功能分类”元数据行；
   - 在 `fetchImages` 请求入参中移除 `imageType` 参数。
3. **TypeScript 领域类型对齐 (`src/types/image/index.ts`)**：
   - `AdminClusterImageVO`：移除 `imageType`；
   - `AdminClusterImageDetailVO`：移除 `imageType`；
   - `AdminClusterImageStatisticsVO`：移除 `challengeImages` 与 `baseImages`；
   - `AdminImagePageRequestDTO`：移除 `imageType` 与 `nodeIp`。

### 2. 后端持久化实体与模型 (`ClusterImage` & DTO / VO)
1. **实体层 (`ClusterImage.java`)**：
   - 移除 `@Table` 上的索引 `@Index(name = "idx_image_type", columnList = "image_type")`；
   - 移除 `private String imageType;` 成员属性；
   - 移除 `private String nodeIps;` 成员属性。
2. **Repository 层 (`ClusterImageRepository.java`)**：
   - 移除 `findByImageType(String imageType);`；
   - 移除 `countByImageType(String imageType);`。
3. **VO / DTO 层**：
   - `AdminClusterImageStatisticsVO.java`：移除 `challengeImages` 与 `baseImages` 属性；
   - `AdminClusterImageVO.java`：移除 `imageType` 属性；
   - `AdminClusterImageDetailVO.java`：移除 `imageType` 属性；
   - `AdminImagePageRequestDTO.java`：移除 `imageType` 与 `nodeIp` 筛选属性。
4. **MapStruct 映射器 (`ClusterImageMapper.java`)**：
   - `toStatisticsVO`：仅映射 `totalImages`、`totalSizeBytes`、`registryOnline`、`registryImagesCount`；
   - `toDetailVO`：移除 `imageType` 映射；
   - `toEntity`：精简为 `ClusterImage toEntity(DiscoveredDockerImage discovered);`，彻底移除不再存在的 `imageType` 与 `nodeIp` 参数及对应 `@Mapping`。

### 3. 服务层深度解耦与清理
1. **`AdminImageServiceImpl.java`**：
   - `getStatistics()`：精简为仅根据 `clusterImageRepository.findByInRegistryTrue()` 汇总 `totalImages` 与 `totalSizeBytes`，不再按分类统计；
   - `listImages()`：移除针对 `request.getImageType()` 的 JPA Specification 查询条件拼接；
   - `pullImage()`：移除实体构造过程中的 `imageType` 与 `nodeIps` 设置，并在异步完成回调中不再维护 `latest.setNodeIps("")`；
   - `migrateMasterImagesToWorker()`：不再依赖 `findByImageType("CHALLENGE")` 与 `img.getNodeIps()`，直接遍历 `findByInRegistryTrue()` 镜像，通过 `triggerDockerPullOnNode` 进行新节点预热；
   - `removeNodeFromImages()`：移除对 `ClusterImage.nodeIps` 字段的遍历修改与写库逻辑，直接刷新节点镜像计数；
   - `loadAndSyncClusterImages()`：移除实体构造与更新时包含的 `imageType` 与 `nodeIps`；
   - `transferImage()`：移除针对源节点在 `image.getNodeIps()` 中的判定以及针对 `CHALLENGE` 类型的特殊限制，改为直接校验集群节点存在性与主控节点限制，异步回调中只更新 `status` 与时间戳；
   - `backupImage()`：移除根据 `image.getNodeIps()` 的重复备份检查与更新逻辑；
   - `exportImage()`：由原先基于 `nodeIps` 的节点选择简化为直接使用 `publicHost`；
   - `pushImageToRegistry()`：由原先基于 `image.nodeIps` 的节点选择演进为遍历集群全部可用节点的 Docker 实例探测是否存在镜像物理副本，杜绝数据库拓扑残留导致的误判；
   - `ensureImageOnNode()`：精简节点物理副本与 Registry 探测流程，移除所有读写 `img.getNodeIps()` 的操作；
   - `syncDiscoveredImageToRepository()`：移除针对本地/远程节点的 `nodeIps` 拼合逻辑，新实体直接通过 `clusterImageMapper.toEntity(img)` 生成；
   - `updateNodeImageCounts()`：统一以中心私有 Registry 托管的镜像总数更新各就绪节点的 `imageCount`；
   - 彻底删除已废弃的私有方法 `parseNodeIps`。
2. **`AdminClusterServiceImpl.java`**：
   - 在管理员更改集群 `CONTAINER_PUBLIC_HOST` 时，移除原有遍历更新所有 `ClusterImage` 中 `nodeIps` 的步骤；
   - 在心跳/指标探测 `updateRemoteNodeMetrics` 中，移除根据子节点镜像列表回写 `ClusterImage.nodeIps` 的循环逻辑；
   - 彻底删除类内部私有方法 `parseNodeIps`。
3. **`AdminContainerServiceImpl.java` 与 `CtfInstanceServiceImpl.java`**：
   - 移除基于 `ClusterImage.nodeIps` 进行节点优先调度的旧式 `isImagePresentOnNode` 私有逻辑；
   - 候选节点选择统一基于就绪资源节点池，容器创建拉取完全由底层 Docker 基础设施与统一 Registry 提供支撑。

---

## 三、安全与隐患评估

1. **数据与持久化安全性**：
   - 数据库表 `cluster_image` 中的 `image_type` 与 `node_ips` 字段已在实体定义中移除。在 Hibernate `ddl-auto: update` 模式下，旧字段在数据库表中保留或被忽略，不会导致已有字段数据丢失或表结构锁死；
   - 后续查询与列表展示均只依赖 `repository`、`tag`、`in_registry` 等核心列，性能得到提升（减少了长文本 `node_ips` 的读取与解析）。
2. **集群多节点调度容错性**：
   - 在新节点加入、容器实例启动、镜像推送到中心 Registry 时，代码均采用直接检测节点物理 Docker Inspect 或 Registry Catalog 的方式，避免了历史缓存元数据与节点实际文件不符导致的死锁或调度失败。

---

## 四、验证结果

1. **后端编译与单元测试**：
   - 执行 `mvn test-compile`：所有 12 个模块编译成功（0 errors）；
   - 执行 `mvn test -pl volur-container`：单元测试全部通过（6 passed, 0 failures）。
2. **前端编译与类型检查**：
   - 执行 `npm run build`：`vue-tsc --noEmit && vite build` 0 告警 0 错误编译构建通过，生产打包完成。
