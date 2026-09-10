# 多节点 Docker 镜像按节点选择性删除与全链路端到端对齐改造审计记录

## 1. 变更背景与需求概述
在 Volur Lite 集群运行架构中，同一个 Docker 镜像可能同时缓存或分发至多个不同的集群节点（例如主控制节点、各个资源 Worker 节点）。此前，前端镜像管理视图（`AdminImageView.vue`）中的“删除镜像”操作仅提供了单一的无参全量删除按钮，当一个镜像存在于多个节点时，管理员无法根据需要选择仅从某个特定节点清理释放磁盘空间，而是会直接触发对全集群所有节点的级联删除。

**本次施工核心目标**：
1. **多节点镜像删除按节点精细化选择**：当镜像同时分布于多个节点时，前端提供交互友好的弹窗对话框（`Delete Modal`），支持管理员自由选择“从指定节点清理（可多选或单选特定节点，保留其他节点镜像副本）”或“从全部节点彻底删除（全量清除）”；对于仅位于单个节点的镜像，保留低摩擦的单机 Popconfirm 直接确认交互。
2. **后端口径与行为一致性严格对齐**：
   - 后端 `DELETE /api/v1/admin/images/{id}` 接口深度支持通过 `targetNodeIp` 传递单个 IP、多个逗号分隔 IP 或空值/`ALL`；
   - 彻底修复 `checkPlatformContainerDependencies` 在指定节点删除时的作用域缺陷：指定节点删除时，仅检查并拦截在该指定节点上运行或关联的历史容器，不再误拦截其他节点上的容器；
   - 强化节点归属校验与本地主节点（`127.0.0.1` 与平台配置的 `CONTAINER_PUBLIC_HOST`）等价性识别（`isLocalNode`）；
   - 更新节点上的镜像计数统计计算逻辑（`updateNodeImageCounts`），确保节点缓存镜像数量精准更新。
3. **国际化与视觉铁律对齐**：
   - 中英双语（`zh_CN`、`en_US`）完整覆盖删除弹窗相关文案；
   - 遵循 Naive UI 组件设计与深浅主题规范，操作按钮、提示状态等与整体设计语言无缝融合。

---

## 2. 关键改动点说明

### 2.1 后端工程重构 (`AdminImageController.java` & `AdminImageServiceImpl.java`)

1. **接口文档与参数规范对齐 (`AdminImageController.java`)**：
   - 更新 `@Operation` 与 `@Parameter` 文档，明确 `targetNodeIp` 支持单个 IP、逗号分隔的多节点 IP 字符串，或留空/`ALL` 表示全量节点彻底清理。
2. **多节点解析、等价性识别与归属核验 (`AdminImageServiceImpl.java`)**：
   - 引入 `isLocalNode(nodeIp, publicHost)` 辅助方法，统一处理 `127.0.0.1`、`localhost` 与动态系统配置的 `publicHost` 之间的节点等价性；
   - 解析待清理节点集合 `targetIpsToDelete`：
     - 若指定了节点，校验每个目标节点是否真实存在于镜像的分布记录中，防止非法或误传 IP；
     - 逐一执行底层 Docker 引擎的容器依赖排查与物理删除核验；
   - 删除后精准维护 `image.nodeIps`：
     - 若清理后仍有其他节点保留副本，则仅更新 `nodeIps` 字段并刷新修改时间，保留数据库记录；
     - 若所有节点均已被清理（或选择了全量删除），则完整删除数据库中的 `ClusterImage` 实体元数据。
3. **容器实例依赖检查作用域精准化 (`checkPlatformContainerDependencies`)**：
   - 方法入参扩展为 `(ClusterImage image, String targetNodeIp)`；
   - 当 `targetNodeIp` 存在时，仅针对调度在该节点上的平台容器实例（`inst.getNodeIp()`）进行状态检查与冲突拦截；其他节点上的运行中容器不会阻断本节点的镜像清理；
   - 当 `targetNodeIp` 为空时，保持全平台范围的严格拦截。
4. **节点镜像缓存统计精准化 (`updateNodeImageCounts`)**：
   - 使用 `parseNodeIps` 结合 `isLocalNode` 进行节点归属研判，杜绝历史代码直接 `split(",")` 带来的前后空格不匹配及主节点 IP 识别失准问题。

### 2.2 前端 API 与实体类型对齐 (`src/api/image/index.ts`)

- 更新 `deleteAdminImageApi` 函数参数签名：
  - `targetNodeIp` 支持 `string | string[]`；
  - 内部自动将数组转为逗号分隔的查询参数（`targetNodeIp: ipParam`），无缝兼容单节点与多节点批量传递。

### 2.3 前端交互与视图组件重构 (`AdminImageView.vue`)

1. **新增精细化删除模态窗 (`showDeleteModal`)**：
   - 顶部显示待删除镜像的完整名称、Tag 与体积，并提示当前所分布的节点总数；
   - **删除范围单选模式**：
     - `SPECIFIC`（从指定节点清理）：展示该镜像实际分布的节点复选卡片列表，支持全选、清空与单选，卡片内清晰标识节点名称、IP 地址、主/从角色及在线/离线状态；
     - `ALL`（从全部节点彻底删除）：高危红色提示警告，说明将向全集群广播物理删除并彻底删除数据库元数据；
   - 校验控制：当处于 `SPECIFIC` 模式时，若未勾选任何节点，底部的“确认删除”按钮保持禁用态，并给出实时表单反馈；若全选节点则提示将触发数据库记录全量清除；
   - 提交操作支持异步 loading 态防重，完成后自动关闭弹窗并刷新列表、概览统计与节点拓扑。
2. **表格操作栏智能分流**：
   - 当 `row.nodeIps && row.nodeIps.length > 1`（多节点镜像）时，点击“删除”触发 `openDeleteModal(row)` 弹出精细化选择弹窗；
   - 当 `!row.nodeIps || row.nodeIps.length <= 1`（单节点镜像）时，保留轻量的 `NPopconfirm`，确认后直接删除，避免单节点场景下的多余操作负担。
3. **表格节点标签便捷交互增强**：
   - 节点标签 Tooltip 中明确展示节点详细属性与操作指引；
   - 标签点击支持一键复制节点 IP 地址至剪贴板。
4. **国际化语言包支持 (`zh_CN/admin/images.json` & `en_US/admin/images.json`)**：
   - 完整添加 `deleteModal` 的标题、副标题、多节点提示、单节点提示、模式描述、选择节点提示、全选/清空、高危警告及成功反馈等中英多语言词条。

### 2.4 Docker 底层虚悬与摘要镜像残留根因剖析与自愈清理 (`removeDockerImageAndConfirm`)

在实践中，当管理员删除子节点上的 `alpine:latest` 镜像时，常发现 Docker 中残留了 `alpine <none> d529dd0c6e55`，其根本原因包括：
1. **Tag 与 Image ID 分离及 `RepoDigests` 仓库摘要残留**：
   - 镜像从远程仓库拉取时，包含 `RepoTags: ["alpine:latest"]` 和 `RepoDigests: ["alpine@sha256:..."]`。
   - `docker rmi alpine:latest` 默认仅执行 **Untag**（移除 `latest` 标签），但该镜像的 `RepoDigests` 依然保留了仓库名 `alpine`；因此在执行 `docker images` 时，Docker 根据摘要解析出 Repository 为 `alpine`，而因无 Tag 显示为 `<none>`。
2. **基础镜像分层依赖保护（Base Layer Protection）**：
   - 在 CTF/AWD 靶场场景下，自制题目环境通常使用 `FROM alpine:latest` 构建，题目镜像分层直接继承自 Alpine 底层分层（`d529dd0c6e55`）；
   - Docker 拥有严格的镜像层级保护规则：**禁止擦除仍被子镜像引用的底层 Base Layer**。即便带 `-f` 参数，Docker 也只会剥离指定的标签名称，而绝对不会删除物理分层。
3. **平台深度自愈优化**：
   - 在 `removeDockerImageAndConfirm` 中，系统在执行 `docker rmi -f <imageName>` 剥离指定标签后，会自动检测该镜像是否已无其他业务标签；
   - 若无其他业务标签，系统自动捕获其 `RepoDigests`（如 `alpine@sha256:...`）和真实 `Image ID`（如 `d529dd0c6e55`）执行安静级联清理尝试，若无子镜像依赖则直接擦除分层；若仍有衍生题目镜像依赖，则安全保留以保障题目容器正常运行。

---

## 3. 安全隐患与异常处理说明
1. **防止跨节点依赖误报导致的合法删除受阻**：此前全平台范围检查会导致只要集群任意节点有该镜像容器在运行，其他节点均无法清理。本次改造后，针对指定节点的删除严格隔离该节点的运行环境，既保证了当前正在运行的容器安全性，又解除了无依赖节点的清理限制。
2. **严格的底层 Docker 物理删除强确认**：即使是针对单一节点删除，依然严格走“底层容器排查 -> `docker rmi -f` -> `docker inspect` 后置核验”完整链路，任何异常均触发事务回滚，杜绝数据库状态与底层 Docker 实际状态脱节。
3. **误删防范**：在前端交互中，指定节点清理与全量删除通过单选框明确分流，全量删除时展示高危警告红色提醒，并对无勾选状态施加提交拦截。

---

## 4. 验证与构建结果
- **后端构建验证**：Maven 编译 `volur-container` 模块通过（`mvn clean compile -pl volur-container -am`，状态：`BUILD SUCCESS`）。
- **前端构建验证**：TypeScript 类型检查与生产打包通过（`npm run build`，状态：`built in 4.34s`，0 errors）。
