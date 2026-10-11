# 2026-10-11 竞赛列表视图重构设计与施工记录

## 1. 变更背景与需求概述

针对 `src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue` 进行结构、层级及样式的细化重构：
1. **容器布局与内边距**：整体页面容器采用弹性布局（Flexbox），内边距（Padding）严格设置为 `48px 24px 24px 24px`；
2. **三段式纵向 Flex 结构（上中下）**：
   - **上部 (Top Header)**：
     - 修改标题元素为 `<h2>`；
     - 移除 `margin-top`，仅保留 `margin-bottom: 32px`；
     - 字体颜色指定为 `#1b1b1e`（暗色模式自适应柔和高对比灰白 `#e8eaed`）；
     - 移除 `<header>` 的 `padding-bottom`；
   - **中部 (Middle Body - Flex 纵向排列)**：
     - **第一行 (Filter Row)**：过滤条件（赛制标签分类切换及关键词搜索）；
     - **第二行 (List Row)**：比赛列表（竞赛卡片、空状态处理与分页控件）；
     - **第三行 (Prompt Notice Row)**：提示词纯文本及“导入竞赛”按钮：
       - 容器居中并设置 `max-width: 400px; padding: 32px 0; text-align: center;`；
       - 第一行文字 headline（“这里找不到您的竞赛？”）：字体颜色 `#1b1b1e`、字号 `14px`、外边距 `margin: 36px 0px 8px;`；
       - 下方描述文字（“此列表仅显示公开到 Volur Studio 的竞赛...”）：
         - `font-family: Inter, sans-serif;`
         - `font-optical-sizing: auto;`
         - `font-size: 12px;`
         - `font-weight: 400;`
         - `line-height: 16px;`
         - `color: var(--color-v3-text-var);`
         - `margin-bottom: 8px;`
       - 在提示词容器下方添加“导入竞赛”按钮（`<n-button>`），严格匹配各项尺寸与颜色要求，并去除点击时的发光与波纹效果。
   - **下部 (Bottom Footer)**：页面底栏组件 `AppFooter`。

---

## 2. 详细改动点与实现

### 2.1 标题与头部样式对齐
- `<header>` 移除 `pb-6`（内边距底部置 0）；
- `<h1>` 调整为 `<h2>` 并绑定 `.competition-title`；
- `.competition-title` 样式：
  - `margin-top: 0;`
  - `margin-bottom: 32px;`
  - `color: #1b1b1e;`
  - `font-size: 24px; font-weight: 700; line-height: 1.25;`
  - 暗色模式下响应式反色为 `#e8eaed`。

### 2.2 底部提示词排版
- `.competition-prompt` 居中容器：
  - `margin: 0 auto; max-width: 400px; padding: 32px 0; text-align: center;`
- `.competition-prompt-headline` 标题：
  - `color: #1b1b1e; font-size: 14px; font-weight: 500; margin: 36px 0 8px;`
- `.competition-prompt-desc` 描述：
  - `font-family: Inter, sans-serif; font-optical-sizing: auto; font-size: 12px; font-weight: 400; line-height: 16px; color: var(--color-v3-text-var, #5f6368); margin-bottom: 8px;`

### 2.3 “导入竞赛”按钮规范实现与去光晕优化
在 `.competition-prompt` 容器内添加 `<n-button class="competition-import-btn">导入竞赛</n-button>`：
- **边框与夜间主题**：
  - 亮色模式：`border-color: #d7d8da;`；
  - 暗夜模式：`border-color: #333;`（穿透至 `.n-button__border` 与 `.n-button__state-border`）；
- **文字与夜间主题**：
  - 亮色模式：`color: #2b2d31;`；
  - 暗夜模式：`color: #fcfcfc;`；
- **排版字阶**：
  - `font-size: 14px;`
  - `font-weight: 500;`
  - `line-height: 20px;`
  - `font-family: Inter, sans-serif;`
  - `letter-spacing: normal;`
- **Flex 布局与交互**：
  - `display: flex;`、`align-items: center;`、`justify-content: center;`、`margin: 0 auto;`；
  - `border-radius: 12px;`；
  - `cursor: pointer;`、`user-select: none;`、`white-space: nowrap;`；
  - `gap: 4px;`；
  - `padding: 0 12px;`；
  - `transition: background-color .15s ease-in-out, border-color .15s ease-in-out, color .15s ease-in-out, box-shadow .1s ease-in-out;`；
- **彻底去除点击与激活发光/光圈效果 (Click Glow & Ripple Removal)**：
  - 隐藏状态边框层：`:deep(.n-button__state-border) { display: none !important; }`，杜绝激活时的焦点与高亮描边；
  - 禁用点击波纹动画：`:deep(.n-base-wave) { display: none !important; animation: none !important; }`，彻底移除点击时向外扩散的水波纹光影；
  - 移除容器阴影与轮廓：在自身以及 `:focus`, `:active`, `:focus-visible` 状态下统一强制 `outline: none !important; box-shadow: none !important;`；
  - 锁定边框颜色与纯净度：在 `:focus` 与 `:active` 下穿透维持固定边框色（`#d7d8da` / `#333`）并锁定 `box-shadow: none !important;`。

### 2.4 纯粹代码与零注释合规
- 代码文件中严格无任何注释，所有技术实现方案统一记录于本文档中。

---

## 3. 影响范围与验证

- **改动文件**：
  - [`src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue)
- **编译与构建验证**：
  - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程 TypeScript 静态类型检查与生产环境打包构建 0 错误顺利通过。
