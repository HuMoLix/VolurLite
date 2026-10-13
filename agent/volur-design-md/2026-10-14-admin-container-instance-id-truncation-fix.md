# Admin 容器管理列表实例 ID 异常截断修复记录

## 1. 变更背景与原因分析

在后台容器调度管理视图 `AdminContainerView.vue`（新一代前端 `volur-lite-view-NEXT`）中，管理员手动启动或监控靶机容器时，列表中所有实例的“实例ID”均显示为固定的 `volur-admin-`，无法辨识实际的具体实例标识。

### 1.1 根本成因推导
1. **后端生成机制**：
   在管理员通过后台手动启动容器（`AdminContainerServiceImpl.java#manualStartInstance`）时，若未显式指定自定义容器名称，后端将自动根据目标用户与时间戳生成资源名称（作为 Docker Swarm 服务名或 K8s 资源名）：
   - 包含赛题：`volur-admin-u{userId}-c{challengeId}-{timestamp}`
   - 独立镜像：`volur-admin-u{userId}-{timestamp}`
   后端将其作为 `containerId` 存储并返回至前端 `AdminContainerInstanceVO.containerId`。
2. **前端错误截断**：
   前端在表格渲染实例 ID 列时，误将 `containerId` 当作 Docker 64 位 SHA256 原生容器短 Hash 进行处理，硬编码调用了 `.substring(0, 12)`。
3. **字符长度巧合**：
   字符串 `"volur-admin-"` 长度恰好为 12 个字符。因此截取前 12 个字符后，所有包含时间戳和用户/题目 ID 的关键后缀被完全切除，导致界面全量显示为 `"volur-admin-"`。

---

## 2. 改动点清单

### 2.1 前端渲染优化 (`AdminContainerView.vue`)
- 文件位置：`src/volur-lite-view-NEXT/src/views/admin/container/AdminContainerView.vue`
- 移除硬编码截断逻辑 `row.containerId?.substring(0, 12)`；
- 采用真实实例标识 `row.containerId || String(row.id)`，并为容器名增加单行文本省略（`truncate`）与原生 `title` 悬浮提示支持；
- 将列最小宽度从 `160px` 适度微调为 `180px`，容器限制最大宽度为 `260px`，保障表格排版紧凑与自适应表现。

---

## 3. 影响范围与验证

- **影响范围**：仅影响 `volur-lite-view-NEXT` 后台容器调度管理界面的实例 ID 列视觉渲染，不涉及任何底层数据结构、数据库实体或后端 API 协议变动。
- **构建验证**：完成前端代码编译与类型核验，确保无语法与类型错误。
