# 集群管理节点列表工具栏样式选择器失效与按钮紧凑问题修复

## 1. 变更背景
在集群管理视图的节点管理标签页 [`NodesTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/tabs/NodesTab.vue) 中，工具栏上的“刷新列表”、“存活检测”、“添加节点”（以及 K3s 集群下的“扫描同步 K3s”）按钮在渲染时挤在一起，失去了预期的 12px 间隔与左右两端对齐布局。

经过溯源排查发现：
- 该组件此前在重构复用统一表格组件 [`AppTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/AppTableCard.vue) 时，移除了外层的 `<div class="nodes-tab-container">`；
- 但底部的 `<style scoped lang="scss">` 样式块内依然保留了 `.nodes-tab-container` 的层级嵌套，导致内部的 `.nodes-toolbar` 和 `.toolbar-actions` 选择器无法匹配 DOM 树中的任何节点；
- 导致 `.toolbar-actions` 的弹性布局属性（`display: flex; gap: 12px;`）以及 `.nodes-toolbar` 的两端对齐属性（`display: flex; justify-content: space-between;`）完全失效。

## 2. 改动说明
- **文件**：[`src/volur-lite-view/src/views/admin/cluster/tabs/NodesTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/tabs/NodesTab.vue)
- **改动详情**：
  - 去除底部 Scoped SCSS 中已不存在的 `.nodes-tab-container` 外层选择器；
  - 将 `.nodes-toolbar`、`.toolbar-title` 与 `.toolbar-actions` 提到顶层规则中，确保与模板中的 `<div class="nodes-toolbar">` 及 `<div class="toolbar-actions">` 正确匹配并生效；
  - 工具栏操作按钮恢复 `display: flex; align-items: center; gap: 12px;` 弹性间距，标题与操作区恢复左右两端对齐。

## 3. 安全隐患说明与设计遵循
- **规范遵循**：严格遵守前端设计规范 [`view-Rules.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-skills/view-Rules.md) 关于 Scoped 样式封装与组件样式的整洁度要求；
- **副作用隔离**：仅修改当前组件作用域样式选择器层级，不引入全局污染，不破坏其他组件；
- **构建验证**：执行 `npm run build` 验证通过，编译无报错。

## 4. 影响范围
- 后台集群管理 -> 节点管理选项卡（`AdminClusterSettingsView` -> `NodesTab`）。
