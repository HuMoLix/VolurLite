# 集群节点管理按钮交互优化与双主节点冲突/内存计量深度审计与修复方案

## 1. 变更背景与问题定位

在平台运维集群管理后台 (`/admin/clusters/nodes`) 演进与联调过程中，发现了以下三类交互文案与集群节点状态对账缺陷：
1. **节点拓扑工具栏按钮规格与文案优化**：
   - 按钮文案需直击核心操作语义：“扫描同步 K3s 节点” 需简化为 “节点扫描”；“全节点存活对账” 简化为 “节点检测”；“扩展集群 / 添加节点” 简化为 “添加节点”；
   - 工具栏中包含“刷新”、“节点扫描”、“节点检测”、“添加节点”在内的四个主操作按钮，按钮尺寸由原先的 `small` 统一重塑为更具视觉权重与点击触达率的 `medium` 规格；
   - 国际化中英文/繁体资源 (`zh-CN`, `en-US`, `zh-TW`) 需同步对齐。

2. **双 Master 节点并存与“异常/离线”锁定缺陷 (Phantom Master Conflict)**：
   - 用户切换为 K3s 集群并执行扫描后，节点列表同时展现了两个主节点：
     - `volur-master-01` (`127.0.0.1:22`，8 核 / 16.0 GB，状态为“异常 / 离线”，操作列锁定为“本机控制面 (不可修改)”)；
     - `volur-master-node` (`10.145.251.25:22`，4 核 / 7.7 MB，状态为“调度就绪”，操作列同样锁定为“本机控制面 (不可修改)”)。
   - **根本成因**：
     - 初始初始化兜底：系统冷启动时若 `cluster_nodes` 表为空，系统会自动执行 `initDefaultMasterNode()` 插入硬编码的 `volur-master-01`（`127.0.0.1`）。
     - K3s 发现对账不匹配：用户接入 K3s 并执行“节点扫描”后，物理 Master 节点上报的名称为 `volur-master-node`、IP 为 `10.145.251.25`，比对查重均不命中原有记录，从而执行了 `INSERT` 新建。
     - 缺少当前集群类型隔离与自动占位清理：`listNodes()` 直接调用 `clusterNodeRepository.findAll()`，未对非当前集群或历史占位的主节点进行类型过滤与冗余清理。
     - 控制面不可删除强保护：前端与后端均硬编码禁止删除 `role == "MASTER"` 节点，导致 `127.0.0.1` 离线节点无法通过 UI 或接口被人工清理。

3. **K3s 内存解析单位丢失导致容量严重缩水 (7.7 MB vs 7.7 GB)**：
   - 真实物理机 8GB 内存被渲染为 `7.7 MB`。
   - **根本成因**：Fabric8 Kubernetes Client 的 `Quantity` 模型中，`cap.get("memory").getAmount()` 仅提取了数值字符串（如 `"8078904"`），而丢失了单位格式 `Ki`（存储在 `getFormat()` 中）。原 `parseMemoryBytes` 解析未检测到 `Ki/Mi` 后缀，将其降级处理为原始 Bytes（`8078904` 字节），经前端 `formatBytes` 格式化后错误展示为 `7.7 MB`。

---

## 2. 核心改动点

### 2.1 前端按钮交互与国际化多语言对齐
- **视图层组件**：[`src/volur-lite-view-NEXT/src/views/admin/cluster/tabs/NodesTab.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/admin/cluster/tabs/NodesTab.vue)
  - 刷新按钮、节点扫描按钮、节点检测按钮、添加节点按钮统一切换为 `size="medium"`。
  - 文案绑定调整为 `t('admin.clusters.nodes.sync_k3s', '节点扫描')`、`t('admin.clusters.nodes.check_all_health', '节点检测')`、`t('admin.clusters.nodes.add_node', '添加节点')`。
- **国际化字典**：
  - `zh-CN/admin.json`：更新 `add_node`（添加节点）、`sync_k3s`（节点扫描）、新增 `check_all_health`（节点检测）。
  - `en-US/admin.json`：更新 `add_node`（Add Node）、`sync_k3s`（Scan Nodes）、新增 `check_all_health`（Check Nodes）。
  - `zh-TW/admin.json`：更新 `add_node`（添加節點）、`sync_k3s`（節點掃描）、新增 `check_all_health`（節點檢測）。

### 2.2 后端历史占位 Master 自动清理与对账自愈
- **服务实现**：[`AdminClusterServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminClusterServiceImpl.java)
  - 在 `listNodes()`、`getClusterOverview()` 与 `syncK3sClusterNodes()` 中引入主节点真实性自愈逻辑：
    - 当发现存在具有真实物理网络 IP 且名称不为 `volur-master-01` 的 Master 节点时，自动探测并级联清除库中历史由系统初始化生成的 `127.0.0.1` 离线占位记录（`volur-master-01`）。
    - 确保统计卡片（`totalNodes`, `masterNodes`, `readyNodes`）与表格数据单一可信源完全一致。

### 2.3 K3s 硬件资源容量换算修复 (Quantity In Bytes)
- **调度策略**：[`K3sRuntimeStrategy.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java)
  - 在扫描 K3s 节点容量时，采用 Fabric8 官方标准 `Quantity.getAmountInBytes(memQuantity).longValue()` 换算，精准解析 `8078904Ki` 为对应真实字节数（`8,272,797,696` 字节）。
  - 解决节点硬件配置从 `7.7 MB` 恢复为真实的 `7.7 GB`。

### 2.4 集群主页面冗余顶层按钮精简
- **视图层组件**：[`src/volur-lite-view-NEXT/src/views/admin/cluster/AdminClusterSettingsView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/admin/cluster/AdminClusterSettingsView.vue)
  - 彻底移除了右上角与子组件职责重叠的“全节点存活对账”与“添加节点”两个冗余按钮；
### 2.5 节点列表 VTable 样式扁平化与双语后缀精简
- **视图层组件**：[`src/volur-lite-view-NEXT/src/views/admin/cluster/tabs/NodesTab.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/admin/cluster/tabs/NodesTab.vue)
  - 彻底移除了“节点角色”、“赛题调度”、“集群状态”以及“SSH 标识”中的胶囊药丸背景（`bg-...`、`rounded-full`、`px-2 py-0.5`），仅保留清晰的高对比度语义文字颜色（如 `text-purple-600 dark:text-purple-400`、`text-cyan-600 dark:text-cyan-400`、`text-green-600 dark:text-green-400` 等）；
  - 移除了“节点名称”、“IP 地址”、“运行容器”、“硬件资源”列的多余字重与字体类（`font-semibold`、`font-mono`、`text-xs`、`text-google-text-secondary` 等），直接返回纯文本或精炼容器，全面继承 [`VTable.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/VTable.vue) 的通用单元格排版规范；
  - 节点角色文本严格收敛为“主节点”与“子节点”，彻底删除了 `(Master)` 与 `(Worker)` 历史双语后缀。
- **国际化字典**：
  - `zh-CN/admin.json`：将 `role_master` 调整为 `主节点`，`role_worker` 调整为 `子节点`；
  - `zh-TW/admin.json`：将 `role_master` 调整为 `主節點`，`role_worker` 调整为 `子節點`。

---

## 3. 验证与影响分析

1. **前端编译打包验证**：
   - 执行 `npm run build`，构建成功，无任何 TS 语法与模板警告。
2. **后端全量构建验证**：
   - 执行 `mvn compile -DskipTests`，全部 13 个子模块均构建成功（SUCCESS）。
3. **数据一致性与安全性**：
   - 自愈逻辑仅针对硬编码名称 `volur-master-01` 且 IP 精准为 `127.0.0.1` 的占位节点生效，绝不误删用户手动接入或真实配置的单机/多机集群主控制面；
   - 彻底解除幽灵 Master 节点对集群健康度与资源调度的干扰。
