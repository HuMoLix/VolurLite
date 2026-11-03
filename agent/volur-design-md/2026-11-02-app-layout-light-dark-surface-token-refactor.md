# AppLayout 表面底色设计令牌与 light-dark 现代化重构记录

## 1. 变更背景与动因

在 `volur-lite-view-NEXT` 体系中，应用主体框架组件 [`AppLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/layouts/primary/AppLayout.vue) 承载了 `/competitions` 等主要公开路由视图的渲染框架，其中 `.app-layout` 为外层全屏容器，`.app-main`（`<div class="app-main flex-1 flex flex-col min-w-0 h-full overflow-y-auto pb-16 md:pb-0">`）为主体滚动内容视口。

原实现中：
1. `.app-layout` 和 `.app-main` 的背景色硬编码为 `background-color: #fff` 或 `background: #fff`；
2. 内部通过 SCSS 嵌套历史选择器 `html.dark & { background-color: #191919; }` 来覆盖暗色样式。

随着工程整体全面转型为 W3C 原生规范的 CSS `light-dark()` 机制与统一设计令牌系统，硬编码色值以及基于 `html.dark` 的选择器覆盖已成为历史包袱。为保持设计令牌单一真实数据源（SSOT）并精简样式定义，将相关样式重构为 `--color-v1-surface` 原生驱动。

---

## 2. 变更内容与改动点

### 2.1 主骨架布局组件优化 ([`AppLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/layouts/primary/AppLayout.vue))

- **`.app-layout` 样式清理**：
  - 将硬编码的 `background-color: #fff` 替换为 `background-color: var(--color-v1-surface)`；
  - 彻底删除冗余的 `html.dark & { background-color: #191919; }` 样式块。
- **`.app-main` 样式清理**：
  - 将硬编码的 `background: #fff` 替换为标准属性 `background-color: var(--color-v1-surface)`；
  - 彻底删除冗余的 `html.dark & { background-color: #191919; }` 样式块。

---

## 3. 设计规范与原生机制保障

1. **统一设计令牌单一真实源**：
   在 [`vcss.scss`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/styles/vcss.scss) 中，`--color-v1-surface` 已明确定义为：
   ```scss
   --color-v1-surface: light-dark(#fff, #191919);
   ```
   同时配合全局 `color-scheme` 规则：
   - 亮色模式下解析为 `#fff`；
   - 暗色模式（由 `color-scheme: dark` 驱动）下自动解析为 `#191919`。
2. **零选择器特异性负担**：
   重构后不再产生编译后的复合高权重选择器 `html.dark .app-layout` 和 `html.dark .app-main`，样式纯净且加载性能更优。

---

## 4. 验证与审计

1. **类型检查与构建测试**：
   - 运行 `vue-tsc --noEmit`，严格类型检查 0 错误通过。
2. **规范审计**：
   - 遵守纯粹代码与零注释铁律，组件内部不保留任何行内备注，逻辑全部自解释并外置沉淀于本案例文档。
