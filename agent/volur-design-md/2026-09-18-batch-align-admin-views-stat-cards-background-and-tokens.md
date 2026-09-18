# 批量对齐后台管理模块统计卡片背景与 Volur 主题设计令牌规范施工记录

**施工日期**：2026-09-18  
**施工模块**：
1. `src/volur-lite-view/src/views/admin/competition/AdminCompetitionListView.vue`
2. `src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue`
3. `src/volur-lite-view/src/views/admin/cluster/AdminClusterSettingsView.vue`
4. `src/volur-lite-view/src/views/admin/container/AdminContainerView.vue`
5. `src/volur-lite-view/src/views/admin/image/AdminImageView.vue`
6. `src/volur-lite-view/src/views/admin/log/AdminLogView.vue`
7. `src/volur-lite-view/src/views/admin/task/AdminTaskView.vue`
8. `src/volur-lite-view/src/views/admin/email/AdminEmailView.vue`
9. `src/volur-lite-view/src/views/admin/system/AdminSystemView.vue`  
**参考规范**：`src/volur-lite-view/src/public/volurEx/components/VolurTableCard.vue`、`src/volur-lite-view/src/public/volurEx/styles/theme.scss`  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与对齐目标

在后台各管理页面中，普遍存在指标统计卡片（`.stat-mini-card`）及业务卡片（`.content-card`、`.overview-card`、`.settings-card`）的背景色与全局表格卡片（`VolurTableCard.vue`）不一致的问题。

### 核心根因分析 (Root Cause)
- 后台页面统一由 `AdminLayout.vue` 嵌套的 `<n-layout-content class="admin-content">` 承载，该容器会自动在根元素上注入 Naive UI 布局主题变量 `--n-color: #f8f9fa`（页面灰底色）。
- 各页面卡片样式此前编写为 `background-color: var(--n-color, #ffffff);`，由于 CSS 变量具有向下级联继承特性，直接继承了外层的 `--n-color`（即浅灰底），导致默认后备值 `#ffffff` 失效，产生卡片与背景融合泛灰的异常。
- 此外，部分页面边框使用了写死的 `rgba(0, 0, 0, 0.06)`，在夜间模式下无法正常自适应，圆角与文字排版变量（如 `#444444`）未完全遵循设计系统标准令牌。

---

## 2. 施工范围与改动细节

### 2.1 统计卡片与容器卡片样式统一对齐
在上述涉及的所有后台视图中，全面执行如下对齐：
1. **背景色**：
   - 彻底将 `var(--n-color, #ffffff)` 替换为 `var(--v-bg-card, #ffffff)`。
   - 日间模式下为纯白 `#ffffff`，黑夜模式下自适应提升为深曜炭黑高层级表面 `#151821`，与 `VolurTableCard` 保持绝对一致。
2. **边框与阴影规范**：
   - 统一边框为 `border: 1px solid var(--v-border-base, #e5e4e7);`；
   - 统一悬浮边框为 `border-color: var(--v-border-focus, #bbbbbb);`；
   - 统一卡片圆角为 `10px`，阴影使用 `var(--v-shadow-sm)` 与 `var(--v-shadow-md)`；
   - 优化过渡效果：`transition: background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.2s ease, transform 0.2s ease;`。
3. **文字颜色令牌对齐**：
   - 统计卡片中的标签文本 `.stat-label` 与页面描述 `.page-desc` 由写死值 `#444444` 规范为 `var(--v-text-desc, #444444)`，保证暗夜模式下 Slate-400 的舒适高对比度体验。

### 2.2 竞赛列表视图说明
- `AdminCompetitionListView.vue` 目前未设置顶部统计指标栏，主体由 `VolurTableCard` 承载。已对齐其 `.page-desc` 的文字令牌，表格卡片天然继承纯白背景规范。

---

## 3. 验证结果

- **构建编译**：在 `src/volur-lite-view` 目录下运行 `npm run build`，编译顺利通过（耗时 5.54s，0 错误）。
- **影响评估**：纯视觉样式层级升级，未变动任何业务接口调用与响应式状态。
