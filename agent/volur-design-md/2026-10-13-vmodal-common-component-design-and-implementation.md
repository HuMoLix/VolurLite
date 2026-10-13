# VModal 通用模态弹窗组件设计与实现文档

## 1. 变更背景与设计动机

在 Volur Lite 前端项目（`src/volur-lite-view-NEXT`）中，原先直接使用 Naive UI 的 `<n-modal preset="card">` 存在以下架构与交互局限：
1. **预设卡片结构限制**：Naive UI 的 `preset="card"` 强制注入内建卡片 DOM 结构，其头部、内容区和底部排版难以灵活使用纯粹的 Flex 纵向伸缩流布局，无法精细化控制各区域在不同屏幕分辨率下的滚动与溢出表现；
2. **关闭按钮样式与定位不统一**：Card 预设下的自带关闭按钮样式为 Naive UI 内部实现，与平台统一规范（如 `VFeedback.vue` 和 `VDrawer.vue` 中的 32px 圆形悬浮背景高对比按钮）存在视觉与交互差距；
3. **平台通用组件库解耦**：为了将通用交互能力收敛至 `@/components/common`，需要封装平台级高阶模态弹窗基础组件 `VModal.vue`，为全平台（CTF 题目弹窗、Writeup 提交、组队表单、通知详情等）提供具备 Google Material 设计阶梯、弹性伸缩布局及统一关闭按钮规范的模态对话框基座。

---

## 2. 核心架构与功能设计

### 2.1 底层基底与功能继承
`VModal.vue` 以 Naive UI 的基础 `<n-modal>` 为基底，不采用预设 `preset="card"`，充分发挥 `<n-modal>` 的底层能力：
- **挂载与传送门 (Teleport & Portal)**：原生支持 `:to` 属性将弹窗传送到 `body` 或指定容器；
- **遮罩层交互与动画**：具备透明度淡入淡出遮罩、统一的居中进出缩放过度动画，支持 `:mask` 及 `:mask-closable`；
- **键盘辅助交互 (Accessibility & ESC)**：原生支持 `:close-on-esc`，自动监听 ESC 键关闭，支持 `:auto-focus` 与 `:trap-focus` 焦点管理；
- **响应式层级与堆叠**：支持 `:z-index` 配置，与全平台多级浮层协调运作。

### 2.2 Flex 弹性布局三层划分
模态窗面板内部使用 Flex 垂直流式布局（`display: flex; flex-direction: column;`），严格划分为三个区域：
1. **`<header class="v-modal-header">` (头部层)**：
   - `flex-shrink: 0`，固定在顶部，不参与内部滚动；
   - 包含主标题 `title`、副说明 `description`，以及可自定义的 `#header` 插槽；
   - 预留右侧安全间距（`padding-right: 52px`），确保标题与右上角固定关闭按钮绝不发生重叠遮挡；
   - 采用纯净无边框（Borderless）一体化设计，顶部内边距为 `18px 20px 0`。
2. **`<div class="v-modal-container">` (容器/主体层)**：
   - `flex: 1 1 auto; min-height: 0; overflow-y: auto; overflow-x: hidden;`；
   - 当弹窗内容高度超出最大限制时，仅该容器层出现平滑滚动条，头部与底部保持视口锁定；
   - 同时支持具名插槽 `<template #container>` 与默认插槽 `<slot />`，满足多样化使用习惯；
   - 内置 Google Material 风格的细窄半透明滚动条，内边距为 `16px 20px`。
3. **`<footer class="v-modal-footer">` (底部操作层)**：
   - `flex-shrink: 0`，固定在底部，不参与滚动；
   - 默认采用 `justify-content: flex-end; gap: 12px;` 对齐操作按钮；
   - 仅在传入 `#footer` 插槽内容时按需渲染；
   - 采用纯净无边框（Borderless）一体化设计，内边距为 `0 20px 20px`。

### 2.3 右上角固定圆形背景关闭按钮
关闭按钮规范严格对齐 `VFeedback.vue`（底层 `VDrawer.vue`）的交互设计：
- **定位**：绝对固定于面板右上角（`position: absolute; top: 12px; right: 14px; z-index: 10;`）；
- **几何与形状**：尺寸为 `32px * 32px`，`border-radius: 50%`，纯圆形；
- **边框与背景**：默认状态背景透明、无边框，呈现精致的浅色图标；
- **悬停高亮光晕 (Hover Halo)**：
  - 亮色模式下悬停背景为 `rgba(60, 64, 67, 0.08)`，图标颜色加深为 `#202124`；
  - 暗色模式下悬停背景为 `rgba(255, 255, 255, 0.08)`，图标颜色加深为 `#e3e3e3`；
- **插槽扩展性**：支持 `#close` 具名插槽，允许调用方在特定业务场景完全自定义关闭按钮行为或样式。

---

## 3. 详细属性与类型定义 (VModalProps)

在 `src/components/common/types.ts` 中声明 `VModalProps` 强类型接口，并在 `src/components/common/index.ts` 中集中导出：

| 属性名 | 类型 | 默认值 | 详细说明 |
| :--- | :--- | :--- | :--- |
| `show` | `boolean` | `false` | 控制弹窗显示/隐藏（双向绑定 `v-model:show`） |
| `title` | `string` | `''` | 弹窗头部主标题 |
| `description` | `string` | `''` | 弹窗头部副说明文本 |
| `width` | `number \| string` | `560` | 弹窗面板宽度（数字默认解析为 px） |
| `maxWidth` | `number \| string` | `'92vw'` | 弹窗面板最大宽度 |
| `height` | `number \| string` | `undefined` | 弹窗面板固定高度 |
| `maxHeight` | `number \| string` | `'88vh'` | 弹窗面板最大高度 |
| `bordered` | `boolean` | `true` | 是否在头部底部与内容层之间渲染 1px 分割线 |
| `round` | `boolean` | `true` | 是否应用圆角 |
| `radius` | `number \| string` | `16` | 圆角大小，默认 16px（对齐 rounded-2xl） |
| `closable` | `boolean` | `true` | 是否显示右上角关闭按钮 |
| `mask` | `boolean` | `true` | 是否显示背景暗色遮罩 |
| `maskClosable` | `boolean` | `true` | 点击背景遮罩是否触发关闭 |
| `closeOnEsc` | `boolean` | `true` | 按下键盘 ESC 键是否触发关闭 |
| `autoFocus` | `boolean` | `true` | 打开时是否自动聚焦首个可用元素 |
| `trapFocus` | `boolean` | `true` | 是否限制焦点在弹窗内部循环 |
| `zIndex` | `number` | `undefined` | 自定义浮层层级 |
| `to` | `string \| HTMLElement` | `undefined` | 指定 Teleport 挂载的目标 DOM 容器 |
| `showHeader` | `boolean` | `true` | 是否显示头部（置为 false 时隐藏整个 `<header>`） |
| `theme` | `'light' \| 'dark'` | `undefined` | 强制主题模式，默认跟随全站响应式主题 |
| `panelClass` | `any` | `''` | 面板外层的附加 Class |
| `contentClass` | `any` | `''` | 容器内容层的附加 Class |
| `headerClass` | `any` | `''` | 头部层的附加 Class |
| `footerClass` | `any` | `''` | 底部操作层的附加 Class |
| `closeAriaLabel` | `string` | `''` | 关闭按钮无障碍 ARIA 标签 |

---

## 4. 颜色与暗黑模式阶梯规范

组件严格遵循 Google Material Design 规范：
- **亮色模式**：
  - 面板底色：纯白 `#ffffff`；
  - 边框颜色：Google Light Border `#dadce0`；
  - 阴影深度：`0 4px 24px rgba(0, 0, 0, 0.12)`；
  - 文本色调：主标题 `#202124`，副文本 `#5f6368`。
- **暗色模式 (`html.dark` & `.v-modal-panel.dark`)**：
  - 面板底色：Google Surface Dark `#1e1f20`；
  - 边框颜色：Google Dark Border `#3c4043`；
  - 阴影深度：`0 8px 32px rgba(0, 0, 0, 0.45)`；
  - 文本色调：主标题 `#e3e3e3`，副文本 `#9aa0a6`。

---

## 5. 验证与审计结论

1. **类型安全性检查**：
   - 执行 `npx vue-tsc --noEmit`，全面检查无任何类型报错（0 errors）。
2. **零注释铁律遵守**：
   - `VModal.vue`、`types.ts`、`index.ts` 中无任何冗余注释，所有设计方案与实现思路均收敛在本文档中。
3. **导出集中性**：
   - 已通过 `@/components/common` 统一暴露 `VModal` 与 `VModalProps`，可供后续业务视图开箱即用。
