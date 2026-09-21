# 管理端竞赛编辑页“返回竞赛列表”样式对齐与交互规范审计说明

## 1. 变更背景与问题定位

在管理后台竞赛详情管理与编辑界面 [`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 中：
- 顶部导航返回按钮使用了 Naive UI 的 `<n-button quaternary>` 组件类型；
- `quaternary` 类型按钮在鼠标悬停（Hover）、聚焦（Focus）与激活（Active）时，会默认应用 Naive UI 的背景遮罩底色（浅灰色块 `rgba(0, 0, 0, 0.06)`）以及可能随主题主色变色的文本高亮（紫色或带色调的变色）；
- 这与前台竞赛详情页 [`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue) 中干净纯粹的文本式返回按钮视觉规范不一致。

---

## 2. 改动点说明

对 [`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 的顶部返回按钮组件及其样式进行了 1:1 对齐重构：

1. **组件属性重构（Template）**：
   - 将 `<n-button quaternary size="small" class="back-btn" @click="backToList">` 替换为 `<n-button text size="small" class="back-btn" @click="backToList">`；
   - 按钮文案采用 `t('competition.back_to_list', '返回竞赛列表')` 统一国际化词条。

2. **样式层级规范对齐（SCSS）**：
   - 将 `.back-btn` 的样式完全对齐 [`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue) 标准：
     ```scss
     .back-btn {
       align-self: flex-start;
       padding: 0;
       color: #444444;
       background-color: transparent !important;
       background: transparent !important;
       transition: color 0.15s ease;

       &:hover,
       &:focus,
       &:active {
         color: #111111 !important;
         background-color: transparent !important;
         background: transparent !important;

         :deep(.n-icon) {
           color: #111111 !important;
         }
       }

       :deep(.n-button__state-border) {
         display: none !important;
       }
     }
     ```
   - **去除灰色背景**：强制背景（常规态与 `:hover`/`:focus`/`:active`）为 `transparent !important`，并隐藏 Naive UI 的状态边框；
   - **杜绝紫色变色**：在交互态下将文字与图标精准聚焦至炭黑主色 `#111111 !important`，杜绝任何主题主色（紫色/主品牌色）污染。

---

## 3. 安全隐患与规范符合性

1. **符合 Volur 核心视觉铁律**：日常描述文本保持 `#444444`，悬停交互平滑过渡至 `#111111`，全程无多余色块与边框抖动；
2. **纯粹多语言响应**：依托 `vue-i18n` 动态输出中英文对应文案；
3. **消除样式污染与回归风险**：通过 scoped 样式隔离与精准的 `:deep()` 穿透，确保不影响页面内其他按钮行为。

---

## 4. 影响范围与验证

- **影响文件**：[`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue)。
- **构建与类型验证**：
  - `npx vue-tsc --noEmit`：0 错误，严格类型推导通过。
