# VCompetitionCard 竞赛描述 Markdown 渲染与安全样式扁平化重构方案

## 1. 变更背景与需求概述

在新一代前端工程 (`volur-lite-view-NEXT`) 中，[VCompetitionCard.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/competition/VCompetitionCard.vue) 原先采用原生文本插值 `{{ competition.description || '-' }}` 展示竞赛描述。当服务端返回的描述内容包含 Markdown 标记格式（如标题、加粗、行内代码、引用及列表等）时，无法被正确解析渲染，而是直接以纯文本原始符号暴露给用户，影响视觉呈现与阅读体验。

参考经典前端工程中的 [CompetitionListView.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionListView.vue) 与 [markdown.ts](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/utils/markdown.ts)，需要在 `volur-lite-view-NEXT` 中构建安全、统一的 Markdown 解析机制，并在保持 Google Material Design 视觉规范的同时，解决 Markdown 块级元素在紧凑卡片容器中无法两行折叠截断的布局冲突。

---

## 2. 核心架构设计与改动点

### 2.1 依赖安装与安全工具函数收敛
- 在 `volur-lite-view-NEXT` 中引入 `marked`、`dompurify` 与 `@types/dompurify`；
- 新建 [src/utils/markdown.ts](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/utils/markdown.ts)，配置 GFM 规范与友好换行选项（`breaks: true`）；
- 使用 `DOMPurify.sanitize` 对解析后的 HTML 进行全量过滤，仅开放安全属性（`target`, `rel`），从根源杜绝 XSS 跨站脚本注入风险；
- 遵循代码极致纯粹与零注释铁律，保持工具类紧凑高内聚。

### 2.2 模版层重构
- 在 [VCompetitionCard.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/competition/VCompetitionCard.vue) 中，引入 `renderMarkdown`；
- 区分有值与空状态分支：有描述时使用带有 `comp-desc` 类的容器通过 `v-html` 渲染；无值时展示 `-`。

### 2.3 样式降维扁平化与 Google 配色体系对齐 (CSS Flattening)
由于 Markdown 解析会输出诸如 `<h1>~<h6>`、`<p>`、`<ul>`、`<ol>`、`<li>`、`<pre>` 等块级元素，若直接应用多行文本截断规则（`-webkit-line-clamp: 2`），块级元素独占行与自带的外边距会导致卡片布局高度撑开或截断异常。为此在 scoped SCSS 中设计了元素扁平化降维规则：
- **行内文本流降维**：将标题、段落、引用、列表项等统一强制设为 `display: inline`，继承行高与文字大小，并通过伪元素追加空格和分号，使多段落自然拼接为连贯文本流；
- **破坏性元素过滤**：对 `<img>` 与 `<hr>` 强制 `display: none`，防止大图或分割线撑爆卡片布局；
- **交互冲突防范**：将 `<a>` 标签设置 `pointer-events: none`，避免卡片内部链接点击拦截卡片整体的导航跳转；
- **Google Material Design 配色对齐**：
  - 正文颜色：亮色 `#3c4043`，暗色 `#bdc1c6`；
  - 强调文字（`strong/b`）：亮色 `#202124`，暗色 `#e3e3e3`；
  - 代码药丸（`code`）：等宽字体（`Fira Code`），亮色背景 `#f1f3f4`，暗色背景 `#303134`；
- **Vue SFC Scoped 编译陷阱规避**：严格规避 `:global(.dark) &` 被 Vue SFC 编译器词法分析器丢弃子选择器的已知陷阱，采用顶级 `html.dark .comp-desc` 声明，并在模板节点附加 `text-google-text-desc dark:text-google-text-darkDesc` 实现双重响应保障。

---

## 3. 安全隐患修复与风险防控说明
- **XSS 安全防御**：严禁未经净化的原始 HTML 直接渲染，DOMPurify 保证即使描述中含有恶意 `<script>`、`javascript:` 伪协议或 DOM 事件属性均会被全量清除；
- **布局防穿透**：通过行内降维与图片过滤，杜绝了恶劣/超长 Markdown 文本撑破卡片栅格布局的隐患；
- **类型安全**：完全兼容 TypeScript 严格类型检查，无 `any` 侵入。

---

## 4. 影响范围
- 新增文件：[src/volur-lite-view-NEXT/src/utils/markdown.ts](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/utils/markdown.ts)
- 修改文件：
  - [src/volur-lite-view-NEXT/package.json](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/package.json)
  - [src/volur-lite-view-NEXT/src/components/competition/VCompetitionCard.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/competition/VCompetitionCard.vue)
- 受益模块：赛事列表页（[CompetitionListView.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue)）及所有使用 `VCompetitionCard` 的展示视图。
