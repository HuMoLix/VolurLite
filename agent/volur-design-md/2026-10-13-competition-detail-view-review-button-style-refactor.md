# 竞赛详情页“回顾复盘”按钮样式重构记录

## 1. 变更背景与目标
在竞赛详情视图（`CompetitionDetailView.vue`）中，当竞赛状态处于已结束（`ENDED`）时，原界面采用原生 `<button>` 配合浅灰边框次要按钮样式呈现“回顾复盘”操作。为统一新一代前端（`volur-lite-view-NEXT`）中核心业务行动号召（Call to Action, CTA）按钮的设计语言与交互规范，现将该按钮重构为平台通用按钮组件 `VBtn`，并应用其 `primary` 主题视觉风格。

## 2. 改动详情

### 2.1 涉及文件
- [`src/views/competition/CompetitionDetailView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/CompetitionDetailView.vue)

### 2.2 具体重构内容
- **组件替换**：将原有的原生 `<button>` 元素及其多余的 Tailwind 边框/背景样式类替换为 `<VBtn>` 通用组件；
- **样式与属性对齐**：
  - 指定 `clz="primary"`，亮色模式下统一采用 Google Blue（`#1a73e8`）实色胶囊背景与纯白文字，暗色模式下反色高对比（`#a8c7fa`）；
  - 继承保留 `:left-icon="RocketOutline"` 火箭图标前缀；
  - 绑定 `@click="enterArena"` 保持与原有复盘进入逻辑 100% 一致。

## 3. 安全与质量验证
- **构建测试**：执行 `npm run build`，Rollup / Vite 生产构建与 TypeScript 编译 0 错误（`built in 15.79s`）；
- **样式隔离与无冲突**：通过 `VBtn` 自带 Scoped SCSS 样式体系实现渲染，彻底移除内联冗余样式类，符合 Google Material Design 视觉铁律与零注释规范。
