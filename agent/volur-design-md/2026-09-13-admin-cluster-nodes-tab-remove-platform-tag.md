# 集群管理节点列表移除主节点冗余 PLATFORM 标签

## 1. 变更背景
在集群管理视图的节点管理标签页 [`NodesTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/tabs/NodesTab.vue) 中，表格第一列“节点名称”原本针对主节点（`role === 'MASTER'`）渲染了一个 `PLATFORM` 标签。

考虑到紧接着的第二列已经明确标示了节点角色为“主控制节点”（`role_master`），在名称列额外附加 `PLATFORM` 标签存在信息冗余，且占用了紧凑的节点名称列宽空间。因此按需求移除该标签展示。

## 2. 改动说明
- **文件**：[`src/volur-lite-view/src/views/admin/cluster/tabs/NodesTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/tabs/NodesTab.vue)
- **改动详情**：
  - 在 `nodeColumns` 计算属性的第一列 `nodeName` 单元格渲染函数中，移除主节点专用的 `PLATFORM` `NTag` 渲染分支；
  - 保留 `hasSshAuth` 状态的 `SSH` 凭证标识标签。

## 3. 安全隐患说明与设计遵循
- **规范遵循**：严格遵守 [`view-Rules.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-skills/view-Rules.md)；
- **构建验证**：执行 `npm run build` 打包构建验证通过，无编译报错。

## 4. 影响范围
- 后台集群管理 -> 节点管理选项卡（`AdminClusterSettingsView` -> `NodesTab`）节点名称列展示效果。
