# 集群可视化拓扑架构图与节点入网状态严密核验重构

## 1. 变更背景与问题根因分析 (Background & Root Cause Analysis)

### 1.1 问题现场定位
1. **实机排查现场 (`192.168.0.105`)**：
   - 目标 Worker 节点（IP: `192.168.0.105`，SSH: `root`，主机名 `kali`）上已存在题目镜像 `ghcr.io/humolix/wzbc2025_web_babyweb:latest`（Image ID: `17ddfeea6aa8`）。
   - 通过 SSH 与 Docker API 探测发现，该节点的 Docker 守护进程处于 `Swarm: inactive` 状态，未加入 Docker Swarm 集群。
   - 在平台主控制节点（Mac Docker Desktop，`192.168.0.104`）上查看 `docker service ps` 报错：
     `"no suitable node (scheduling constraints not satisfied on 1 node)"`，失败原因是约束 `node.hostname == volur-worker-02` 无法匹配到任何就绪的 Swarm 节点。
   - 从 `192.168.0.105` 执行 `nc -zv 192.168.0.104 2377`，提示 `Connection refused`。原因是 macOS Docker Desktop 运行在轻量 Hypervisor LinuxKit 虚拟机内，内部 dockerd 虽监听了 2377 端口，但并未向 macOS 物理局域网 IP (`192.168.0.104`) 放行暴露。

2. **既有逻辑缺陷**：
   - **节点添加与编辑缺乏底层入网核验**：在过去逻辑中，只要节点 SSH 凭据正确或 2376 TLS 能连通，系统便直接将其状态标记为 `READY`，未真正核验子节点是否成功加入 Swarm / K3s 编排集群，造成假就绪（Ghost Ready）；
   - **调度约束盲目拼装**：若目标节点不在 Swarm 集群中，服务创建时依然向 Swarm 注入该节点的自定义别名约束，导致 Swarm 任务永久卡在 `Pending/Starting`；
   - **缺乏直观的网络与拓扑可视化**：管理员无法宏观把握控制面（Master）与各数据面（Worker）的连接连通性、入网状态与资源分布。

---

## 2. 核心架构设计与改动点 (Key Implementations)

### 2.1 集群节点持久化模型与入网状态真实性对齐
1. **实体与视图拓展 (`ClusterNode.java` & `AdminClusterNodeVO.java`)**：
   - 新增 `@Column(name = "in_cluster", nullable = false) private Boolean inCluster = false;`：标志节点是否真正处于底层编排集群（Swarm / K3s）的就绪状态；
   - 新增 `@Column(name = "cluster_node_id", length = 64) private String clusterNodeId;`：持久化记录底层编排引擎分配的唯一标识（如 Swarm Node ID）。
2. **底层入网双向探测机制 (`AdminClusterServiceImpl`)**：
   - **Manager 视角**：通过 `docker node ls --format "{{.ID}}\t{{.Hostname}}\t{{.Status}}\t{{.Availability}}\t{{.ManagerStatus}}"` 获取当前集群所有活跃就绪的 Node ID 与 IP；
   - **Worker 视角**：通过 SSH 执行 `docker info --format '{{.Swarm.LocalNodeState}}\t{{.Swarm.NodeID}}'`，核验远程节点的 Swarm 状态是否为 `active`；
   - **一致性对账**：只有当远程 Worker 状态为 `active` 且 Manager 的节点清单中包含该节点且状态为 `Ready` 时，`inCluster` 才置为 `true`；若宿主机在线但未入集群，状态明确标为 `ERROR`，杜绝假 `READY`。

### 2.2 节点添加与修改流程严密校验 (Strict Join Verification)
1. **添加节点 (`addNode`)**：
   - 自动生成 2376+TLS 双向证书后，执行 `docker swarm join`；
   - **硬性入网核验**：加入后等待 Gossip 同步并重新核验 `inCluster`；若加入失败，立即抛出友好的业务异常并附带诊断原因（如 2377 端口连接被拒绝或超时），彻底阻止无效节点被保存为可用节点。
2. **修改节点 (`updateNode`)**：
   - 保存前触发 `performHealthCheck`；
   - 若节点为 Worker 且管理员尝试将其指定为资源节点（`isResourceNode = true`），但 `inCluster == false`，直接拦截并提示必须先完成集群入网。
3. **新增手动入网 API (`joinCluster`)**：
   - 接口：`POST /api/v1/admin/clusters/nodes/{id}/join-cluster`；
   - 允许管理员在排查放通网络或配置端口代理后，一键触发指定 Worker 重新执行 Swarm 加入与对账。
4. **运行时调度拦截 (`DockerSwarmRuntimeStrategy`)**：
   - 在 `resolveSwarmNodeConstraint` 中，若指定的目标节点未处于 Swarm 集群中或处于非就绪状态，立即抛出明确异常，防止生成死锁任务。

### 2.3 交互式集群可视化拓扑图 (`ClusterTopology.vue`)
1. **画布操作与渲染体系**：
   - **平移与缩放 (Pan & Zoom)**：支持滚轮缩放（0.4x ~ 2.2x）与画布鼠标拖拽平移，附带浮动工具栏（放大、缩小、100% 视图重置、全屏沉浸浏览）；
   - **节点内自由拖拽**：管理员可直接鼠标拖拽任意 Master / Worker 节点卡片进行个性化拓扑排版；
   - **动态三次贝塞尔曲线 (Dynamic Cubic Bezier Curves)**：Master 与各 Worker 之间动态计算连接曲线，随节点拖拽实时平滑形变；
   - **动态物理粒子/虚线流动**：
     - `inCluster === true`：绿色流动虚线动画，代表心跳与 Overlay 覆盖网络流量互通；
     - `inCluster === false`：红色警示虚线，中点显示红色叹号指示牌。
2. **节点卡片与详情侧滑抽屉**：
   - **Master 节点**：金色王冠徽章、IP、规格、容器数、Manager Leader 状态；
   - **Worker 节点**：绿色 `IN SWARM` 或红色 `DISCONNECTED` 徽章，实时容器数与缓存镜像数；
   - **侧滑抽屉**：点击节点展示完整硬件、Docker 版本、Swarm Node ID、TLS 状态，并内置“加入/重连集群”、“存活检测”、“同步镜像”、“修改节点”快捷操作。
3. **视觉风格与规范**：
   - 完全对齐 Volur Lite 规范：主要操作按钮 `#000000` 背景、文本 `#444444`、深浅主题自动适配。

---

## 3. 影响范围与测试验证 (Impact & Verification)

### 3.1 编译与工程验证
- **后端工程**：`mvn test-compile -pl volur-container -am` 构建成功，10 个模块 0 error。
- **前端工程**：`npm run build` 构建成功，`vue-tsc` 类型检查通过，0 error。

### 3.2 接口变更与数据迁移说明
- `cluster_node` 表新增 `in_cluster` 与 `cluster_node_id` 两列（由 Hibernate DDL 自动补充更新）。
- 新增接口：`POST /api/v1/admin/clusters/nodes/{id}/join-cluster`。
- 拓扑图无缝集成于集群设置页面 Tab 3（`topology`），不影响原有参数设置与节点列表表格。
