# Markdown 编辑器组件提取与全链路国际化 (i18n) 完善审计说明

## 1. 变更背景与需求说明
根据用户指令：
> 「提取 @[src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue] 中的 markdown 编辑器为一个 components 以便进行复用。并且完善 i18n。」

在前期工作中，我们在 [`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 中直接内联编写了 Markdown 编辑与预览面板。该实现存在以下可复用性与国际化不足：
1. **耦合度过高**：工具栏、Tab 切换、文本编辑区、渲染预览区与特定业务视图强绑定，后续在公告管理、题目描述编辑、Writeup 撰写等模块无法直接复用；
2. **样式与逻辑冗余**：样式与状态（如 `descEditorMode`）分散在视图层，增大了页面维护成本；
3. **i18n 未完全抽取**：工具栏提示、预览占位符等文本存在硬编码，缺少多语言支持。

本次变更将 Markdown 编辑器抽象为通用的受控组件 [`MarkdownEditor.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/MarkdownEditor.vue)，全面完善中英文双语字典，并重构了管理端竞赛编辑视图。

---

## 2. 核心架构设计与改动点

### 2.1 组件封装 (`src/components/common/MarkdownEditor.vue`)
新建高内聚、易扩展的 Markdown 编辑器公共组件：
- **双向绑定适配**：同时支持 `v-model:value`（Naive UI 常用风格）与标准 `v-model`（`v-model:modelValue`），在值变更时同步触发 `update:value`、`update:modelValue` 与 `change` 事件；
- **自适应与高度控制**：
  - `rows`（默认 8 行，控制输入框高度）；
  - `minHeight`（默认 200px，控制预览区最小高度）；
  - `maxHeight`（默认 480px，支持大文本滚动预览）；
- **完备的 Props 接口**：
  - `value?: string`
  - `modelValue?: string`
  - `placeholder?: string`
  - `rows?: number`
  - `minHeight?: string | number`
  - `maxHeight?: string | number`
  - `disabled?: boolean`
  - `showTip?: boolean`
  - `tipText?: string`
  - `emptyPreviewText?: string`
- **所见即所得与安全清洗**：
  - 预览模式下调用全局 [`renderMarkdown()`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/markdown.ts)，经过 DOMPurify 清洗后渲染为 `.volur-markdown-body` 排版；
  - 空状态自动渲染国际化占位提示文本；
- **视觉风格对齐**：
  - 文本编辑区采用等宽字体 `var(--v-font-mono)`（Fira Code / 等宽字体）；
  - 工具栏采用浅灰底色分段式 Tab，完全适配亮暗色主题。

### 2.2 国际化 (i18n) 字典完善
在公共语言字典中新增编辑器专用键位，覆盖简体中文与英文：

#### `src/locales/zh_CN/common.json`
```json
"markdown_edit": "编辑",
"markdown_preview": "预览",
"markdown_tip": "支持 Markdown 语法排版",
"markdown_empty_preview": "暂无预览内容，请在编辑模式输入 Markdown 文本",
"markdown_placeholder": "请输入 Markdown 内容..."
```

#### `src/locales/en_US/common.json`
```json
"markdown_edit": "Edit",
"markdown_preview": "Preview",
"markdown_tip": "Supports Markdown syntax",
"markdown_empty_preview": "No content to preview. Switch to edit mode and enter Markdown text.",
"markdown_placeholder": "Enter Markdown content..."
```

并在 `admin/competitions.json` 中配置了 `form_desc_placeholder` 的对应双语描述。

### 2.3 管理端竞赛编辑视图清理重构 ([`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue))
- **模板层替换**：
  ```html
  <markdown-editor
    v-model:value="formModel.description"
    :placeholder="t('admin.competitions.form_desc_placeholder')"
    :rows="8"
  />
  ```
- **脚本层清理**：
  - 引入 `MarkdownEditor from '@/components/common/MarkdownEditor.vue'`；
  - 移除了无用依赖导入：`NTab`、`CreateOutline`、`EyeOutline`、`renderMarkdown`；
  - 移除了局部冗余状态 `descEditorMode`；
- **样式层清理**：
  - 彻底删除了原有的 `.markdown-editor-box` 及其全部嵌套 SCSS 样式代码，样式完全由组件本身 scoped 管理，降低页面体积。

---

## 3. 安全与质量验证

1. **XSS 安全防护**：
   组件预览渲染统一走底层 `renderMarkdown` -> `DOMPurify.sanitize()` 安全防护链路，防止恶意注入；
2. **TypeScript 与打包构建验证**：
   执行 `npm run build`：
   - 零编译错误与警告（Exit Code 0）；
   - Vite 自动将 Markdown 渲染引擎拆分为独立 chunk `markdown-EjhoO3N8.js`，保证页面初始加载性能；
3. **多语言切换验证**：
   支持中英文无缝切换，动态响应 Tab 标签、操作提示与空内容提示。

---

## 4. 影响范围

| 文件 | 变更类型 | 影响范围与说明 |
| :--- | :--- | :--- |
| [`src/volur-lite-view/src/components/common/MarkdownEditor.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/MarkdownEditor.vue) | 新增 | 全局通用 Markdown 编辑器组件，供全平台编辑场景复用 |
| [`src/volur-lite-view/src/locales/zh_CN/common.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/common.json) | 修改 | 增加通用 Markdown i18n 中文字典 |
| [`src/volur-lite-view/src/locales/en_US/common.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/common.json) | 修改 | 增加通用 Markdown i18n 英文字典 |
| [`src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) | 重构 | 使用公共组件替换内联实现，移除冗余代码与样式 |
