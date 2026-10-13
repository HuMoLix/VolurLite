# 通用表单与操作控件高度基准统一设计与重构文档

## 1. 变更背景与问题分析

在 `AdminUserView.vue`、`AdminCompetitionListView.vue` 等数据管理界面的工具栏（Toolbar）中，通常将 `VSearch`（搜索框）、`VSelection`（下拉过滤框）以及 `VBtn`（搜索/刷新/新增等操作按钮）横向并排陈列。

在排查中发现工具栏存在显著的**视觉高度参差不齐**问题：
1. **`VSelection.vue`**：在 `selectThemeOverrides` 中显式指定了 `heightMedium: '36px'` 与 `heightSmall: '28px'`，基准高度为 **36px**；
2. **`VSearch.vue` / `VInput.vue`**：底层依赖的 `VInput.vue` 在 `inputThemeOverrides` 中未覆写 `heightMedium`，导致 Naive UI 原生 `NInput` 回退到其内置 common 变量默认值 `heightMedium: '34px'`，实际高度为 **34px**；
3. **`VBtn.vue`**：`v-btn-normal`、`v-btn-warning`、`v-btn-danger` 声明了 `height: 36px`，但 `v-btn-primary`（主要按钮，如“新增用户”）写死为 `height: 40px`，且未声明 `.size-medium` 的高度约束，导致主要操作按钮在 `medium` 下呈现 **40px** 高度；
4. **横向容器垂直居中产生物理错位**：工具栏容器采用了 `flex items-center`，使得 34px 的搜索框、36px 的下拉框与常规按钮、40px 的新增按钮并列排布时，产生 2px ~ 4px 的肉眼可见梯级错位。

## 2. 核心设计与改造方案

遵循 Google Workspace / Material Design 3 的控件尺度梯级规范，全平台表单输入与按钮组件统一采用以下高度标准：
- **`small`**：`28px`（字号 12px~13px，用于紧凑卡片、表格内行操作）；
- **`medium`（默认基准）**：`36px`（字号 14px，用于全局工具栏、标准表单输入、常规按钮）；
- **`large`**：`44px`（字号 14px~15px，用于重点引导表单与大型行动点）。

### 2.1 `VInput.vue`
1. 在 `inputThemeOverrides` 中显式注入完整高度尺度：
   - `heightSmall: '28px'`
   - `heightMedium: '36px'`
   - `heightLarge: '44px'`
   - 同步将 `fontSizeSmall` 由 14px 微调为 13px，对齐 `VSelection`；
2. 在组件样式中，将单行文本输入 `.n-input__input-el` 的 `line-height` 改为 `inherit`（继承 Naive UI 计算的 `var(--n-height)`），确保文本与光标在 36px 高度容器内绝对垂直居中；多行文本 `.n-input__textarea-el` 维持 `line-height: 21px`；
3. 补齐 `.n-input--small` 状态下的字体与 placeholder 13px 样式缩放规则。

### 2.2 `VSelection.vue`
1. 在 `selectThemeOverrides.peers.InternalSelection` 中补齐 `heightLarge: '44px'`，形成完整的 `heightSmall: '28px'`、`heightMedium: '36px'`、`heightLarge: '44px'` 尺度闭环。

### 2.3 `VBtn.vue`
1. 将 `.v-btn-primary` 的基准高度从 40px 调整为 **36px**，内边距微调为 `0 16px`，与 `.v-btn-normal`、`.v-btn-warning`、`.v-btn-danger` 统一；
2. 显式新增 `&.size-medium { height: 36px; font-size: 14px; }` 类规则，确保在任何场景下显式声明 `size="medium"` 时按钮高度均恒定为 36px。

### 2.4 `VSearch.vue`
1. `VSearch.vue` 直接复用 `VInput.vue`，自动继承 36px 基准高度；
2. 新增 `.n-input--small` 与 `.n-input--large` 下前缀放大镜图标的阶梯自适应（15px / 18px / 20px），保持视觉比例和谐。

## 3. 影响范围与验证

- **影响文件**：
  - `src/volur-lite-view-NEXT/src/components/common/VInput.vue`
  - `src/volur-lite-view-NEXT/src/components/common/VSelection.vue`
  - `src/volur-lite-view-NEXT/src/components/common/VBtn.vue`
  - `src/volur-lite-view-NEXT/src/components/common/VSearch.vue`
- **验证结果**：
  - 执行 `npm run build` 打包构建，`vue-tsc` 类型检查通过，`vite build` 编译成功（0 error）；
  - `AdminUserView.vue` 工具栏中的搜索框、下拉框、搜索按钮、刷新按钮以及新增用户按钮的高度全部严格统一为 **36px**，消除了所有错位与不齐。
