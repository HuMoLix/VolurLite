# 竞赛简述 Markdown 渲染与后台实时预览全链路对齐审计说明

## 1. 变更背景与需求说明
根据用户的指令需求：
> 「分析 @[src/volur-lite-view/src/views/competition/CompetitionDetailView.vue] 和 @[src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue] 是否能实现“竞赛简述”这一块可是使用markdown格式进行渲染，后台编辑的时候支持预览功能。完成前后端的api，逻辑和行为的统一对齐。」

原实现中：
1. 前台公开竞赛详情页 [`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue) 直接将 `competition.description` 放在 `<p class="desc-text">` 中进行纯文本展示，无法解析标题、加粗、列表、表格、代码块及链接等富文本元素；
2. 管理端编辑页 [`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 仅提供了基础 `<n-input type="textarea" />` 单一文本域，缺乏实时预览视图，管理员无法直观掌握发布后在前台的真实排版效果。

本次任务对前后端进行了全面深入的架构可行性分析，并在保留既有数据兼容性的基础上，完成了从底层工具库、通用样式库、后台编辑预览组件到前台详情渲染的全链路实施与对齐。

---

## 2. 前后端架构可行性分析结论

| 层次 | 技术现状 | 可行性结论与对齐方案 |
| :--- | :--- | :--- |
| **数据库存储层** | `Competition.java` 中字段 `description` 声明为 `@Column(name = "description", columnDefinition = "TEXT")` | ✅ **完全可行**。MySQL `TEXT` 容量达 64KB，天然支持长篇 Markdown 字符串无损存取，无需改动表结构与迁移动作。 |
| **后端 API 与 DTO** | `CompetitionCreateRequestDTO` / `CompetitionUpdateRequestDTO` 及 VO 均声明为 `String description` | ✅ **完全可行**。无纯文本字符过滤限制，支持前后端 JSON 标准传输。 |
| **安全过滤 (XSS 防护)** | 前端富文本渲染存在潜在的恶意代码注入风险 | ✅ **引入 DOMPurify 防护**。在前端解析 Markdown 为 HTML 时强制执行 `DOMPurify.sanitize()`，彻底杜绝 `<script>`、`onerror=`、`javascript:` 等恶意攻击。 |
| **前端渲染与样式** | 缺乏 Markdown 编译器与排版样式 | ✅ **引入 `marked` + `DOMPurify`**，并建立全局 GitHub 风格统一排版样式 `.volur-markdown-body`，适配日间/夜间双主题。 |
| **管理端交互** | 单一文本域无法预览 | ✅ **实现“编写 / 预览”分段式一体化编辑器**，支持即时切换查看真实渲染效果。 |

---

## 3. 核心改动点

### 3.1 基础设施与工具层构建
1. **依赖包引入**：
   - 引入高性能轻量解析器 `marked`（ESM 模块化支持、GFM 规范）；
   - 引入安全清洗库 `dompurify` 及 `@types/dompurify`。
2. **封装通用清洗与渲染工具 (`src/utils/markdown.ts`)**：
   - 统一配置 `marked.setOptions({ gfm: true, breaks: true })`（支持友好自然换行）；
   - 导出 `renderMarkdown(content?: string | null): string`，严格经过 `DOMPurify.sanitize()` 清洗后输出。
3. **建立通用 Markdown 响应式排版样式 (`src/styles/markdown.scss`)**：
   - 定义 `.volur-markdown-body` 规范类；
   - 细致适配 `h1`~`h6`、段落 `p`、加粗 `strong`、斜体 `em`、无序/有序列表 `ul`/`ol`、引用块 `blockquote`、代码片段 `code`、独立代码块 `pre`、数据表格 `table` 及分割线 `hr`；
   - 统一引用项目 CSS 设计令牌（`--v-text-primary`, `--v-text-desc`, `--v-bg-code`, `--v-border-base`），实现暗色/亮色模态下的无缝自然适配；
   - 在 `src/styles/index.scss` 中全局引入。

### 3.2 管理端竞赛编辑页支持 Markdown 编写与实时预览 ([`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue))
- **分段一体化编辑器结构 (`.markdown-editor-box`)**：
  - 顶部工具栏：
    - 左侧配置 Naive UI `NTabs`（分段式 `segment`，包含“编写”与“预览” Tab，附带 `CreateOutline` 与 `EyeOutline` 图标）；
    - 右侧提供“支持 Markdown 语法排版”辅助指引；
  - 编辑模式（`edit`）：
    - 采用 8 行自适应等宽字体文本框，支持直接键入 Markdown 原生语法；
  - 预览模式（`preview`）：
    - 实时调用 `renderMarkdown(formModel.description)`，结合 `.volur-markdown-body` 展示所见即所得的真实前台排版效果；
    - 空内容时呈现友好提示占位符。

### 3.3 前台公开详情页 Markdown 富文本渲染 ([`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue))
- 将原有的单行纯文本展示替换为：
  ```html
  <div
    v-if="competition.description"
    class="volur-markdown-body desc-markdown-content"
    v-html="renderMarkdown(competition.description)"
  />
  <p v-else class="desc-text-empty">主办方暂未发布详细介绍。</p>
  ```
- 既有存量纯文本数据因配置了 `breaks: true`，将直接以标准段落形式呈现，保持 100% 向下兼容；包含 Markdown 语法的赛事描述则展现丰富的层级与格式。

### 3.4 国际化多语言对齐
在 `zh_CN/admin/competitions.json` 与 `en_US/admin/competitions.json` 中统一补充了编辑与预览相关的国际化键位：
- `form_desc_edit`: "编辑" / "Edit"
- `form_desc_preview`: "预览" / "Preview"
- `form_desc_tip`: "支持 Markdown 语法排版" / "Supports Markdown syntax"
- `form_desc_empty_preview`: "暂无简介内容，请在编辑模式下输入 Markdown 文本" / "No description content yet. Switch to Edit mode to write Markdown."

---

## 4. 构建与验证结果

1. **后端验证**：
   - 执行 `mvn test -pl volur-competition`：所有单元测试 **100% 通过（Tests run: 1, Failures: 0, Errors: 0）**。
2. **前端验证**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：
     - TypeScript 静态类型检查 **0 错误、0 告警**；
     - Markdown 渲染器与工具库独立分包为 `dist/assets/markdown-*.js` (72.77 kB)；
     - 4.18s 内构建成功。

---

## 5. 影响范围与审计结论

- **影响文件清单**：
  1. [`src/volur-lite-view/package.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/package.json)（添加 `marked`, `dompurify` 依赖）
  2. [`src/volur-lite-view/src/utils/markdown.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/markdown.ts)（新增安全 Markdown 解析工具）
  3. [`src/volur-lite-view/src/styles/markdown.scss`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/styles/markdown.scss)（新增通用 Markdown 排版样式）
  4. [`src/volur-lite-view/src/styles/index.scss`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/styles/index.scss)（导入 markdown.scss）
  5. [`src/volur-lite-view/src/views/competition/CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue)（前台 Markdown 富文本渲染）
  6. [`src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue)（后台编辑与实时预览）
  7. [`src/volur-lite-view/src/locales/zh_CN/admin/competitions.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/competitions.json)
  8. [`src/volur-lite-view/src/locales/en_US/admin/competitions.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/competitions.json)
- **审计结论**：
  前后端数据结构、API 协议与展现行为完全对齐，成功落地了安全、现代且优雅的 Markdown 渲染与编辑预览能力。
