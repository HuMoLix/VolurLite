# 集群管理多 Tabs 独立子路由解耦与文案精炼施工记录

## 1. 变更背景与核心目标

在 Volur Lite 新一代前端工程（`volur-lite-view-NEXT`）中，原集群管理视图（`AdminClusterSettingsView.vue`）位于单一路由 `/admin/clusters` 下，内部三个主要子视图（集群调度参数、节点列表、架构拓扑）仅通过组件内部的内存响应式变量（`activeTab`）进行状态切换。

这导致了以下用户体验与架构痛点：
1. **无法通过 URL 直达与持久化定位**：管理员在分享或收藏“节点管理”或“集群拓扑”页面时，刷新后始终回退到第一个 Tab，无法通过独立 URL（如 `/admin/clusters/nodes`）直接访问特定面板；
2. **Tab 按钮文案过长冗余**：原按钮名称为“集群参数与调度设置”、“节点管理与集群拓展”、“集群拓扑架构图”，在多分辨率与紧凑排版下占用过宽视觉空间；
3. **按钮职责定义需精准对应**：管理员要求将三项按钮精炼为直观的“集群设置”、“节点管理”和“集群拓扑”，并分别与标准 RESTful 路由 `/admin/clusters/settings`、`/admin/clusters/nodes`、`/admin/clusters/topology` 强关联。

---

## 2. 施工改动详述

### 2.1 路由架构与重定向扩展 (`src/router/routes/admin.ts`)

重构原单一路由 `path: 'clusters'`，拓展为具备独立 URL 的模块化路由组合：
1. **根路径优雅重定向**：
   - 保留 `path: 'clusters'`，配置 `redirect: '/admin/clusters/settings'`，确保历史外链与侧边栏访问平滑过渡；
2. **三项独立语义化路由**：
   - `path: 'clusters/settings'`：对应组件 `AdminClusterSettingsView.vue`，元数据名称 `AdminClusterSettings`，标题映射 `admin.clusters.tabs.settings`（“集群设置”）；
   - `path: 'clusters/nodes'`：对应组件 `AdminClusterSettingsView.vue`，元数据名称 `AdminClusterNodes`，标题映射 `admin.clusters.tabs.nodes`（“节点管理”）；
   - `path: 'clusters/topology'`：对应组件 `AdminClusterSettingsView.vue`，元数据名称 `AdminClusterTopology`，标题映射 `admin.clusters.tabs.topology`（“集群拓扑”）。

### 2.2 视图组件双向路由联动重构 (`AdminClusterSettingsView.vue`)

1. **Tab 标识与路由语义对齐**：
   - 将内部 `activeTab` 类型与选项值从旧有的 `'params' | 'nodes' | 'topology'` 升级为标准的 `'settings' | 'nodes' | 'topology'`；
   - 更新 `tabOptions` 配置对象，文案分别绑定 `admin.clusters.tabs.settings`（“集群设置”）、`admin.clusters.tabs.nodes`（“节点管理”）、`admin.clusters.tabs.topology`（“集群拓扑”）。
2. **深度双向路由同步状态机**：
   - **路由监听（Route -> Tab）**：借助 `watch(() => route.path, ...)` 与 `syncTabFromRoute()`，在组件加载、浏览器前进后退或地址栏变更时，自动提取子路径尾缀并精准激活对应的 Tab 面板；
   - **点击响应（Tab -> Route）**：新增 `handleTabChange(tab)` 方法，当管理员点击切换 Tab 时，自动调度 `router.push('/admin/clusters/' + tab)` 驱动 URL 变动，实现无缝平滑路由推进。
3. **模板条件渲染解耦**：
   - 将第一项子面板组件的挂载条件调整为 `v-if="activeTab === 'settings'"`，彻底废弃旧有 `params` 别名。

### 2.3 国际化多语言资源字典对齐 (`src/locales`)

同步更新中文（简体）、英文、中文（繁体）语言包中 `clusters.tabs` 的键位与语义：
- **`zh-CN/admin.json`**：
  ```json
  "tabs": {
    "settings": "集群设置",
    "nodes": "节点管理",
    "topology": "集群拓扑"
  }
  ```
- **`en-US/admin.json`**：
  ```json
  "tabs": {
    "settings": "Cluster Settings",
    "nodes": "Node Management",
    "topology": "Cluster Topology"
  }
  ```
- **`zh-TW/admin.json`**：
  ```json
  "tabs": {
    "settings": "集群设置",
    "nodes": "节点管理",
    "topology": "集群拓扑"
  }
  ```

---

## 3. 验证与构建结果

1. **TypeScript 强类型与 Vite 构建**：
   - 执行 `npm run build` in `src/volur-lite-view-NEXT`；
   - `vue-tsc --noEmit` 静态类型检查零错误；
   - Vite 生产包构建成功（`built in 19.09s`），成功输出 `AdminClusterSettingsView-D5vS-2G4.js` 等产物。
2. **导航与回退测试**：
   - 访问 `/admin/clusters` 自动 302 重定向至 `/admin/clusters/settings`；
   - 点击 Tab 分别推进至 `/admin/clusters/nodes` 和 `/admin/clusters/topology`，地址栏实时同步，刷新页面精准保留当前激活面板。
