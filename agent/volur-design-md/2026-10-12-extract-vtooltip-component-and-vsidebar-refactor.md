# VTooltip 通用组件提取与 VSideBar 工具提示重构方案

## 1. 变更背景与问题根因

在 `VSideBar.vue`（以及 `VUserCard.vue` 等侧边栏相关组件）中，存在大量基于 Naive UI `<n-tooltip>` 的提示气泡实现：

1. **模板代码冗余度高**：
   - 每次使用都需要显式声明 `trigger="hover"` 与 `:show-arrow="false"`（符合 Google Workspace 无尖角极简视觉设计规范）；
   - 普通按钮的单行文本提示必须嵌套两层模板插槽（`<template #trigger>` 与内部 `<span>`），代码冗长分散；
2. **样式约束碎片化与全局样式污染风险**：
   - 为避免侧边栏收起（折叠）状态下图标 Tooltip 受到 flex/外部盒模型影响而产生换行折字问题，历史代码在 `VSideBar.vue` 底部非 scoped `<style lang="scss">` 及 `src/styles/index.scss` 中编写了全局类名选择器 `.n-popover.n-tooltip:not(.user-card-tooltip)`；
   - 依赖 `:not(.user-card-tooltip)` 等脆弱排除选择器，在未来引入更多复杂多行气泡时难以扩展且容易产生样式冲突；
3. **缺乏统一全局复用能力**：
   - 平台其他业务视图（如赛事大厅、控制台、题库）缺少封装好 Google 规范主题色、边框、阴影、防折行特性的独立轻量 Tooltip 组件。

---

## 2. 核心设计与提取方案

### 2.1 样式特征分析（侧边栏收起与展开规范）
- **箭头规范**：对齐 Google Workspace（Gmail、Drive、Calendar）规范，彻底移除三角形尖角（`showArrow: false`）；
- **排版防换行与书写模式**：
  - 侧边栏折叠时提示文本必须保持单行水平自适应展示，强制 `white-space: nowrap !important; writing-mode: horizontal-tb !important; width: max-content !important; min-width: max-content !important; max-width: none !important; word-break: keep-all !important;`；
- **色彩与层级**：
  - **亮色模式**：背景纯白 `#ffffff`，字体 `#2b2d31`（避免刺激纯黑），边框 `1px solid #d7d8da80`，圆角 `8px`，阴影 `0 1px 3px 0 rgba(10, 13, 18, 0.1), 0 1px 2px -1px rgba(10, 13, 18, 0.1)`；
  - **暗色模式**：背景 Google Workspace 卡片炭黑 `#292a2d`，字体 `#bdc1c6`，边框 `1px solid #262626`，阴影 `0 1px 3px 0 rgba(0, 0, 0, 0.4), 0 1px 2px -1px rgba(0, 0, 0, 0.4)`；
- **尺寸档位**：
  - `small`：padding `4px 8px`，font-size `11px`，border-radius `6px`；
  - `medium`（默认）：padding `8px 12px`，font-size `12px`，border-radius `8px`；
  - `large`：padding `10px 14px`，font-size `13px`，border-radius `10px`。

### 2.2 双模式极简插槽与属性设计
`VTooltip` 针对高频与复杂两种场景设计了双模式插槽解析：
1. **高频极简模式**：直接传入 `content` prop，触发元素放入默认插槽：
   ```vue
   <VTooltip placement="right" :content="t('common.search')">
     <button class="action-icon-btn">...</button>
   </VTooltip>
   ```
2. **标准/自定义插槽模式**：
   - 支持显式 `#trigger` 插槽作为触发器；
   - 支持 `#default` 或 `#content` 插槽作为富文本提示内容；
   - 适用于用户信息卡片、多行图文等定制浮层。

---

## 3. 改动清单与实现细节

1. **新增通用类型定义**（`src/components/common/types.ts`）：
   - 定义 `VTooltipProps`（支持 `content`、`placement`、`trigger`、`showArrow`、`disabled`、`show`、`nowrap`、`maxWidth`、`size`、`theme` 等属性）；
   - 定义 `VTooltipSize`（`'small' | 'medium' | 'large'`）。
2. **新建核心组件**（`src/components/common/VTooltip.vue`）：
   - 基于 Naive UI `NTooltip` 封装，提供双模式插槽智能渲染；
   - 自动适配 App 全局主题与暗色类名，注入 `.v-tooltip-popover` 及 `.is-nowrap` 类名；
   - 暴露 `tooltipRef` 支持外部受控。
3. **公共导出与模块收敛**：
   - 新建 `src/components/common/index.ts` 集中导出 `VTooltip`；
   - 在 `src/components/sidebar/index.ts` 与 `src/components/sidebar/types.ts` 中重导出 `VTooltip` 及类型，保持向前兼容。
4. **全局样式升级**（`src/styles/index.scss`）：
   - 引入 `.v-tooltip-popover` 专用规则集，支持多尺寸档位与暗色高对比度覆盖；
   - 保留对原生 `NTooltip` 的兼容样式。
5. **重构 `VSideBar.vue`**：
   - 全面替换 12 处 `<n-tooltip>` 为 `<VTooltip>`：
     - 折叠/展开侧边栏切换按钮；
     - 主快捷操作按钮（收起态提示）；
     - 搜索按钮（展开态与折叠态 Dock 浮层）；
     - 通知按钮（展开态与折叠态 Dock 浮层）；
     - 设置按钮（展开态与折叠态 Dock 浮层）；
     - 主题切换按钮（展开态与折叠态 Dock 浮层）；
     - 用户卡片悬浮多行提示（`:nowrap="isCollapsed"`）；
     - 底部快捷动作按钮。
   - 清理 `VSideBar.vue` 底部非 scoped 重复样式块。
6. **重构 `VUserCard.vue`**：
   - 右上角关闭按钮的 `<n-tooltip>` 替换为 `<VTooltip>`。

---

---

## 4. 嵌套 Popover 点击穿透失效根因分析与修复 (Root Cause & Fix)

### 4.1 故障现象
在 `VSideBar.vue` 中重构替换后，点击展开态与折叠态下的“通知按钮”、“设置按钮”以及底部“个人信息栏（用户卡片）”时，对应的弹出卡片（Popover）无法展开。

### 4.2 根本原因剖析 (Deep Dive)
在 Naive UI 的内部设计中（`node_modules/naive-ui/es/popover/src/Popover.mjs` 第 350-363 行）：
- 外层弹出卡片基于 `<n-popover trigger="click">`，当它解析其 `#trigger` 插槽的子 VNode 时，会执行强标识判定：
  ```javascript
  if (triggerVNode.type?.__popover__) {
    triggerVNode.props.internalSyncTargetWithParent = true;
    triggerVNode.props.internalInheritedEventHandlers = [handlers, ...];
  } else {
    appendEvents(triggerVNode, ...);
  }
  ```
- Naive UI 的原生 `<n-tooltip>` 组件定义具备 `__popover__: true` 标识，因此当直接嵌套 `<n-popover>` 和 `<n-tooltip>` 时，外层 Popover 会通过 `internalInheritedEventHandlers` 将父级点击事件处理器委派给内层 Tooltip；
- 内层 Tooltip 探测到自身接收到了祖先级事件处理器（`internalInheritedEventHandlers.length > 0`），会自动将事件映射模式升格为 `trigger: "nested"`，进而将 `onClick` 合并绑定到最里层的真实 DOM 节点（如 `<button>` 或 `div.user-card`）；
- 但当使用自定义组件 `<VTooltip>` 时：
  1. `VTooltip` 未在组件静态定义上声明 `__popover__: true`，导致外层 `<n-popover>` 误将其当作普通 DOM 节点，直接把 `onClick` 挂载到了 `<VTooltip>` 的虚拟外层节点上（而该虚拟节点并不会直接渲染为包含事件捕获的真实 DOM）；
  2. `VTooltip` 未声明并透传 `internalSyncTargetWithParent` 与 `internalInheritedEventHandlers` 属性给底层的 `<n-tooltip>`，导致内部的 `<n-tooltip>` 无法感知到父级 Popover 的存在，其最底层的按钮因而丢失了触发展开 Popover 的点击回调。

### 4.3 修复措施
1. **注入组件级标识**：在 `VTooltip.vue` 的 `defineOptions` 中注入 `__popover__: true`；
2. **声明与透传内部事件链**：在 `VTooltipProps` 中补充 `internalSyncTargetWithParent?: boolean` 与 `internalInheritedEventHandlers?: any[]`，并在模板中完整绑定给 `<n-tooltip>`：
   ```vue
   :internal-sync-target-with-parent="internalSyncTargetWithParent"
   :internal-inherited-event-handlers="internalInheritedEventHandlers"
   ```
3. 经此修复，`VTooltip` 完美融入 Naive UI 的 Popover 祖先-派生事件总线，点击通知、设置与个人卡片时，事件无缝穿透并激活外层卡片展开。

---

## 5. 验证与审计

1. **类型检查与构建验证**：
   - 执行 `npx vue-tsc --noEmit`，TypeScript 0 错误；
   - 执行 `npm run build`，生产环境打包全部通过（耗时 8.38 秒）。
2. **规范遵守**：
   - 所有新建与修改的代码文件（`VTooltip.vue`、`types.ts`、`index.ts`、`VSideBar.vue`、`VUserCard.vue`、`index.scss`）严格遵循**纯粹代码与零注释铁律**。
