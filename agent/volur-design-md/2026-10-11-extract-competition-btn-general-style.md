# 2026-10-11 竞赛列表通用按钮样式提取与赛制筛选器适配设计与施工记录

## 1. 变更背景与需求概述

在 `src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue` 中：
1. 原先在底部 `.competition-prompt` 下定义的“导入竞赛”按钮（`.competition-import-btn`）具有高定制化的圆角矩形质感（圆角 `12px`、边框、过渡、深浅色模式适配及水波纹/光圈抑制）；
2. 顶部赛制筛选栏（“全部赛制”及各个赛制 Tab）原先使用胶囊药丸（`rounded-full`）样式，且在标签元素内内联大量 Tailwind 类名，缺乏统一设计语言与复用性；
3. 本次重构目标：
   - 提取 `.competition-prompt` 下的按钮样式，抽象并重构为该页面的通用按钮样式类 `.competition-btn`；
   - 支持原生 `<button>` 与 Naive UI `<n-button>` 双形态无缝兼容；
   - 顶部的“全部赛制”及其赛制分类 Tab 统一改用该 `.competition-btn` 样式，并配置 `.tab-count-badge` 徽标与 `.is-active` 激活态；
   - 空状态下的“重置筛选”按钮同步接入该通用样式。

---

## 2. 详细改动点与实现

### 2.1 样式提取：抽象 `.competition-btn`
将原 `.competition-import-btn` 的排版、尺寸与色彩规范收敛为页面通用按钮类 `.competition-btn`：
- **布局与几何尺寸**：
  - `display: inline-flex !important;`
  - `align-items: center; justify-content: center;`
  - `gap: 6px; height: 36px; padding: 0 12px;`
  - `border-radius: 12px;`
- **边框与文字色彩**：
  - 亮色模式：默认边框 `#d7d8da`，文字 `#2b2d31`，背景透明；
  - 暗黑模式：默认边框 `#333`，文字 `#fcfcfc`，背景透明；
- **交互与悬浮态**：
  - 亮色 Hover：`rgba(60, 64, 67, 0.04)`；
  - 暗黑 Hover：`rgba(255, 255, 255, 0.04)`；
- **激活态 (`.is-active`)**：
  - 亮色：Google Blue `#1a73e8` 实底色、白字、同色边框，Hover 为 `#1765cc`；
  - 暗黑：Google Blue Dark `#8ab4f8` 实底色、反色深黑字 `#202124`、同色边框，Hover 为 `#93bbf9`；
- **Naive UI 穿透兼容与光圈去除**：
  - 隐藏状态边框层：`:deep(.n-button__state-border) { display: none !important; }`；
  - 禁用点击波纹动画：`:deep(.n-base-wave) { display: none !important; animation: none !important; }`；
  - 内容容器样式同步穿透 `:deep(.n-button__content)`。

### 2.2 底部导入按钮轻量化解耦与双重边框消除
- `.competition-import-btn` 仅保留位置约束：`margin: 0 auto !important;`；
- 结构调整为 `<n-button class="competition-btn competition-import-btn" @click="handleImportClick">`；
- **双重边框修复 (Double Border Elimination)**：
  - 由于 Naive UI `<n-button>` 内部通过绝对定位的 `.n-button__border` 渲染边框，若 `.competition-btn` 根节点上声明了 `border: 1px solid #d7d8da !important;`，会导致根节点与内部边框层叠加产生 2px 的双重边框问题；
  - 针对该问题，在 `.competition-btn` 下补充 `&.n-button { border: none !important; }`，并在 `.competition-import-btn` 显式声明 `border: none !important;`，使 `<n-button>` 的边框完全由其内部 `.n-button__border` 统一接管，彻底消除边框叠加，同时保证原生 `<button>` 单层边框正常生效。

### 2.3 赛制 Tab 纯粹样式还原与多余激活背景移除
- 移除了强加在 `.competition-btn` 上的高饱和度蓝底激活样式（`background-color: #1a73e8 !important; border-color: #1a73e8 !important; color` 及其深色模式对应规则），使筛选按钮保持与底部“导入竞赛”按钮高度纯粹一致的 Google 风格透明背景与灰色质感边框；
- 移除 `.tab-count-badge` 的 `.is-active` 反白高亮规则，保持灰阶徽标层级。

### 2.4 第一行过滤栏垂直中线对齐深度分析与修复 (Alignment Fix)
- **不对齐原因分析**：
  1. **左侧滚动容器存在非对称内边距 `pb-1`**：左侧装载 Tab 按钮的容器声明了 `pb-1`（`padding-bottom: 4px`），而无 `pt-1`，导致在父容器 `md:items-center` 对齐时，左侧容器的几何中线被单边内边距拉低，使得内部按钮相较于父容器中线视觉明显上偏；
  2. **左右控件物理尺寸不一致**：左侧 `.competition-btn` 显式设置高度为 `36px`，而右侧 Naive UI `<n-input>` 默认 medium 尺寸外包高度为 `34px`，两者存在 `2px` 的尺寸落差；
  3. **缺乏统一盒模型与圆角收敛**：原生 `<button>` 在部分样式未重置时存在盒模型外延风险。
- **对齐修复方案**：
  1. 移除左侧 Tab 容器上的 `pb-1`，消除单边 4px 下外边距导致的几何中线偏移；
  2. 在 `.competition-btn` 上补充 `box-sizing: border-box !important;`；
  3. 为右侧搜索输入框配置 `.competition-search-input`，将其内部高度及 `--n-height` 严格对齐至 `36px`，圆角统一样式为 `12px`，实现左右两侧控件在同一水平中线像素级严格平齐。

### 2.5 赛制 Tab 样式独立重构为 `competition-filter-btn` 与角标清理
- **数量徽标清理**：删除 `tab-count-badge` 元素，不在按钮内显示比赛数量，保持极简视觉；
- **独立类名 `.competition-filter-btn` 提取与微交互精修**：
  - 基础阴影：`box-shadow: 0 1px 2px 0 #0000000d !important;`；
  - 默认边框：亮色 `border-color: #d7d8da80 !important;`，暗夜模式 `border-color: #262626 !important;`；
  - Hover 悬浮态：亮色 `background-color: #eaeaeb !important;`，暗夜模式 `background-color: #2a2b2d !important;`；
  - Selected 选中态：
    - 亮色：`background-color: #f4f4f5 !important; border-color: #d7d8da !important;`；
    - 暗夜：`background-color: #262626 !important; border-color: #333 !important;`；
    - 径向渐变圆点指示器：`padding-left: 26px !important; background-size: 100% !important; background-repeat: no-repeat !important; background-image: radial-gradient(circle at 16px center, var(--color-v3-text, currentColor) 3px, transparent 3.4px) !important;`，配合 `transition: padding-left .15s ease-in-out` 提供流畅展开动效。

### 2.6 空状态“重置筛选”按钮统一
- 原内联类名切换为使用 `.competition-btn`，统一页面整体视觉层级。

### 2.7 纯粹代码与零注释合规
- `CompetitionListView.vue` 中严格杜绝行内与解释性注释，所有方案与审计细节全量归档于本文档。

---

## 3. 影响范围与验证

- **涉及文件**：
  - [`src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue)
- **编译与构建验证**：
  - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程 TypeScript 类型校验与打包构建 0 错误通过。
