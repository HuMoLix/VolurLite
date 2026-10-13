# VCompetitionHeader 阴影升级与语言弹出菜单重构方案

## 1. 变更背景与目标
在竞赛全屏与沉浸式考场顶栏组件 [`VCompetitionHeader.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/competition/VCompetitionHeader.vue) 提取后，需要进一步优化其视觉层次与右侧操作区交互体验：
1. **顶栏视觉层级重构**：将顶栏由原本依赖硬边框（`border-b`）的方式升级为 Google Workspace 沉浸式轻量阴影（`box-shadow`），强化顶部浮层感；
2. **清理右侧冗余身份标签**：彻底移除右侧由 `candidateDisplayName` / `teamName` 渲染的个人及战队标签胶囊 Pill，消除不必要的信息堆叠；
3. **右侧操作区精简**：严格仅保留“语言切换”、“亮暗模式切换”以及“用户个人中心/登录”三大核心功能；
4. **语言切换菜单改造**：
   - 将语言切换按钮图标统一改为 `GlobeOutline`；
   - 参考 `VSettingCard.vue` 中的多语言切换交互，点击按钮后通过无边框微阴影的 `VCard` 浮层展示语言选项二级菜单（`zh-CN` 简体中文、`zh-TW` 繁體中文、`en-US` English），并根据当前选中的系统语言动态展示勾选标识（`CheckmarkOutline`）与主题高亮。

## 2. 核心架构与落地细节

### 2.1 Box-Shadow 阴影体系
- 在 `<header>` 元素上添加 `.v-competition-header`，并定义轻量自适应多阶阴影：
  - **日间模式**：`box-shadow: 0 1px 3px 0 rgba(60, 64, 67, 0.08), 0 1px 2px 0 rgba(60, 64, 67, 0.04);`
  - **夜间模式**：`box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.4);`
- 搭配 `backdrop-blur-md` 与 95% 半透明高斯模糊底色，保证滚动时的沉浸式平滑穿透。

### 2.2 语言切换浮层与 VCard 复用
- 使用 Naive UI `<n-popover>` 设置 `trigger="click"`、`placement="bottom"`、`raw` 与无箭头；
- 内部复用 `@/components/sidebar` 导出的通用卡片容器 `<VCard>`（配置 `class="v-header-language-card"` 与 `width="150"`）；
- 选项列表：
  - 支持 `zh-CN`、`zh-TW`、`en-US`；
  - 选中态：高亮 Google Blue 品牌色（日间 `#1a73e8`，夜间 `#8ab4f8`），并在右侧显示 `<CheckmarkOutline />`；
  - 悬浮态：轻微变灰交互底色；
- 触发器外层包裹 `VTooltip`，并在语言菜单展开时动态 `:disabled="isLanguageMenuShow"`，避免浮层与气泡发生视觉重叠。

### 2.3 右侧区域清理
- 移除原本插槽内的 `computedTeamName` 与 `candidateDisplayName` 对应 Pill DOM 元素；
- 移除未使用的 `PeopleOutline`、`PersonOutline` 图标引入，保持代码精炼。

## 3. 规范与质量验证
- **代码零注释原则**：所有 `.vue` 与组件代码均无行内解释性注释，详细逻辑完整记录于本方案文档中；
- **全量构建验证**：执行 `npm run build`，TypeScript 类型检查与 Vite 生产构建顺利通过（0 错误，0 警告）。
