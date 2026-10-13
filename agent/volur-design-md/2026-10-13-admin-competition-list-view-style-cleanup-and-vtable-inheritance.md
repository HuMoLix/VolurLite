# 管理端竞赛列表视图表格样式精简与 VTable 样式继承重构

## 1. 变更背景与诉求

在 `src/volur-lite-view-NEXT` 项目的 `AdminCompetitionListView.vue`（管理端竞赛管理视图）中，竞赛列表表格（`VTable`）的部分列渲染定义了不必要的行内特殊样式与等宽字体规则：
1. **竞赛名称（title）列**：
   - 原渲染在外层 div 添加了 `cursor-pointer` 与点击跳转事件，在标题文本上添加了 `hover:text-google-blue dark:hover:text-google-blue-dark transition-colors` 强行定制 Hover 高亮效果；
   - 标题下方的副文本包含冗余的 `"Slug: "` 前缀字样；
   - Slug 副文本使用了 `font-mono` 与自定义字体字号定义，引入了 Fira Code 等宽字体，破坏了与平台统一 Inter 字体的整体协调；
2. **赛程时间（col_time / 比赛周期）列**：
   - 原渲染包含了 `font-mono`（Fira Code 等宽字体）以及 `text-[11px]`、`text-google-text-secondary`，强行改变了表格单元格原本的字重、字号与色系；
   - 导致与全平台后台表格设计系统脱节。

为了消除冗余的特殊 Hover 交互及等宽字体样式，使表格单元格完全、直接继承 `VTable.vue` 自带的设计系统（Inter 字体族、字号、字重与 Google 灰度色阶体系），并去除 slug 前缀标签，需要对该视图进行精简重构。

---

## 2. 变更内容与改动点

### 2.1 竞赛名称列重构

- **移除特殊交互与 Hover 样式**：
  - 移除标题容器上的 `cursor-pointer` 与 `onClick` 点击跳转事件；
  - 移除标题文本上的 `hover:text-google-blue dark:hover:text-google-blue-dark transition-colors`；
  - 竞赛跳转统一收敛于操作列（actions）中的“管理”按钮，保持数据表格单元格的纯粹文本呈现。
- **Slug 纯值展示与字体规范化**：
  - 移除副文本中的 `"Slug: "` 标签前缀，仅保留并纯粹展示 `row.slug` 字段值；
  - 移除 `font-mono` 类名，取消强制引入 Fira Code 等宽字体，使其继承 Inter 字体族；
  - 采用规范的 `text-xs text-google-text-secondary dark:text-google-text-darkSecondary` 呈现次级文本层次。

### 2.2 比赛周期（赛程时间）列直接继承 VTable 样式

- **移除 Fira Code 等宽字体与特定缩放**：
  - 移除容器上的 `font-mono`、`text-[11px]` 以及色彩类覆盖；
  - 保留清晰紧凑的行间距 `class="space-y-0.5"`；
  - 起止时间（`起: ...` 与 `止: ...`）完全、直接继承 `VTable.vue` 的单元格规范：
    - 字体族：`Inter, sans-serif`；
    - 字号与行高：标准表格 14px / 1.5 行高；
    - 颜色阶梯：亮色 `#3c4043` / 暗色 `#e8eaed`；
    - 字重：500。

---

## 3. 影响范围

- **修改文件**：
  - `src/views/admin/competition/AdminCompetitionListView.vue`
- **影响视图**：
  - 管理后台 - 竞赛管理页面 (`/admin/competitions`) 竞赛列表数据表格。
- **破坏性检查**：
  - 竞赛操作列的编辑管理、归档/解档、删除以及搜索过滤、分页等业务功能完全保持不变，无破坏性改动。
