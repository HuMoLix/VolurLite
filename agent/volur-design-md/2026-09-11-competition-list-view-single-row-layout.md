# 竞赛列表视图（CompetitionListView）单行卡片布局重构与展示字段优化审计说明

## 1. 变更背景与需求说明
根据用户的最新指令：
> 「修改 @src/volur-lite-view/src/views/competition/CompetitionListView.vue 实现竞赛一行一个，要求能展示 竞赛Logo，竞赛标题，比赛时间以及报名队伍数量，删除复制短链接功能。」

在先前的迭代中，竞赛大厅采用的是多列响应式卡片网格布局，并且每个卡片带有复制短链接交互。为了提升竞赛大厅的条理感、浏览效率和赛事关键信息的聚焦度，本次任务对 [`src/volur-lite-view/src/views/competition/CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionListView.vue) 进行了重构，实现**一行一个竞赛**的横向条目卡片，集中凸显**竞赛Logo、竞赛标题、比赛时间以及报名队伍数量**，并彻底移除了复制短链接相关代码。

---

## 2. 核心架构与界面改动点

### 2.1 布局架构重构：一行一个条目式卡片
1. **容器重构**：
   - 废除原有的多列网格容器，改为采用纵向堆叠的 `.competition-row-list`；
   - 内部每个条目为 `.competition-row-card`，基于 Naive UI `<n-card :bordered="false" hoverable>` 构建；
   - 内部采用三段式 Flexbox 水平流式排版（`.row-card-inner`）：
     - **左侧**：竞赛 Logo / 封面海报容器；
     - **中间**：核心信息流（标题、赛制/状态徽章、赛程起止时间、报名队伍数、简介）；
     - **右侧**：操作直达区（查看竞赛详情按钮）。
2. **响应式自适应支持**：
   - 针对桌面端保持宽屏横向展示；
   - 添加媒体查询 `@media (max-width: 768px)`，在移动端屏幕下自动将三段横向结构优雅折叠为纵向堆叠卡片，确保各端最佳用户体验。

### 2.2 核心展示字段与视觉元素强化
1. **竞赛 Logo / Banner 封面区 (`.comp-logo-box`)**：
   - 设定为 170px × 100px 的标准圆角封套（桌面端）/ 140px 高度（移动端）；
   - 当竞赛配置有 `bannerUrl` 时，加载真实封面图片（`object-fit: cover`），支持悬浮放大微动效（`scale(1.04)`）；
   - 当竞赛未上传封面时，自动降级展示现代渐变占位容器，并居中渲染 `TrophyOutline` 奖杯图标。
2. **竞赛标题与赛制类型**：
   - 标题采用粗体（17px / font-weight 700），并排呈现赛制类型标签（CTF / THEORY / AWD / AWD+ / ISW 等，按一赛一制分类）与赛事运行状态标签（报名中 / 进行中 / 冻结 / 已结束）；
   - 支持长标题省略截断（`text-overflow: ellipsis`）。
3. **比赛时间 (`.comp-meta-row .meta-item`)**：
   - 结合 `CalendarOutline` 图标，格式化清晰呈现竞赛起止时间区间：`formatDateTime(startTime) ~ formatDateTime(endTime)`。
4. **报名队伍数量 (`.team-count-item`)**：
   - 结合 `PeopleOutline` 图标，显著呈现战队参赛规模：`comp.teamCount || 0 支战队`；
   - 采用主题绿（`#18a058`）加粗高亮强调。
5. **竞赛摘要描述**：
   - 保留两行文本截断展示（`-webkit-line-clamp: 2`），兼顾信息密度与视觉呼吸感。

### 2.3 功能精简与清理：彻底删除复制短链接
1. **UI 元素清理**：
   - 彻底移除了原卡片中的 `.slug-pill` 链接药丸胶囊；
   - 移除了卡片操作栏中的「复制短链接」按钮；
   - 移除了无用的 `CopyOutline` 图标导入。
2. **脚本逻辑清理**：
   - 删除了组件中用于写入剪贴板的 `copyShortLink()` 方法与状态，保持组件代码精简无冗余。
3. **主操作直达**：
   - 保留右侧「查看竞赛详情」按钮（`theme-primary-btn`），点击或点击整张卡片直达 SPA 独立详情页 `/c/:slug`。

---

## 3. 设计铁律与工程规范遵循 (GEMINI.md / view-Rules.md)

1. **主操作按钮规范**：
   - 查看详情按钮与搜索按钮统一采用 `.theme-primary-btn`，亮色模式下背景为 `#000000`，文字为 `#ffffff`，悬浮态为 `#222222`。
2. **描述文本规范**：
   - 副标题、标签描述文本、竞赛简介颜色严格遵循规范统一为 `#444444`，杜绝使用纯黑 `#000000`。
3. **TypeScript 严格类型**：
   - 全程强类型推导，无任何 `any` 类型使用。

---

## 4. 构建与验证结果

1. **前端工程构建与类型检查**：
   - 运行 `npm run build`（`vue-tsc --noEmit && vite build`）：
     - 类型推导 100% 通过（0 错误，0 告警）；
     - `CompetitionListView` 打包体积由 8.80 kB 进一步精简为 **7.98 kB** (gzip: 3.39 kB)；
     - 4.05s 构建成功。

---

## 5. 影响范围与审计结论

- **影响文件**：[`src/volur-lite-view/src/views/competition/CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionListView.vue)
- **审计结论**：本次重构完全契合用户的设计需求，竞赛列表由网格重构为清晰条理的单行卡片，精准展示了竞赛 Logo、标题、比赛时间与战队数量，并彻底清理了短链接复制功能，样式严格对齐 Volur Lite 视觉规范。
