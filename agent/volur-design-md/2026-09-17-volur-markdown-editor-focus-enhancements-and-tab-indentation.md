# VolurMarkdownEditor 聚焦文本编辑增强与 Tab 缩进体系实施

## 1. 变更背景与组件关系辨析

### 1.1 组件辨析与系统实际使用说明
在系统代码库中，存在两个名称相近的 Markdown 编辑器文件：
1. **[`VolurMarkdownEditor.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurMarkdownEditor.vue)**：
   - **定位**：Volur Lite 平台自研核心设计系统（`volurEx`）的平台级 UI 组件；
   - **架构归属**：严格按照《Volur Lite 前端开发规范》（`view-Rules.md`）收敛在 `@/public/volurEx/components`，并通过 `volurEx` 根索引集中向全平台导出；
   - **角色**：承载全系统 Markdown 编辑、实时渲染预览、快捷键体系、Tab 缩进排版及深浅主题适配的**唯一核心实现载体**。
2. **[`MarkdownEditor.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/MarkdownEditor.vue)**：
   - **定位**：向后兼容代理垫片（Backward-compatibility Shim / Facade）；
   - **角色**：代码仅 8 行，通过 `<VolurMarkdownEditor v-bind="$attrs" />` 将所有 Props 和事件透传给平台组件 `VolurMarkdownEditor`。保留此垫片仅为防止历史模块或潜在第三方引用在解耦迁移过程中发生路径断链。
3. **系统真实运行结论**：
   - **系统当前正在真正使用的核心组件是 `VolurMarkdownEditor.vue`**。
   - 包括竞赛管理模块（`BasicInfoTab.vue`、`AnnounceTab.vue` 等）在内的现有所有活动视图，均已 100% 直接从 `@/public/volurEx` 引入并使用 `VolurMarkdownEditor`。

---

## 2. 功能升级与增强详情

针对用户聚焦（Focus）在输入框时的编辑体验，在 `VolurMarkdownEditor.vue` 中实现了 IDE 级别的专业 Markdown 编辑能力：

### 2.1 Tab 与 Shift+Tab 智能缩进系统
- **单光标/单行无选中状态**：
  - 按 `Tab`：在光标所在处插入配置长度的空格（默认 `tabSize: 2`）；通过标准 `document.execCommand('insertText')` 结合备用范围替换，完美保留浏览器的原生 Undo/Redo（`Ctrl+Z / Cmd+Z`）历史栈。
- **多行选区（Block Indentation）**：
  - 按 `Tab`：智能计算并为选区跨越的所有行的行首批量增加 `tabSize` 个前导空格，同时自适应扩展选区范围，保持文本块的高亮选中状态。
  - 按 `Shift + Tab`：批量减少选区跨越所有行的行首缩进（最多减少 `tabSize` 个空格），选区同步自适应收缩。
- **输入法安全保障（IME-Safe）**：
  - 拦截处理前置检验 `if (e.isComposing || e.keyCode === 229) return`，杜绝在中文拼音输入法选词/确认键时产生任何干扰。

### 2.2 回车智能继承缩进与 Markdown 列表延续
- **智能缩进保持**：按下 `Enter` 换行后，自动继承上一行的代码或文本前缀缩进空格。
- **列表智能延续与优雅退出**：
  - **任务列表（`- [ ] `）**：若上一行已输入内容，回车新行自动补全 `- [ ] `；若上一行为空任务项（连续按两次回车），自动清除当前行前缀退出列表。
  - **无序列表（`- `、`* `、`+ `）**：自动延续前导符号；空项回车自动清除符号并退出列表。
  - **有序列表（`1. `、`2. `）**：新行自动递增序号（例如上一行是 `1. `，下一行自动生成 `2. `）；空项回车自动退出。
  - **引用块（`> `）**：自动延续引用；空行回车自动退出。

### 2.3 符号成对闭合与选中文本包裹（Auto-Pairing & Wrapping）
- **选中文本包裹**：
  - 选中一段文字后，输入 `(`、`[`、`{`、`"`、`'`、`` ` `` 或 `*`，编辑器会自动将选中文本包裹起来（如选中 `word` 输入 `` ` `` 即变成 `` `word` ``）。
- **光标处自动闭合**：
  - 未选中文本时输入成对标点符号，自动补齐闭合符号并将光标置于中间。
  - 遇到右闭合符号且下一字符恰为对应符号时自动跳过（Skip-over）。
  - 在空括号/引号中间按 `Backspace` 会一次性删除整对符号。

### 2.4 快捷键与快速格式化工具栏
- **内置 Markdown 快捷键**：
  - `Ctrl/Cmd + B`：选中文本切换加粗（`**text**`），未选中时插入 `****` 并居中光标。
  - `Ctrl/Cmd + I`：选中文本切换斜体（`*text*`）。
  - `Ctrl/Cmd + K`：智能插入超链接 `[selected](url)`，并自动高亮选中 `url` 方便直接打字替换。
- **编辑态快捷工具栏**：
  - 在工具栏左侧模式切换右侧提供便捷操作按钮：加粗 `B`、斜体 `I`、行内代码、引用、无序列表、有序列表、超链接、插入表格模板。
  - 按钮绑定 `@mousedown.prevent`，点击按钮时不会导致多行输入框失焦，保持光标与选区状态连贯。

### 2.5 聚焦态环形阴影与底部状态栏
- **视觉聚焦反馈**：
  - 编辑器容器在输入框获得焦点时动态添加 `.is-focused` 类名，日间模式呈现 `#000000` 纯粹锐利的高亮边框与细致外阴影，夜间模式呈现 `rgba(255, 255, 255, 0.35)` 深曜炭黑微光。
- **底部状态栏（Status Bar）**：
  - **左侧**：未聚焦时提示默认说明，聚焦时动态切换为快捷键指引（`Tab 缩进 · Shift+Tab 反缩进 · 回车延续列表 · Ctrl+B 加粗 · Ctrl+K 链接`）。
  - **右侧**：实时计算并展示光标所在行号、列号（如 `行 3, 列 12`）、总字符数与总行数。

---

## 3. 影响范围与验证

- **修改文件**：
  - `src/volur-lite-view/src/public/volurEx/components/VolurMarkdownEditor.vue`
  - `src/volur-lite-view/src/locales/zh_CN/common.json`
  - `src/volur-lite-view/src/locales/en_US/common.json`
- **构建测试**：
  - 运行 `npm run build`，5.37s 编译打包通过（Exit Code 0），无任何 TypeScript 与样式错误。
