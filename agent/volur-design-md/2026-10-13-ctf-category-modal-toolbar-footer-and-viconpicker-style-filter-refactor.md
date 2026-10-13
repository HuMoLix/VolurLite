# CTF 赛题分类模态框工具栏/底部按钮调整与 VIconPicker 样式对齐及筛选重构方案

## 1. 变更背景与需求概述

针对后台管理赛题方向分类模态框（`CtfCategoryModal.vue`）以及平台通用图标选择器组件（`VIconPicker.vue`），根据最新视觉设计规范和交互体验要求进行深度重构：
1. **工具栏视觉轻量化**：去除 `CtfCategoryModal.vue` 顶部添加分类输入区的外层左右内边距（`px`）与底色背景，保持与模态框主面板的通透对齐；
2. **样式严格 1:1 对齐 `VSelection`**：将 `VIconPicker.vue` 的边框颜色、边框 Hover/Focus 状态、文字颜色、占位符颜色、下拉指示箭头颜色、禁用状态底色与字体体系与 `VSelection.vue` 保持绝对像素级对齐；
3. **提升浮层层叠上下文（z-index）**：将 `VIconPicker.vue` 弹出图标选择卡片的 `z-index` 显式提升至 `3000`，确保在 `VModal`（默认 `z-index: 2000`）等各级模态框与遮罩层内弹开时不发生遮挡与层叠异常；
4. **组件复用与分类体系扩充**：
   - 弹出卡片内的搜索输入框改用平台通用组件 `VSearch.vue`；
   - 过滤分类标签改用平台通用按钮组组件 `VFilterBtnGroup.vue`；
   - 图标分类过滤项全面升级为 `all`、`fluent`、`ionicons4`、`ionicons5`、`antd`、`fa`、`material`、`tabler`、`carbon`；
5. **模态框操作流优化**：将 `CtfCategoryModal.vue` 中原本位于顶部的“添加分类”操作按钮移入模态框 `#footer` 底部，彻底删除原有冗余的“关闭”按钮，保持底部操作流清晰聚焦。

---

## 2. 核心架构设计与改动明细

### 2.1 CtfCategoryModal.vue 工具栏与底部重构
- **去除工具栏左右 Padding 与底色**：
  - 移除原 `p-3 rounded-lg bg-gray-50 dark:bg-[#202124] border border-google-border-light dark:border-google-border-dark`；
  - 替换为 `flex flex-wrap items-center gap-3 py-1`，去除水平内边距与容器背景；
  - 输入框宽度进行合理适配（分类名称 `w-48`、图标选择 `w-56`、简要说明 `flex-1 min-w-[200px]`）。
- **操作按钮流转**：
  - 从顶部表单栏移除 `VBtn` 添加分类按钮；
  - 在 `<template #footer>` 中移除单一的“关闭”按钮，置入带 `AddOutline` 图标与 `creatingCategory` loading 状态的主操作按钮 `VBtn`（`clz="primary"`）。

### 2.2 多图标库聚合与 VIcon.vue 动态解析引擎
- **扩充 8 大主流现代图标库**：
  - `@vicons/ionicons5` (Ionicons v5)
  - `@vicons/ionicons4` (Ionicons v4)
  - `@vicons/fluent` (Microsoft Fluent UI Regular 图标集)
  - `@vicons/antd` (Ant Design 图标集)
  - `@vicons/fa` (Font Awesome 图标集)
  - `@vicons/material` (Google Material Design Round/Outlined 图标集)
  - `@vicons/tabler` (Tabler 图标集)
  - `@vicons/carbon` (IBM Carbon 图标集)
- **高性能动态解析算法 (`resolveIcon`)**：
  - 支持带命名空间的前缀格式解析（如 `fluent:Globe24Regular`、`antd:CodeOutlined`）；
  - 支持直接名称快速 O(1) 字典检索；
  - 兼容 Ionicons 缺少 `Outline` 后缀时的自动回退；
  - 支持全集大小写不敏感容错查找。
- **导出规范**：
  - 导出 `resolveIcon`、`getAllIconNames` 与各库图标字典 `ICON_SETS`；
  - 废除历史废弃名称别名，遵循无妥协纯粹重构规范。

### 2.3 VIconPicker.vue 视觉与交互全面重构
- **样式严格对齐 `VSelection.vue`**：
  - **日间模式**：
    - 边框：`1px solid #d7d8da80`，Hover `1px solid #d7d8da80`，Focus/Active `1px solid #b7babd`，无 box-shadow；
    - 文本颜色：正文 `#1b1b1e`，占位符 `#9aa0a6`，箭头 `#565b61`；
    - 禁用状态：底色 `#f5f6f7`，文字 `#80868b`，占位符 `#bdc1c6`，箭头 `#9aa0a6`。
  - **夜间模式**：
    - 边框：`1px solid #262626`，Hover `1px solid #262626`，Focus/Active `1px solid #333`；
    - 文本颜色：正文 `#d4d4d4`，占位符 `#80868b`，箭头 `#8c8c8c`；
    - 禁用状态：底色 `rgba(255, 255, 255, 0.04)`，文字 `#73777d`，占位符 `#55595d`，箭头 `#55595d`。
  - **尺寸与排版**：
    - `size-small`：高度 28px、字号 13px、内边距 0 8px；
    - `size-medium`：高度 36px、字号 14px、内边距 0 10px；
    - `size-large`：高度 44px、字号 14px、内边距 0 12px；
    - 字体强制采用 `Inter, sans-serif`，圆角继承自 `--v-icon-picker-radius`（默认 8px）。
- **层叠上下文与模态框边界溢出防护（z-index 与 to="body"）**：
  - **`position: static` 告警分析与修复**：在 CSS 规范中，未显式声明定位的普通块级容器默认为 `position: static`，直接设置 `z-index` 无效。在 `.picker-panel-card` 上显式添加 `relative`（`position: relative`），确保卡片自身的 `z-index` 真正生效；
  - **模态框溢出剪裁（截断）根因分析**：Naive UI 的 `NPopover` 内部采用 `useAdjustedTo` 逻辑，当检测到上下文处于 `NModal` 内部时，若未显式指定 `to`，会默认将 Popover Teleport 挂载到模态框的 DOM 节点（即 `.v-modal-container`）内。而 `.v-modal-container` 配置了 `overflow-y: auto; overflow-x: hidden;`，当模态框高度较小或空间不足时，浮层就会被模态框边界剪裁截断；
  - **显式 Teleport 逃逸方案**：在 `VIconPickerProps` 中新增 `to?: string | boolean | HTMLElement` 并默认设定为 `'body'`，强制 `<n-popover :to="to">` 将浮层直接 Teleport 挂载至根页面 `document.body`，彻底摆脱模态框容器的 overflow 限制，实现真正的全局视口自由漂浮。
- **弹出卡片底部精简化**：
  - 彻底删除了卡片底部的已选图标名称字样徽章（如 `FingerPrintOutline`）与清空按钮，浮层内仅保留图标网格与 `VPagination`，使弹出卡片更加轻量紧凑；
  - 清空图标动作完全收敛至输入框触发器本身的 Clear Icon（`clear-btn`），消除重复入口。


### 2.4 CtfCategoryModal.vue 赛题分类表格图标展示精简
- **表格“图标”列去除冗余文本**：
  - 移除了分类列表中图标右侧的“`GlobeOutline`”等文字描述代码块（`<code class="text-xs ...">`）；
  - 将图标列宽由 `140px` 紧凑调整为 `80px`（`align: 'center'`），单元格内仅居中展示对应尺寸的图标组件 `VIcon`（`size: 20`），使表格视觉更为紧凑利落。

### 2.5 CtfImportModal.vue 按钮规格与操作流精简
- **组件规范审查**：确认 `CtfImportModal.vue` 内部已 100% 采用平台通用按钮规范组件 `VBtn`（引入自 `@/components/common/VBtn.vue`），杜绝了第三方组件混用；
- **按钮规格标准化**：
  - 将顶部栏的“下载题目包模板”按钮尺寸由 `tiny` 调整为标准中等高度 `medium`（36px）；
  - 将底部操作栏的“确认导入”主操作按钮尺寸由 `small` 调整为 `medium`（36px）；
- **操作流精简化**：
  - 彻底移除了底部冗余的“取消”按钮，遵循平台模态框通过顶部统一 Close 动作关闭的精简体验；
  - 仅在导入成功展示解析报告时提供 `medium` 尺寸的“关闭”按钮供用户确认后退出。

---

## 3. 规范遵守与审计检查

1. **纯粹代码与零注释铁律**：所有修改与新建的 `.vue` 与 `.ts` 代码文件（`CtfCategoryModal.vue`、`VIconPicker.vue`、`VIcon.vue`、`iconMap.ts`、`types.ts`、`index.ts`）完全杜绝了行内注释与解释性备注；
2. **单一真实源**：统一依赖 `VSelection` 标准样式体系与 `VIcon` 核心解析，无任何兼容桥接或重复样式冗余；
3. **全量构建验证**：全项目通过 `vue-tsc --noEmit` 严格类型检测与 Vite 生产构建。

