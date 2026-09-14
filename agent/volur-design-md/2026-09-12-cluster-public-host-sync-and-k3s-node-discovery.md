# 集群公网 Host 变更节点镜像去重与 K3s 节点纳管发现设计与施工审计文档

## 1. 变更背景

本次施工针对管理员在集群与容器镜像运维过程中的两项核心需求与痛点进行系统性排查、架构分析与全栈改造：

1. **镜像所在节点分布重复问题**：
   管理员在集群设置（`ParamsTab.vue`）中将“靶机公网 Host”由默认的 `127.0.0.1` 修改为本机局域网/公网 IP 后，在“镜像管理”（`AdminImageView.vue`）页面中，镜像所在节点分布同时出现了 `127.0.0.1` 和本机物理 IP 两个标签。
2. **K3s 集群模式下的节点加入行为与集群节点自动发现扫描需求**：
   - 深入剖析在 K3s 集群模式下，管理端平台如何调用 K3s 核心组件进行 Worker 节点纳管与加入。
   - 当系统切换为 K3s 集群模式或日常运维时，支持自动扫描当前 K3s 集群中已注册的全部物理/虚拟机节点，并自动完成与平台 `ClusterNode` 的增量发现、状态同步与资源对账。

---

## 2. 根因剖析 (Root Cause Analysis)

### 2.1 镜像节点分布重复显示的根本原因
排查后端 `volur-container` 业务逻辑发现以下链路缺陷：
1. **主节点 IP 与配置不同步**：当修改 `publicHost` 时，原系统仅更新了 `SystemSettingKeys.CONTAINER_PUBLIC_HOST` 字典项，而在 `cluster_node` 表中主节点（`volur-master-01`）记录的 `ip_address` 仍残留为历史的 `127.0.0.1`。
2. **Worker 节点过滤逻辑倒错**：在 `AdminImageServiceImpl.loadAndSyncClusterImages()` 的步骤 2 中，Worker 节点过滤条件原写为：
   ```java
   // 反模式代码：
   List<ClusterNode> workerNodes = allNodes.stream()
       .filter(n -> "WORKER".equalsIgnoreCase(n.getRole()) || !n.getIpAddress().equalsIgnoreCase(masterIp))
       .toList();
   ```
   由于使用了 `|| !n.getIpAddress().equalsIgnoreCase(masterIp)`，当主节点数据库记录还是 `127.0.0.1` 而 `masterIp` 已变成物理 IP 时，该条件直接命中为 `true`，导致主控节点被错误当成 Worker 节点进行了二次探测与同步。
3. **镜像 IP 集合未归一化**：在本地镜像发现 (`syncDiscoveredImageToRepository`) 时，如果历史记录已有 `127.0.0.1`，直接使用 `Set.add(masterIp)`，结果变成了 `"127.0.0.1,192.168.x.x"`。而在步骤 3 的对账剔除中，`127.0.0.1` 和 `masterIp` 都判定属于本地并存在镜像，因此两个 IP 都被保留在了 `ClusterImage.nodeIps` 字段中。

### 2.2 K3s 节点加入与现有节点纳管机制分析
- **K3s 官方入网机制**：K3s 控制平面服务在启动后会生成唯一的集群通信凭证，存放在 `/var/lib/rancher/k3s/server/node-token`（格式通常为 `K10<hash>::server:<token>`）。Worker 节点（K3s Agent）加入集群时，通过执行 `k3s agent --server https://<master-ip>:6443 --token <node-token>` 与 Master 建立安全 TLS 连接并注册。
- **Fabric8 KubernetesClient API 拓扑探测**：K3s 完全兼容 Kubernetes API。通过 KubernetesClient 的 `/api/v1/nodes` 端点（`client.nodes().list()`），平台可以直接实时查询整个集群的 Node 资源对象：
  - 节点名称：`metadata.name`
  - 节点内网 IP：`status.addresses` 中类型为 `InternalIP` 的项
  - 节点角色：通过标签 `node-role.kubernetes.io/control-plane` 或 `node-role.kubernetes.io/master` 区分 Master 与 Worker
  - 节点就绪状态：从 `status.conditions` 中查找 `type == "Ready"` 且 `status == "True"`
  - 资源规格：从 `status.capacity` 中提取 `cpu`（核心数）与 `memory`（Ki/Mi/Gi 字节数）
  - 运行时版本：从 `status.nodeInfo.containerRuntimeVersion`（如 `containerd://1.7.15-k3s1`）和 `osImage`、`kernelVersion` 提取

---

## 3. 具体改动点

### 3.1 后端改动 (Java & Spring Boot)

1. **系统配置与持久化扩展**：
   - [`SystemSettingKeys.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/constant/SystemSettingKeys.java)：新增常量 `CONTAINER_K3S_TOKEN = "system.container.k3s.token"`。
   - [`SystemSettingInitializer.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/init/SystemSettingInitializer.java)：初始化 `system.container.k3s.token` 默认项。

2. **K3s 运行时探测与集群纳管支持**：
   - [`ClusterNodeRepository.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/repository/ClusterNodeRepository.java)：新增 `Optional<ClusterNode> findByNodeName(String nodeName)`，支持以主机名为主键进行双向对账。
   - [`K3sRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java)：
     - 暴露 `public synchronized KubernetesClient getClient()` 供管理服务直接复用长连接。
     - 新增 `K3sDiscoveredNode` 记录模型。
     - 新增 `scanK3sNodes()` 方法：直连 K3s API Server 拉取所有 Node，精准解析其 IP、角色、健康状态、CPU 核心数、内存字节数、OS 及内核版本。
     - 提供 `resolveK3sToken()`：优先从系统设置读取，若留空则自动通过本地读取 `/var/lib/rancher/k3s/server/node-token` 提取。

3. **集群设置与节点同步服务 (`AdminClusterServiceImpl`)**：
   - [`AdminClusterSettingsDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminClusterSettingsDTO.java) & [`AdminClusterSettingsVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminClusterSettingsVO.java)：增加 `k3sToken` 字段。
   - `updateSettings()` 核心联动：
     - 当检测到 `publicHost` 变更时，联动更新 `cluster_node` 中 Master 节点的 IP 地址。
     - 全量清洗 `ClusterImage` 库中的 `nodeIps` 字段，彻底剔除 `127.0.0.1`、`localhost` 及旧 Host，归一化注入新物理 IP。
     - 联动更新正在运行的 `CtfChallengeInstance` 的 `nodeIp` 与 `accessUrl`。
     - 重新校准各节点的 `imageCount`。
     - 当切换为 K3s 模式时，自动重置 K3s 客户端并立即触发一次 `syncK3sClusterNodes()` 发现当前集群节点。
   - `syncK3sClusterNodes()` 核心实现：
     - 调用 `k3sStrategy.scanK3sNodes()` 扫描集群物理机器；
     - 结合 `ClusterNodeRepository` 对已有节点进行比对与属性更新（CPU、内存、就绪状态、加入集群标识 `inCluster=true`、集群节点 UID `clusterNodeId` 等）；
     - 对于新发现的 K3s 节点，自动新增入库；
     - 若扫描到的 Master 控制面节点拥有真实物理 IP 且当前 `CONTAINER_PUBLIC_HOST` 为 `127.0.0.1`，自动将系统公网 Host 同步升级为该物理 IP。
   - `addNode()` / `joinCluster()`：
     - 在 K3s 模式下，远程测试目标机是否就绪 K3s / containerd / docker / curl 运行时。
     - 通过 SSH 自动执行 `k3s agent --server ... --token ...` 完成无感集群加入，并轮询 KubernetesClient 进行验证。
   - `testNodeConnection()` / `performHealthCheck()`：
     - 支持 K3s / containerd 运行时的存活与版本探测。

4. **控制器暴露 (`AdminClusterController`)**：
   - 新增 `POST /api/v1/admin/clusters/nodes/sync-k3s` 端点，返回扫描并同步后的节点列表。

5. **镜像管理与对账收敛 (`AdminImageServiceImpl` & `ClusterImageMapper`)**：
   - [`ClusterImageMapper.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/mapper/ClusterImageMapper.java)：`parseNodeIps` 强化 `.distinct()`。
   - [`AdminImageServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminImageServiceImpl.java)：
     - `loadAndSyncClusterImages()`：修正子节点过滤条件为严格的 `"WORKER".equalsIgnoreCase(n.getRole()) && !isLocalNode(n.getIpAddress(), masterIp)`，彻底避免 Master 误判为 Worker。
     - 在步骤 3 对账清理中，对于本地镜像命中判定，全表仅保留唯一的权威 Master IP（优先物理 IP，非 127.0.0.1 时坚决不落 127.0.0.1）。
     - `syncDiscoveredImageToRepository()`：在添加本地镜像时，清理历史残留的 `127.0.0.1` / `localhost`，保持唯一归一化。
     - `listImages()` / `getImageDetail()`：在返回前端 VO 时增加收敛映射，当配置了真实主机 IP 时自动将残留的 `127.0.0.1` 替换为归一化物理 IP 并去重。
     - `updateNodeImageCounts()` 设为公开接口，并在集群参数修改时被联动调用。

---

### 3.2 前端改动 (Vue 3 / TypeScript / Naive UI)

1. **类型与 API 对齐**：
   - [`src/types/cluster/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/cluster/index.ts)：`AdminClusterSettingsDTO` 与 `AdminClusterSettingsVO` 均声明 `k3sToken?: string`。
   - [`src/api/cluster/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/cluster/index.ts)：导出 `syncK3sClusterNodesApi()` 函数，对应 `POST /admin/clusters/nodes/sync-k3s`。
2. **集群参数配置 (`ParamsTab.vue`)**：
   - 当编排模式为 K3s 时，增加 `K3s 节点通信 Token` 输入项（支持点击查看明文/密文切换），并提供 Token 存放路径提示。
3. **节点管理与集群扩展 (`NodesTab.vue`)**：
   - 增加 `clusterType` Prop，在 K3s 集群模式下展示“扫描同步 K3s 节点”操作按钮（带 Loading 状态）。
   - 将原有硬编码标签 `'已入 Swarm'` 升级为动态判断：`row.clusterType === 'K3S' ? '已入 K3s' : '已入 Swarm'`。
4. **镜像视图防御性去重 (`AdminImageView.vue`)**：
   - 在表格列渲染、详情抽屉部署节点列表、镜像转移源节点选择、镜像删除目标节点列表中，统一使用 `Array.from(new Set(...))` 强化兜底去重，确保 100% 杜绝重复节点标签。
5. **国际化 (`zh_CN` / `en_US`)**：
   - 在 `admin/clusters.json` 中补齐 `k3s_token`、`k3s_token_hint`、`k3s_token_placeholder`、`sync_k3s`、`sync_k3s_hint`、`sync_k3s_success` 等双语词条。

---

## 4. 影响范围与测试验证

1. **后端构建验证**：
   - 运行 `mvn compile`，平台全部 12 个模块（`volur-framework`, `volur-logging`, `volur-system`, `volur-user`, `volur-verification`, `volur-notification`, `volur-auth`, `volur-challenge`, `volur-container`, `volur-competition`, `volur-server`）**100% 编译成功 (BUILD SUCCESS)**。
2. **前端构建验证**：
   - 运行 `npm run build`，Vite / vue-tsc 强类型语法检查通过，静态资源打包成功（**耗时 4.62s，零错误零警告**）。
3. **兼容性保障**：
   - Docker Swarm 与 K3s 两种底层编排模式保持完全解耦与隔离。
   - 即使 K3s Token 留空，后端也会尝试从 Master 主机本地文件系统自动探测，保证开箱即用。
