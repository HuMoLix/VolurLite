# 竞赛列表页简介 Markdown 渲染与标题去粗样式优化

## 1. 变更背景与需求
- 在公开竞赛列表页 [`CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionListView.vue) 中，原实现直接通过 `{{ comp.description }}` 以纯文本形式展示竞赛简介；
- 随着后台支持 Markdown 编辑，主办方发布的竞赛简介可能包含丰富的 Markdown 语法（如 `#` 标题、`**` 加粗、代码、链接、列表等）；
- 用户需求明确要求：
  > 「在 @[src/volur-lite-view/src/views/competition/CompetitionListView.vue] 中的显示竞赛简介的地方也用markdown渲染，但是不要显示#的加粗等。」
- 列表页卡片属于紧凑的单行卡片视图，包含标题、类型与状态徽章、赛程时间等元数据，简介区域采用 2 行截断（`-webkit-line-clamp: 2`）；
- 若直接按全文 Markdown 渲染，`#`（`h1`~`h6`）标题会生成大字号、粗体、上下边距和下划线，不仅破坏卡片紧凑排版，还会与卡片的主标题造成视觉重复与冲突。因此需要对标题的粗体、字号及块级边距进行平铺降级处理。

---

## 2. 改动点汇总

### 2.1 模板与脚本改造 ([`CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionListView.vue))
1. **引入通用 Markdown 渲染工具**：
   ```ts
   import { renderMarkdown } from '@/utils/markdown'
   ```
2. **替换纯文本展示为安全 HTML 渲染容器**：
   ```html
   <div
     v-if="comp.description"
     class="comp-desc"
     v-html="renderMarkdown(comp.description)"
   />
   ```
   - 使用 `<div>` 替代原 `<p>` 容器，遵循 HTML5 规范，避免内部块级元素导致的非法嵌套及自动闭合问题；
   - 复用统一清洗过滤的 `renderMarkdown` 工具函数，经由 DOMPurify 彻底防御 XSS 攻击。

### 2.2 样式排版优化与 `#` 标题样式降级 ([`CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionListView.vue))
针对 `.comp-desc` 内渲染出的 Markdown 标签定制深度作用样式（`:deep`）：
1. **`h1` ~ `h6` 标题样式去加粗与平铺**：
   - `font-size: inherit`：字号与简介正文（13px）保持一致，取消大字号；
   - `font-weight: normal` 与 `* { font-weight: normal; }`：取消 `#` 标题默认的粗体加粗，恢复为常规字重；
   - `margin: 0; padding: 0; border: none;`：彻底清除标题的上下外边距及底部分割线；
   - `display: inline` 与 `&::after { content: ' '; }`：以行内流式文本呈现，并以空格分隔后续文本，保证与段落自然衔接并在两行截断机制下优雅折叠。
2. **正文段落与列表适配**：
   - `p`：`margin: 0; padding: 0; display: inline`，并带有空格间隔；
   - `strong`, `b`：保持 Markdown 常规加粗效果（`font-weight: 600`）；
   - `code`：等宽字体行内轻量代码块胶囊；
   - `ul`, `ol`, `li`：行内平铺排列，以分号分隔列表项；
   - `img`, `hr`：在 2 行简介缩略视图中隐藏（`display: none`），避免遮挡或撑大列表卡片高度；
   - `a`：保留下划线装饰，添加 `pointer-events: none`，防止富文本内部链接点击穿透干扰外层竞赛卡片的跳转事件。

---

## 3. 验证结论
- **TypeScript 静态检查与打包构建**：
  - 运行 `npm run build`（`vue-tsc --noEmit && vite build`）顺利通过，**0 错误、0 警告**；
- **视觉排版与功能验证**：
  - Markdown 语法（粗体、斜体、代码、行内链接等）正常解析渲染；
  - `#` 对应的一级至六级标题取消了加粗、大字号与外边距，以流式普通字重展示；
  - 2 行省略号截断（`-webkit-line-clamp: 2`）稳定运作，卡片布局规整美观。
