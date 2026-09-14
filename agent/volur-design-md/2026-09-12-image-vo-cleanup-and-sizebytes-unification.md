# 镜像领域模型瘦身与前端自适应格式化改造施工记录

## 1. 变更背景

随着 Volur Lite 镜像管理架构全面切换为中心私有 Registry（Docker Registry V2）托管模式，旧版本中针对各分布式节点的离散同步与副本管理逻辑（包括各节点 IP 分布、离线判断、迁移状态等）已彻底废止。在此背景下：
1. **接口冗余与职责混淆**：
   - 镜像分页列表接口 `/api/v1/admin/images?page=1&size=10` 及详情接口 `/api/v1/admin/images/<imageId>` 中依然保留了服务端预生成的 `sizeFormatted` 格式化字符串；
   - 服务端返回固定的废弃字段（`nodeIps: []`、`onlineNodeIps: []`、`offlineNodeIps: []`、`isOffline: false`、`status: "READY"`），产生多余的数据载荷并混淆领域模型职责；
2. **单一数据源原则**：
   - 后端领域 VO（`AdminClusterImageVO`、`AdminClusterImageDetailVO`、`DockerImageLayerVO`）应当遵循 RESTful 原则，仅向外部暴露纯净的度量数值 `sizeBytes`；
   - 前端各视图（数据表格、详情抽屉总览、Docker 构建历史分层、容器创建下拉选项）统一基于 `@/utils/parser.ts` 中的 `formatBytes` 函数自适应换算为可读单位（B、KB、MB、GB、TB）。

---

## 2. 具体改动点

### 2.1 后端工程改造 (Java & Spring Boot)

1. **VO 模型精简与废弃字段删除**：
   - **`AdminClusterImageVO.java`**：
     - 彻底删除 `sizeFormatted`；
     - 彻底删除节点分布与状态字段：`nodeIps`、`onlineNodeIps`、`offlineNodeIps`、`isOffline`、`status`；
     - 移除无用 import `java.util.List`；
   - **`AdminClusterImageDetailVO.java`**：
     - 彻底删除 `sizeFormatted`；
     - 彻底删除节点分布与状态字段：`nodeIps`、`onlineNodeIps`、`offlineNodeIps`、`isOffline`、`status`；
   - **`DockerImageLayerVO.java`**：
     - 彻底删除 `sizeFormatted`，仅保留原始构建命令与 `sizeBytes`。

2. **MapStruct 映射器清理 (`ClusterImageMapper.java`)**：
   - 移除针对 `sizeFormatted` 和 `nodeIps` 的 `@Mapping` 注解表达式；
   - 移除多余的工具方法 `formatBytes(Long)` 与 `parseNodeIps(String)`，消除无用映射开销。

3. **业务实现层收敛 (`AdminImageServiceImpl.java`)**：
   - `listImages`：移除为每个 VO 填充已废弃固定字段（`status`、`isOffline`、`nodeIps` 等）的 `for` 遍历逻辑，由 MapStruct 纯净输出；
   - `getImageDetail`：移除对 `sizeFormatted` 及废弃节点列表的赋值；
   - `DockerImageLayerVO` 构建：移除 `.sizeFormatted(formatBytes(sizeBytes))`；
   - 彻底删除 `AdminImageServiceImpl` 尾部已无任何调用的私有方法 `formatBytes(long bytes)`。

### 2.2 前端工程改造 (Vue 3 + TypeScript)

1. **TypeScript 类型对齐 (`src/types/image/index.ts`)**：
   - 同步更新 `AdminClusterImageVO`、`DockerImageLayerVO`、`AdminClusterImageDetailVO` 接口定义，彻底移除已废弃字段，确保严格映射后端响应结构，杜绝类型逃逸。

2. **镜像控制台视图格式化重构 (`src/views/admin/image/AdminImageView.vue`)**：
   - **数据表格列**：将表格列 `key` 调整为 `sizeBytes`，`render` 函数通过 `formatBytes(row.sizeBytes)` 动态计算展示；
   - **详情抽屉头部**：总大小采用 `formatBytes(detailImage.sizeBytes)` 动态格式化；
   - **历史分层 (Layers Tab)**：汇总徽标与每个分层卡片体积均通过 `formatBytes` 格式化 `layer.sizeBytes`；
   - **配置总览 (Inspect Tab)**：物理存储体积统一由 `formatBytes` 动态显示。

3. **容器管理视图关联修复 (`src/views/admin/container/AdminContainerView.vue`)**：
   - 引入 `@/utils/parser` 中的 `formatBytes`，修复手动运行容器下拉选单中镜像体积的动态格式化标签。

---

## 3. 后台刷新触发 `/api/v1/admin/tasks/statistics` 诊断分析

用户反馈：在后台任意界面刷新均会触发 `/api/v1/admin/tasks/statistics`。
- **定位原因**：
  - 在 `src/volur-lite-view/src/layout/AdminLayout.vue` 中，管理员侧边栏包含“任务管理”菜单项（`key: 'AdminTasks'`）；
  - 为在菜单项旁实时展示当前正在执行或排队中的任务数量小红点（Badge），在 `AdminLayout` 的 `onMounted` 钩子中主动调用了 `updateTaskBadge()` -> `getAdminTaskStatisticsApi()`；
  - 此外，`AdminLayout` 建立了 SSE 长连接客户端（`/api/v1/realtime/stream`），当接收到 `task_event` 推送时也会调用 `updateTaskBadge()` 以保持角标数字准确。
- **结论与建议**：
  - 这是 AdminLayout 菜单角标（Badge）的主动拉取机制，保证管理员无论在任何子页面刷新进入后台，都能第一眼感知是否有异步任务正在后台运行。该行为属于设计的常态全局状态初始化。

---

## 4. 验证与影响范围

- **后端编译**：Maven 执行 `mvn clean test-compile -pl volur-container -am` 构建成功（Reactor 10/10 模块全部 SUCCESS）；
- **前端打包**：Vite 执行 `npm run build` 打包构建成功，类型校验 100% 通过；
- **影响范围**：镜像列表、镜像详情、分层列表、容器手动启停镜像选择器；网络载荷有效降低，彻底杜绝已废弃节点状态逻辑残留。
