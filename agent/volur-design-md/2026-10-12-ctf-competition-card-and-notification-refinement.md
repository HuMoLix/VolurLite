# CTF 竞技场赛题卡片与通知栏视觉及提示定位优化方案

## 1. 变更背景与架构目标

在 Volur Lite NEXT（`src/volur-lite-view-NEXT`）前端工程中，CTF 竞技场（`CtfCompetitionView.vue`）及其子组件 `CtfCompetitionChallengeCard.vue`、`VCompetitionNotification.vue` 与全局悬浮提示组件 `VTooltip.vue` 在视觉排版与悬浮交互上存在以下需要优化的问题：

1. **实时战报空状态提示与行高**：
   - 战报滚动条 `VCompetitionNotification.vue` 在无首血推送时的占位文案较长（原为“赛事进行中，等待战队首血战报...”），在窄屏与标准视口下显得冗长且不够简洁；
   - 需精简为“赛事进行中...”，同时需要将空状态文字行高调整为统一标准的 `line-height: 12px;`。
2. **通知栏边框样式清洗**：
   - `VCompetitionNotification.vue` 原包含内联的上下边框 `border-top: 1px solid #dadce0` 与 `border-bottom: 1px solid #dadce0`；
   - 在父级头部工具栏已具备底边框分割线的情况下，组件自身的上下边框造成了双重边框冗余，需彻底移除上下边框。
3. **赛题卡片 Tooltip 冗余与定位漂移缺陷修复**：
   - 原 `CtfCompetitionChallengeCard.vue` 在题目标题处包裹了 `VTooltip`，导致悬浮及点击整个卡片时标题提示频繁触发，造成视觉干扰；
   - 针对卡片底部 Top 3 战队（一、二、三血）的 Tooltip：此前 `VTooltip` 挂载在外层包含勋章图标且带有 `w-full` 的弹性容器上。当只有 1 个战队解出时，容器拉伸占据整个卡片宽度，导致 Tooltip 弹出时水平居中计算于卡片正中央的空白区域，严重偏离战队名称本身；
   - 需删除题目标题的 Tooltip，保留且仅在 Top 3 战队处使用 Tooltip；并将 Tooltip 触发节点精准锚定在战队名称（`span`）节点上，确保悬浮提示严格且精准定位在名称正上方；
   - 调整战队文字及全局 Tooltip 的基础字号为 `14px`、行高为 `14px`。
4. **赛题长标题居中与字号放大**：
   - 赛题卡片中央的题目长标题此前偏左对齐且字号偏小（`text-sm sm:text-base`），在三层流式卡片中的视觉视觉重心偏弱；
   - 需将题目标题设置为居中显示（`text-center`），并将字号提升至 `text-lg`（18px）加粗呈现。

---

## 2. 核心改动点与技术实现

### 2.1 战报通知组件优化 (`VCompetitionNotification.vue`)

1. **占位文案与行高**：
   - 将组件模板及各语言包中的 `notification_waiting` 统一更新为 `赛事进行中...`（简体中文 `赛事进行中...`、繁体中文 `賽事進行中...`、英文 `Competition in progress...`）；
   - 在 `.empty-state` 与 `.empty-text` 样式中显式配置 `line-height: 12px;`，与 `font-size: 12px;` 保持紧凑齐平对齐。
2. **边框清除**：
   - 在 `.v-competition-notification` 样式中移除 `border-top`、`border-bottom`，显式配置 `border: none;`；
   - 在暗黑模式 `:global(.dark) .v-competition-notification` 中移除对应的暗色边框颜色覆盖属性。

### 2.2 全局提示组件排版微调 (`VTooltip.vue`)

1. **字号与单行行高标准化**：
   - 将 `.v-tooltip-popover` 默认基准字号与行高从原先的 `font-size: 12px; line-height: 1.4;` 调整为 `font-size: 14px !important; line-height: 14px !important;`；
   - 相应微调 `size-small`（`11px` / `line-height: 11px;`）与 `size-large`（`16px` / `line-height: 16px;`）；
   - 在 `.is-nowrap .n-popover__content` 中显式继承 `font-size: inherit !important; line-height: inherit !important;`，确保单行无换行提示气泡的高度与字号统一。

### 2.3 赛题卡片重构 (`CtfCompetitionChallengeCard.vue`)

1. **移除题目标题 Tooltip 并居中放大**：
   - 移除包裹 `<h3>` 的 `VTooltip` 组件；
   - 外层标题容器配置 `text-center`；
   - 标题元素样式调整为：`text-lg font-bold text-center text-google-text-primary dark:text-google-text-darkPrimary line-clamp-2 leading-snug`。
2. **Top 3 战队 Tooltip 精准定位修复与字号对齐**：
   - 移除内部单项容器的 `w-full` 强行拉伸属性，调整为 `inline-flex items-center gap-1 min-w-0 max-w-full cursor-pointer text-[14px] leading-[14px]`；
   - 勋章图标尺寸同步提升为 `:size="14"`；
   - 将 `VTooltip` 下沉至精准包裹战队名称 `<span>`，Popper 锚点直接基于截断的战队名称文本节点计算，确保提示气泡无论处于单战队还是多战队场景，均精准出现在名称正上方；
   - 战队文本统一应用 `text-[14px] leading-[14px]`。

---

## 3. 安全隐患防范与架构边界

1. **零注释铁律与代码纯粹性**：所有修改的文件中严格杜绝任何解释性行内注释，技术方案完整外置于本文档；
2. **多语言单一真实源**：同步更新 `zh-CN`、`zh-TW`、`en-US` 三处语言包字典，杜绝国际化回退异常；
3. **Popper 定位稳定性**：将 `VTooltip` 直接挂载在文字节点上，避免了外层 Flex 容器自适应宽度变化对 Popper 定位计算的副作用。

---

## 4. 影响范围与验证结果

- **影响文件**：
  - `src/volur-lite-view-NEXT/src/views/competition/components/VCompetitionNotification.vue`
  - `src/volur-lite-view-NEXT/src/components/common/VTooltip.vue`
  - `src/volur-lite-view-NEXT/src/views/competition/components/CtfCompetitionChallengeCard.vue`
  - `src/volur-lite-view-NEXT/src/locales/zh-CN/competition.json`
  - `src/volur-lite-view-NEXT/src/locales/zh-TW/competition.json`
  - `src/volur-lite-view-NEXT/src/locales/en-US/competition.json`
- **验证结果**：
  - 执行 `npm run build`（Vite + vue-tsc）：成功编译打包（`built in 16.29s`），无 TypeScript 类型报错，无样式破坏。
