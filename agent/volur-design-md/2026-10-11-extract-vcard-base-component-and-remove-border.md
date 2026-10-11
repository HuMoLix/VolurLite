# VCard 基底组件提取与无边框重构审计记录

## 1. 变更背景与架构动机
在 `volur-lite-view-NEXT` 前端工程中，侧边栏底部停靠区（Bottom Dock）包含了两个核心悬浮卡片：
1. `src/components/sidebar/VSideBar.vue` 下的 Bottom Dock 用户卡片（`.dock-user-popover-card`）；
2. `src/components/sidebar/VNotice.vue` 下的站内通知卡片（`.dock-notice-popover-card`）。

经比对分析，两者在容器视觉层与交互结构上高度重合：
- 相同的圆角阶梯（`border-radius: 16px`）；
- 相同的内外边距（`box-sizing: border-box`, `padding: 14px 16px 12px`）；
- 相同的背景色彩与暗黑模式适配（亮色 `#FCFCFC` / 暗色 `#191919`）；
- 相同的 Google 阴影体系（亮色 `0 4px 20px rgba(60, 64, 67, 0.15), 0 1px 4px rgba(60, 64, 67, 0.1)`，暗色 `0 4px 20px rgba(0, 0, 0, 0.6), 0 1px 4px rgba(0, 0, 0, 0.4)`）；
- 相同的顶部标题/关闭按钮栏交互（`height: 28px`，`.card-close-btn` 28px 圆形微交互按钮，相同的 Hover/Active 透明度阶梯）；
- 两者均带有硬编码外边框（`border: 1px solid #d7d8da80` / 暗色 `border-color: #3c4043`）。

为提升组件化复用能力、收敛样式真实源并满足设计规范要求，本次重构将上述卡片容器基底独立提取为平台级通用卡片组件 `VCard.vue`，并在提取的基底中彻底移除原有硬编码 border。

---

## 2. 核心改动点

### 2.1 新增通用基底组件 `VCard.vue`
- 文件路径：`src/components/sidebar/VCard.vue`；
- 容器结构：
  - 提供响应式根容器 `.v-card`；
  - 默认属性：`border: none`（彻底移除原有 border），亮暗色阶梯背景与 Google 风格投射阴影；
  - 提供可选 `bordered` 属性开关（默认 `false`）；
  - 样式自动重置 Naive UI Popover 外层阴影与背景：`:global(.n-popover:has(.v-card))`；
- 插槽与结构支持：
  - `#header` / `#top`：顶部区域（内置预置 `.card-top-layer`，支持 `#header-left`、`#header-center`、`#header-right` 以及原生圆形关闭按钮与 `close` 事件）；
  - `default`：中间主体内容，完全透传；
  - `#footer` / `#bottom`：底部区域（内置 `.card-bottom-layer` 插槽支持）；
- 动态属性（`VCardProps`）：
  - 支持动态 `width`、`minWidth`、`maxWidth`、`height`、`padding`、`radius`、`gap`、`theme` 等，无缝兼容不同尺寸卡片。

### 2.2 导出与类型定义对齐
- `src/components/sidebar/types.ts`：新增 `VCardProps` 接口定义；
- `src/components/sidebar/index.ts`：集中向外导出 `VCard` 组件。

### 2.3 重构 `VSideBar.vue` 下的 User Card
- 模板改动：将 `.dock-user-popover-card` 根节点替换为 `<VCard class="dock-user-popover-card" :theme="currentTheme" :width="280" :gap="12">`；
- 样式精简：从 `.dock-user-popover-card` 中移除 `border: 1px solid #d7d8da80;` 与暗黑模式下的 `border-color: #3c4043;`；
- 容器适配：更新 `.n-popover:has(.v-card)` 规则。

### 2.4 重构 `VNotice.vue` 站内通知卡片
- 模板改动：将 `.dock-notice-popover-card` 根节点替换为 `<VCard class="dock-notice-popover-card" :theme="currentTheme" :width="340" :gap="10">`；
- 脚本改动：显式导入并使用 `VCard` 组件；
- 样式精简：从 `.dock-notice-popover-card` 中移除 `border: 1px solid #d7d8da80;` 与暗黑模式下的 `border-color: #3c4043;`。

---

## 3. 验证与影响范围

1. **类型检查与构建验证**：
   - 执行 `vue-tsc --noEmit && vite build`，编译与打包 100% 成功通过，无任何 TypeScript 类型冲突或模板语法缺陷。
2. **纯粹代码与规范检查**：
   - 所有新建及修改的代码文件（`.vue`、`.ts`）均严格执行零注释铁律；
   - 彻底移除了原卡片外层 border，视觉呈现更加通透纯净。
3. **兼容性与影响范围**：
   - `VSideBar` 底部用户卡片（280px）与通知卡片（340px）保持原有各自内容排版与交互逻辑；
   - 用户自定义插槽 `#user-card` 与 `#notice-card` 接口兼容性不受影响。
