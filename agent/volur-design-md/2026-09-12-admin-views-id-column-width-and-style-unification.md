# 管理后台九大管理视图 ID 列宽与样式统一规范与加固

## 1. 变更背景与问题定位

在 Volur Lite 管理后台九大视图（用户、竞赛管理、广播、集群节点、容器、镜像、日志、异步任务、邮件日志）中，业务主键通常为 64 位雪花算法 ID（Snowflake ID，长度为 19 位数字）。

此前排查发现，在不同管理视图中 ID 列的宽度和样式配置存在不一致现象：
1. **折行与截断缺陷**：
   - 部分视图（如 `AdminLogView.vue`、`AdminEmailView.vue`、`AdminTaskView.vue`）的 ID 列宽仅配置为 `140px`，甚至 `AdminCompetitionListView.vue` 原先配置为 `80px`；
   - 在 Naive UI `n-data-table` 默认具有左右各 `12px`（共 `24px`）单元格内边距的情况下，`140px` 列宽实际净宽仅有 `116px`。而 19 位等宽字符在 `12.5px` 的 `Fira Code` 字体下约需 `144.4px`，导致 ID 文本被强制折行到第二行；
   - 部分视图（如 `AdminTaskView.vue`）为了规避折行此前采用了 `.slice(-8)` 截取后缀展示的反模式，丢失了完整的 ID 信息。
2. **样式表现不统一**：
   - `AdminCompetitionListView.vue` 之前未应用等宽字体 `.id-text`；
   - 全局 `.id-text` 缺少 `white-space: nowrap;` 防御性样式声明。

---

## 2. 改动点汇总

### 2.1 全局公共表格样式加固 (`src/volur-lite-view/src/styles/table.scss`)
- 在 `.id-text` 与 `.mono-text` 规则中增加 `white-space: nowrap;`，从 CSS 基础层杜绝雪花算法 ID 在任何屏幕分辨率或单元格内边距变动下的被动折行。

### 2.2 九大管理视图 ID 列宽统一为 180px 与渲染标准化
1. **用户管理 (`AdminUserView.vue`)**：
   - 保持标准宽度 `width: 180`，使用 `id-text` 等宽字体渲染。
2. **日志审计 (`AdminLogView.vue`)**：
   - ID 列宽由 `140px` 调整为 `180px`，完美单行容纳 19 位雪花 ID。
3. **邮件日志 (`AdminEmailView.vue`)**：
   - ID 列宽由 `140px` 调整为 `180px`。
4. **广播管理 (`AdminBroadcastView.vue`)**：
   - ID 列宽由 `140px` 调整为 `180px`。
5. **容器管理 (`AdminContainerView.vue`)**：
   - 实例 ID 列宽由 `170px` 统一微调为 `180px`。
6. **异步任务 (`AdminTaskView.vue`)**：
   - ID 列宽由 `140px` 调整为 `180px`；
   - 移除 `.slice(-8)` 截断反模式，渲染完整 ID 并应用标准 `.id-text` 类。
7. **竞赛管理 (`AdminCompetitionListView.vue`)**：
   - ID 列宽由 `80px` 调整为 `180px`；
   - 补充 `render(row)` 使用标准 `id-text` 渲染。
8. **镜像管理 (`AdminImageView.vue`)** 与 **集群节点 (`NodesTab.vue`)**：
   - 确认主表格结构中无单独主键 ID 列（镜像主表首列为 Registry 镜像全名 `fullImageName: 320px`，节点表首列为节点名 `nodeName: 200px`），设计保持一致。

---

## 3. 安全隐患说明与影响范围

1. **信息完整性与运维体验**：
   - 完整显示 19 位雪花 ID 避免了管理员在排查任务、审计日志或关联容器时因 ID 被截断或折行导致误判；
   - 所有 ID 列统一步调与对齐宽度，增强了控制台的整体视觉质感。
2. **构建与类型验证**：
   - 前端执行 `npm run build`（`vue-tsc --noEmit && vite build`）顺利通过（0 错误，耗时 4.55s）。
