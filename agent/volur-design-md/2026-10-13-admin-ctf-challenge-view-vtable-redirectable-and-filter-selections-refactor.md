# 管理端 CTF 赛题列表视图 VTable 链接跳转与工具栏维度过滤重构

## 1. 变更背景与需求说明

在 `src/volur-lite-view-NEXT` 项目的管理后台 CTF 赛题管理列表页面（`AdminCtfChallengeView.vue`）中，存在以下样式脱节与交互过滤不完善的问题：
1. **表格字体与样式未遵循平台统一规范**：
   - 赛题名称列原实现使用了自定义标题粗体加粗与强行指定的色彩类，镜像副文本使用了 `font-mono`（Fira Code 等宽字体）与 `text-[11px]`；
   - 创建时间列使用了 `font-mono` 与 `text-xs text-google-text-secondary dark:text-google-text-darkSecondary` 自定义规则；
   - 引入了 Fira Code 等宽字体破坏了后台列表统一 Inter 字体族与 Google Workspace 视觉层级；
   - 赛题标题缺少平台统一的表格可点击跳转样式，用户无法直接通过点击标题跳转至题目编辑页面。
2. **顶栏工具栏缺乏难度与类型筛选维度**：
   - 顶栏仅有单一的“全部分类”下拉框，且外层容器宽度硬编码为 `sm:w-52`（208px），视觉上过于宽大，未对齐 `AdminCompetitionListView.vue` 的双列响应式紧凑栅格；
   - 缺少对“难度等级”（入门、简单、中等、进阶、困难）和“题目类型”（静态附件、动态容器）维度的筛选。

---

## 2. 详细改造实施点

### 2.1 样式清洗与 VTable 规范继承

- **赛题名称列（redirectable 样式）**：
  - 赛题主标题绑定平台统一定义的 `.redirectable` 类名（来自 `VTable.vue`，自适应 `--color-v3-text-link`，Hover 显示下划线，字重 500）；
  - 绑定 `onClick` 事件，点击后通过路由导航直接跳转至 `/admin/challenges/ctf/${row.id}/edit`；
  - 标题下方的镜像副文本（`row.imageName`）彻底移除 `font-mono` 等宽字体，对齐 Slug 规范，设置 `style: { color: 'var(--color-v3-text-var)', fontSize: '12px' }`。
- **创建时间列**：
  - 彻底移除 `font-mono` 与自定义字体颜色类名，直接返回格式化后的本地化日期时间字符串，完全继承 `VTable.vue` 单元格的统一字号（14px）、行高（1.5）与 Google 灰度色阶体系。
- **题目类型与难度展示纯化**：
  - 类型列通过 `getTypeText` 统一展示“静态附件”或“动态容器”，对齐平台中文语义，消除原始全大写枚举字符串的生硬感；
  - 难度列通过 `getDifficultyText` 保持纯中文等级（入门、简单、中等、进阶、困难）。

### 2.2 顶栏工具栏多维度过滤下拉框集成

- **响应式栅格布局**：
  - 参考 `AdminCompetitionListView.vue` 与 `AdminRiskIpView.vue`，将下拉框容器由原本独立的固定宽块重构为聚合响应式栅格：
    ```html
    <div class="grid grid-cols-2 sm:grid-cols-3 gap-2.5 w-full sm:flex sm:w-auto shrink-0">
    ```
  - 小屏端（`< sm`）采用 `grid grid-cols-2`，第三个下拉框设置 `col-span-2 sm:col-span-1` 优雅铺展；
  - 桌面端（`>= sm`）切换为紧凑弹性布局 `sm:flex sm:w-auto`，所有下拉框宽度统一收敛为规范的 `sm:w-36`（144px）。
- **新增维度状态与过滤逻辑**：
  - 新增 `filterDifficulty`（全部难度、入门、简单、中等、进阶、困难）；
  - 新增 `filterType`（全部类型、静态附件、动态容器）；
  - 增加 `matchDifficulty` 与 `matchType` 判定函数，对服务端返回的列表进行实时客户端多条件组合过滤；
  - `handleReset` 联动重置 `selectedCategoryId`、`filterDifficulty`、`filterType` 及关键词输入，恢复初始分页并重新请求。

### 2.3 语言包国际化补齐

- 在 `src/locales/zh-CN/admin.json`、`src/locales/en-US/admin.json` 与 `src/locales/zh-TW/admin.json` 的 `challenges` 节点下增补多语言键：
  - `filter_category`: "全部分类" / "All Categories" / "全部分類"
  - `filter_difficulty`: "全部难度" / "All Difficulties" / "全部難度"
  - `filter_type`: "全部类型" / "All Types" / "全部類型"

---

## 3. 影响范围与回归验证

- **影响文件**：
  - `src/views/admin/challenge/AdminCtfChallengeView.vue`
  - `src/locales/zh-CN/admin.json`
  - `src/locales/en-US/admin.json`
  - `src/locales/zh-TW/admin.json`
  - `agent/volur-design-md/2026-10-13-admin-ctf-challenge-view-vtable-redirectable-and-filter-selections-refactor.md`
- **构建与类型验证**：
  - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程 0 报错通过。
