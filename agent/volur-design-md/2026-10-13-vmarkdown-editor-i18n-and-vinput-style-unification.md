# VMarkdownEditor 国际化完善与 VInput 视觉风格统一设计记录

## 变更背景
为了保持平台前端基础输入组件间的一致性与严谨的 Google Material Design 3 / Workspace 设计语言规范，需对 `src/components/common/VMarkdownEditor.vue` 进行视觉系统对齐与国际化（i18n）全覆盖升级：
1. **多语言（i18n）闭环**：完善编辑模式、预览模式、格式工具栏所有快捷动作提示、状态栏光标与字数统计、快捷键提示及占位符的国际化语言包，覆盖 `zh-CN`、`en-US`、`zh-TW` 三地语言；
2. **样式语言统一化（对齐 `VInput.vue`）**：
   - **边框体系**：圆角统一为 `var(--v-input-radius, 8px)`，默认边框采用 `#d7d8da80`（亮色）与 `#262626`（暗色）；
   - **聚焦与 Hover 动效**：聚焦/focus-within 状态统一采用高雅平滑的 `#b7babd`（亮色）与 `#333`（暗色），彻底消除外发光投影环（`box-shadow: none !important`）；配合 `transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1)` 实现与 `VInput` 100% 一致的微交互；
   - **文字与排版规格**：统一采用 `Inter, sans-serif` 与代码等宽字体 `'Fira Code'`，字号 `14px`、行高 `21px`、字重 `400`；正文字色收敛至 `#1b1b1e`（亮色）与 `#d4d4d4`（暗色），光标颜色收敛至 `#2b2d31` / `#d4d4d4`，占位文字颜色收敛至 `#9aa0a6` / `#80868b`。

## 详细改动点

### 1. 多语言国际化键位补充 (`src/locales/{zh-CN,en-US,zh-TW}/common.json`)
在三地通用语言包中新增并对齐了 Markdown 编辑器核心词条：
- `markdown_edit`：“编辑” / “Edit” / “編輯”
- `markdown_preview`：“预览” / “Preview” / “預覽”
- `markdown_tip`：“支持 Markdown 语法排版” / “Supports Markdown syntax” / “支援 Markdown 語法排版”
- `markdown_empty_preview`：“暂无内容，请在编辑模式下输入 Markdown 文本” / “No content yet. Switch to Edit mode to write Markdown.” / “暫無內容，請在編輯模式下輸入 Markdown 文字”
- `markdown_placeholder`：“请输入内容，支持 Markdown 语法（标题、加粗、列表、表格、代码块等）...” / “Enter content, supports Markdown syntax (headings, bold, lists, tables, code blocks)...” / “請輸入內容，支援 Markdown 語法（標題、粗體、清單、表格、程式碼區塊等）...”
- `markdown_shortcuts_hint`：“快捷键: Ctrl/⌘+B 粗体 · Ctrl/⌘+I 斜体 · Ctrl/⌘+K 链接 · Tab 缩进”
- `markdown_cursor`：“行 {line}, 列 {col}” / “Ln {line}, Col {col}”
- `markdown_words`：“{count} 字符” / “{count} chars” / “{count} 字元”
- `markdown_lines`：“{count} 行” / “{count} lines”
- `markdown_action_*`：加粗、斜体、行内代码、引用、无序列表、有序列表、超链接、插入表格等操作的快捷提示。

### 2. VMarkdownEditor.vue 视觉风格重构 (`src/components/common/VMarkdownEditor.vue`)
- **边框与阴影规范**：
  - 容器外边框：采用 `border-radius: var(--v-input-radius, 8px) !important;`；
  - 边框色彩：默认亮色 `#d7d8da80`，暗色 `#262626`；内部工具栏与状态栏分割线同步匹配；
  - 聚焦表现：聚焦或 `:focus-within` 时，边框平滑过渡至 `#b7babd`（亮色）与 `#333`（暗色），`box-shadow: none !important`；
- **排版与文字规范**：
  - 文本区字体：`font-family: 'Fira Code', Inter, sans-serif; font-size: 14px; line-height: 21px; font-weight: 400;`；
  - 颜色收敛：正文字体 `#1b1b1e`，暗色 `#d4d4d4`；光标 `#2b2d31`，暗色 `#d4d4d4`；占位符 `#9aa0a6`，暗色 `#80868b`；
  - 状态栏与工具栏标签均采用 `Inter, sans-serif` 与相应层级颜色；
- **组件 Props 扩展**：
  - 在 `types.ts` 中的 `VMarkdownEditorProps` 中新增可选 `theme?: 'light' | 'dark'`，内部联动 Pinia Setup Store `appStore.theme` 自动派生 `effectiveTheme`。
- **纯粹代码规范**：代码保持零注释原则，逻辑高度自解释。

## 影响范围与验证
- `src/components/common/VMarkdownEditor.vue`
- `src/components/common/types.ts`
- `src/locales/zh-CN/common.json`
- `src/locales/en-US/common.json`
- `src/locales/zh-TW/common.json`
- 经过 `vue-tsc` 严格类型检测，上述组件与类型声明无任何类型或编译问题。
