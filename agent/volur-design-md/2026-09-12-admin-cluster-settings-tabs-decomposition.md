# 集群管理与调度设置 AdminClusterSettingsView Tab 组件化拆分施工文档

## 1. 变更背景与需求概述
在 Volur Lite 管理端集群与容器管理模块中，[`AdminClusterSettingsView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/AdminClusterSettingsView.vue) 承担了集群核心编排参数、节点列表管理、健康巡检、镜像下发同步以及集群拓扑图的全量逻辑。此前该视图将「集群参数与调度设置」与「节点管理与集群扩展」的表单、表格及业务交互全部直接内嵌在单文件组件中，导致页面代码量超 1000 行，业务逻辑与视图耦合较重。

为符合 Volur Lite 前端模块化架构规范（参考 `views/admin/system/tabs/` 与 `views/admin/competition/tabs/` 的成功设计模式），本次重构将 `AdminClusterSettingsView.vue` 内部内联的各个 Tab 彻底解耦，分离为独立的单文件组件（SFC），并统一收敛至 `src/views/admin/cluster/tabs/` 目录下。

---

## 2. 改动点清单与技术实现

### 2.1 新建独立的 Tab 子组件 (`src/views/admin/cluster/tabs/`)

1. **`ParamsTab.vue`（集群参数与调度设置）**：
   - **职责定位**：承载集群调度核心配置表单（编排引擎类型、动态端口范围、公网 Host、Docker Swarm 专属 API/Overlay 网络/TLS 证书路径/外网访问、K3s 专属 API/Namespace/Kubeconfig 凭据、主从隔离策略与自动镜像同步）；
   - **内置功能**：封装即时集群连通性探测诊断（`testAdminClusterConnectionApi`）及其结果面板、保存配置（`updateAdminClusterSettingsApi`）；
   - **事件契约**：通过 `settings` Prop 接收外部状态，在保存成功后向父组件发送 `@saved` 事件。

2. **`NodesTab.vue`（节点管理与集群扩展）**：
   - **职责定位**：承载集群节点名册、健康巡检、计算资源配置与节点生命周期操作；
   - **内置功能**：
     - 工具栏：全量节点存活巡检（`checkAllAdminClusterNodesHealthApi`）、刷新名册、添加新工作节点；
     - 节点表格：节点名、IP 及 SSH 端口、角色标识（Master/Worker）、资源节点标记、运行状态（就绪/加入中/异常离线）、容器数、镜像缓存数、硬件规格；
     - 操作列：加入集群操作（`joinAdminClusterNodeApi`）、编辑节点配置、镜像同步下发（`syncImagesToClusterNodeApi`）以及二次确认移除节点（`removeAdminClusterNodeApi`）；
   - **事件契约**：接收 `nodes` 与 `loading` Props，向外派发 `@refresh`、`@add-node`、`@edit-node` 与 `@node-updated` 事件。

3. **`TopologyTab.vue`（集群拓扑架构图）**：
   - **职责定位**：作为集群拓扑架构可视化的 Tab 容器，封装并承载 [`ClusterTopology.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/components/ClusterTopology.vue)；
   - **事件契约**：接收 `nodes`、`settings` 与 `loading` Props，向外传递拓扑图上的 `@refresh` 与 `@edit-node` 事件。

### 2.2 宿主视图重构 (`AdminClusterSettingsView.vue`)

- **瘦身与职责收敛**：从 1032 行大幅精简至约 200 行，仅作为顶层状态编排器与弹窗总控（State Orchestrator）；
- **全局状态协调**：统一管理顶栏集群指标徽章（Total Nodes、Master Nodes、Worker Nodes、Ready Nodes）以及跨组件复用的 [`ClusterNodeModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/components/ClusterNodeModal.vue) 节点弹窗；
- **全站联动广播**：在节点状态更新或节点保存成功后，统一触发 `window.dispatchEvent(new CustomEvent('volur:cluster-nodes-updated'))`，保持镜像管理等其他管理模块的无感实时同步。

---

## 3. 验证情况

1. **类型检查与前端编译**：
   - 运行 `npm run build`（`vue-tsc --noEmit && vite build`）；
   - 结果：构建耗时 4.88s，全量静态类型检查一次性通过，0 Error，0 Warning。
2. **交互完整性验证**：
   - 参数配置保存、连通性探测正常响应；
   - 节点名册加载、添加/编辑节点弹窗联动正常；
   - 拓扑图画布缩放与双向编辑契约保持 100% 完整无损。
