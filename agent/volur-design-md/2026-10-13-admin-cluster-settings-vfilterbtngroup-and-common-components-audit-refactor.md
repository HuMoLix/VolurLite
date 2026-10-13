# 集群设置视图 VFilterBtnGroup 选项卡与通用组件规范化重构

## 1. 变更背景
针对下一代前端工程（`volur-lite-view-NEXT`）中集群设置模块（`AdminClusterSettingsView.vue`）进行全面排查与对齐重构：
1. 原页面中选项卡切换采用原生 HTML `<button>` 标签，未遵循平台通用控件体系，需统一替换为自研轻量级选项卡控件 `VFilterBtnGroup.vue`；
2. 页面中输入框、选择框与按钮未完全规范化，需全面统一采用平台封装的高阶通用组件 `VInput.vue`、`VSelection.vue` 和 `VBtn.vue`；
3. 参考经典前端工程（`src/volur-lite-view/src/views/admin/cluster`）的设计与实现，发现当前 NEXT 版本缺少大量核心功能（包括集群架构拓扑可视化、K3s Token 与 Kubeconfig 凭证配置、Swarm 出站与 TLS 证书配置、Docker Registry 私有镜像仓库管理、节点 SSH 私钥导入认证、节点实时连通性测试与重新加入编排集群等），需完成全量功能的完整补全与模块化解耦。

## 2. 核心架构与改动点

### 2.1 主视图重构 (`AdminClusterSettingsView.vue`)
- **选项卡切换迁移至 `VFilterBtnGroup`**：声明 `tabOptions`，提供“集群调度配置 (`params`)”、“节点拓扑列表 (`nodes`)”与“架构拓扑可视化 (`topology`)”，并展示节点动态数量徽章；
- **组件拆分与解耦**：将单文件平铺逻辑拆分为三个独立子选项卡组件（`ParamsTab.vue`、`NodesTab.vue`、`TopologyTab.vue`）与通用弹窗/拓扑组件（`ClusterNodeModal.vue`、`ClusterTopology.vue`）；
- **操作按钮规范化**：顶部操作栏中“全节点存活对账”与“添加节点”统一迁移为 `VBtn`。

### 2.2 集群调度配置选项卡 (`tabs/ParamsTab.vue`)
- **基础网络与端口配置**：使用 `VSelection` 切换 `DOCKER_SWARM` 与 `K3S` 架构；使用 `VInput` 受控输入公网 Host 与靶机端口映射区间（`portRangeStart` ~ `portRangeEnd`）；
- **Docker Swarm 专有分支**：补齐 Swarm API URL、Overlay 网络名称、TLS 证书目录以及出网访问开关（`dockerSwarmOutboundEnabled`）；
- **K3s / Kubernetes 专有分支**：补齐 K3s APIServer 地址、命名空间（Namespace）、Kubeconfig 配置文件路径以及 Join Token 凭据输入；
- **资源隔离与私有镜像中心 (Docker Registry)**：
  - 调度主从隔离策略开关（`resourceIsolationEnabled`）；
  - 私有 Registry 中心配置（开关、内部 REST 地址、对外开放端口及在线健康状态探针探测）；
- **连通性测试与保存操作**：使用 `VBtn` 执行集群连通性握手测试，完整展示握手耗时（`latencyMs`）、集群版本（`clusterVersion`）与可用节点数（`nodeCount`）。

### 2.3 节点拓扑列表选项卡 (`tabs/NodesTab.vue`)
- **工具栏与操作规范化**：刷新、同步 K3s 节点、全量存活对账、添加节点均采用 `VBtn`；
- **无边框表格样式对齐**：使用 `VTable` 并显式配置 `:bordered="false"`，遵循全局管理后台现代无边框设计风格；
- **节点全生命周期管控动作**：
  - 未入网节点提供“加入集群”操作；
  - 节点信息快捷“编辑”；
  - 题目容器镜像分发“同步镜像”操作；
  - 危险动作带二次确认弹窗的“移除节点”操作。

### 2.4 节点添加与编辑弹窗 (`components/ClusterNodeModal.vue`)
- 基于 `VModal` 封装统一增改弹窗；
- 表单控件全面采用 `VInput` 与 `VSelection`；
- **双模认证凭据支持**：支持密码认证与 SSH 私钥认证切换；私钥凭据支持本地文件上传读取（`.pem`, `.key`, `id_rsa`）与文本直接粘贴；
- **安全保障与连通测试**：编辑模式下包含 IP 变更证书重签风险提示，并集成独立节点实时连通性探测功能。

### 2.5 架构拓扑可视化交互画布 (`components/ClusterTopology.vue` & `tabs/TopologyTab.vue`)
- 移植完整的高性能交互式拓扑图：支持画布拖拽平移、滚轮平滑缩放、全屏切换；
- 动态 SVG 贝塞尔曲线连接主控节点与计算节点，带节点入网就绪状态标记与虚实线状态切换；
- 节点卡片支持拖拽与实时点击，右侧展开抽屉式详情面板，并提供直接发起存活测试、镜像同步与重新加入集群的快捷操作链。

## 3. 验证与质量保证
- 全代码库执行 `vue-tsc --noEmit`，TypeScript 强类型与模板检查 100% 通过（退出码 0），无任何语法、类型或属性声明错误；
- 所有代码文件严格遵循零注释与文档外置铁律。
