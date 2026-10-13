# VWriteupModal 接入通用组件 VModal 重构方案文档

## 1. 变更背景与动因

在 Volur Lite 前端项目（`src/volur-lite-view-NEXT`）中，CTF 赛事的 Write Up 上传弹窗 [`VWriteupModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/competition/ctf/VWriteupModal.vue) 原先直接采用 Naive UI 的 `<n-modal preset="card">` 实现，存在如下问题：
1. **预设卡片侵入性与样式穿透 Hack**：Card 预设强制引入卡片内部结构，必须通过 `:deep(.n-card__content) { padding: 0; }` 穿透覆盖；
2. **操作按钮布局脆弱性**：原先的“提交 Write Up”按钮被平铺在整体内容流末尾，在小屏幕或多条历史提交记录的情况下容易被挤出视窗，用户需要向下滚动后方可找到提交按钮；
3. **关闭按钮样式与定位不统一**：Card 自带的关闭按钮与全站规范（`VDrawer` / `VFeedback` 的 32px 圆形光晕背景按钮）存在差异；
4. **组件库规范演进**：在实现平台通用基础模态组件 [`VModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/VModal.vue) 后，需及时推动业务弹窗重构接入，保持平台技术栈纯净与统一。

---

## 2. 重构与实现详情

严格遵循**方案 A（标准模态窗架构）**进行改造：

### 2.1 基础模态组件替换
- 将 `<n-modal preset="card">` 全面替换为通用组件 `<VModal>`；
- 配置属性：`:width="680"`、`:max-width="'92vw'"`、`:radius="20"`；
- 事件绑定：保持 `@update:show="handleClose"` 与 `@close="handleClose"`，无缝承接 ESC 键响应与遮罩层关闭。

### 2.2 布局分层（Header / Container / Footer）
- **Header 头部层**：
  - 由 `VModal` 的 `title="提交 Write Up"` 自动接管，自带右侧安全间距与 1px 细线边框分隔；
  - 继承右上角固定 32px 圆形 Hover 光晕关闭按钮（亮色 `rgba(60, 64, 67, 0.08)` / 暗色 `rgba(255, 255, 255, 0.08)`），与 `VFeedback` 保持像素级一致。
- **Container 容器层**：
  - 提交说明卡片、提交结果卡片（含 `VTable`）以及文件上传与验证码卡片全部收敛在 `VModal` 默认插槽中；
  - 自动享有 `flex: 1 1 auto; min-height: 0; overflow-y: auto;` 容器保护，内容高度超出时平滑滚动，并去除了原先外层人工垫补的 `pb-5 sm:pb-6 pt-2` 冗余边距。
- **Footer 底部操作栏**：
  - 将“提交 Write Up”操作按钮移至 `<template #footer>` 插槽中；
  - 按钮稳固悬挂在弹窗右下角，无论说明或历史记录多长，提交按钮始终常驻可见，大幅提升用户体验。

### 2.3 依赖与样式清理
- **依赖瘦身**：从 `naive-ui` 导入中剔除 `NModal`，改由 `@/components/common` 统一导入 `VModal`；
- **消除样式穿透 Hack**：彻底删除组件底部的 `:deep(.n-card__content)` 选择器，代码更加纯净。

---

## 3. 验证与审计结论

1. **类型检查**：
   - 执行 `npx vue-tsc --noEmit`，结果为 0 错误（exit code 0），完全符合 TypeScript 强类型约束；
2. **纯粹代码与零注释铁律**：
   - `VWriteupModal.vue` 中无任何行内注释，所有重构背景、决策逻辑与技术选型均归档记录于本方案文档中。
