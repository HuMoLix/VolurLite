# VolurMarkdownEditor 组件提取与光标及对齐细节修复

## 1. 变更背景

在后台竞赛管理页面（`AdminCompetitionEditView.vue`）下属的“基本信息”选项卡（`BasicInfoTab.vue`）与“赛事公告”选项卡（`AnnounceTab.vue`）中，原先直接在业务组件内部编写了用于“竞赛简述”和“公告内容”的简易 Markdown 编辑文本框。

该编辑器存在以下若干视觉与交互体验层面的问题：
1. **未收敛至平台组件库**：未按平台规范收敛至 `@/public/volurEx/components`，导致代码复用成本高且风格不统一；
2. **工具栏左右未对齐**：工具栏左侧“编辑 / 预览”的分段 Tab 按钮与右侧“支持 Markdown 语法排版”的提示文字在垂直中心线上产生偏移，未严格基准线对齐；
3. **光标与 Placeholder 不对齐**：聚焦多行文本输入框时，光标的垂直起始高度与 placeholder 占位文字的首行文字基线不一致，出现上浮或错位现象；
4. **光标颜色过深突兀**：光标颜色默认继承原生黑色（`#000000`），在与浅灰色的 placeholder 文字（`#999999`）共存时对比度过于强烈突兀。

---

## 2. 改动详情

### 2.1 提取平台级组件 `VolurMarkdownEditor`

在 `src/volur-lite-view/src/public/volurEx/components/` 下新建 [`VolurMarkdownEditor.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurMarkdownEditor.vue)：
- **命名规范**：遵循 `volurEx` 组件库规范，命名为 `VolurMarkdownEditor`；
- **接口封装**：
  - 双向绑定：同时支持 `v-model:value`、`v-model:modelValue` 与 `@change`；
  - 属性配置：支持 `placeholder`、`rows`（默认 8）、`minHeight`（默认 200px）、`maxHeight`（默认 480px）、`disabled`、`showTip`、`tipText`、`emptyPreviewText`；
  - 国际化支持：原生内置 `vue-i18n` 兜底文本（`common.markdown_edit`、`common.markdown_preview`、`common.markdown_tip` 等）；
  - 渲染安全：基于统一的 `@/utils/markdown` 模块渲染 HTML，容器使用 `.volur-markdown-body` 保证排版隔离。
- **全局导出**：
  - 在 `src/public/volurEx/components/index.ts` 中导出 `VolurMarkdownEditor`；
  - 在 `src/public/volurEx/index.ts` 根入口集中向外导出。
- **历史兼容与收敛**：
  - 将 `src/components/common/MarkdownEditor.vue` 改造为引用 `VolurMarkdownEditor` 的薄代理，杜绝破坏历史引用；
  - 将 `BasicInfoTab.vue` 与 `AnnounceTab.vue` 统一重构为从 `@/public/volurEx` 引入 `<VolurMarkdownEditor ... />`。

---

### 2.2 工具栏左侧按钮与右侧提示对齐修复

**原因定位**：
- 工具栏容器仅依赖了简单的弹性布局，但由于左侧分段 Tab（`n-tabs`）带有自身预设的外边距与行高基准，而右侧文字仅为裸 `span`，两者在默认继承行高（`line-height: normal` 或 `1.5`）下，文本字形基线产生 2px~4px 的垂直偏差。

**解决方案**：
1. 工具栏容器 `.editor-toolbar` 设定显式高度与中心轴约束：
   ```scss
   min-height: 44px;
   padding: 6px 14px;
   display: flex;
   justify-content: space-between;
   align-items: center;
   box-sizing: border-box;
   ```
2. 左侧 Tab 内部 `.tab-label` 与右侧 `.toolbar-tip` 均设置：
   ```scss
   display: inline-flex;
   align-items: center;
   line-height: 1;
   ```
3. 右侧提示文字颜色严格遵循平台视觉铁律：`color: var(--v-text-desc, #444444);`。使得左右两端的水平中轴严格对齐在同一水平线。

---

### 2.3 输入框 Placeholder 与光标不对齐的根因与修复

#### 根因分析
1. **多行文本框内边距（Padding）不一致**：
   - 旧实现给 `.n-input__textarea-el` 声明了 `padding: 12px`；
   - 但 Naive UI 内部绝对定位的占位层 `.n-input__placeholder` 并未同步重写，仍继承了组件库默认的 `padding: 8px 12px`；
   - 导致 placeholder 的顶边绝对距离为 8px，而真实光标所在 textarea 的顶边为 12px，产生 4px 的物理错位。
2. **全局样式行高污染**：
   - 全局样式 `src/styles/index.scss` 中原先存在如下单行输入框垂直居中规则：
     ```scss
     .n-input {
       .n-input__placeholder > span {
         line-height: 1 !important;
       }
     }
     ```
   - 该规则对所有 `n-input` 暴力生效，导致多行文本框（textarea）下的 placeholder 行高被直接腰斩为 `1`（13px），而文本框输入元素 `textarea-el` 实际行高为 `1.6`（20.8px），造成 placeholder 文字基线严重向上紧缩。
3. **字体族（font-family）与首字符宽度微差异**：
   - textarea 输入框应用了等宽字体 `Fira Code, monospace`，而占位符继承了默认非等宽系统字体，使得字符宽度与字高垂直盒模型产生细微偏移。

#### 修复措施
1. **全局样式作用域精准收窄**：
   - 将 `src/styles/index.scss` 中的全局修饰限制为非多行文本框：
     ```scss
     .n-input:not(.n-input--textarea) {
       .n-input__placeholder > span {
         line-height: 1 !important;
       }
     }
     ```
2. **精准同步尺寸盒模型与字体**：
   - 在 `VolurMarkdownEditor.vue` 中对 `.n-input__textarea-el` 与 `.n-input__placeholder` 实行 100% 像素级对齐：
     ```scss
     :deep(.n-input__textarea-el) {
       font-family: var(--v-font-mono, 'Fira Code', monospace), -apple-system, sans-serif;
       font-size: 13px;
       line-height: 1.6;
       padding: 12px 14px;
       color: var(--v-text-primary, #111111);
       box-sizing: border-box;
     }

     :deep(.n-input__placeholder) {
       padding: 12px 14px !important;
       font-family: var(--v-font-mono, 'Fira Code', monospace), -apple-system, sans-serif !important;
       font-size: 13px !important;
       line-height: 1.6 !important;
       box-sizing: border-box;

       > span {
         display: inline-block;
         font-family: inherit !important;
         font-size: inherit !important;
         line-height: 1.6 !important;
       }
     }
     ```

---

### 2.4 光标颜色深浅度微调

**设计考虑**：
- 默认情况下，浏览器的 `caret-color` 为 `auto`，直接渲染为深黑 `#000000`，在浅色未输入状态下与 `#999999` 的占位符文字对比度过于突兀；
- 按照要求，将光标颜色调整为比 placeholder 略深一点点、温和但可清晰辨识的层级：
  - **日间模式**：
    - Placeholder：`#999999`
    - Caret（光标）：`caret-color: #666666;`
  - **夜间模式**：
    - Placeholder：`#64748b`
    - Caret（光标）：`caret-color: #94a3b8;`

---

## 3. 影响范围与验证

- **涉及文件**：
  - 新建：`src/volur-lite-view/src/public/volurEx/components/VolurMarkdownEditor.vue`
  - 导出：`src/volur-lite-view/src/public/volurEx/components/index.ts`、`src/volur-lite-view/src/public/volurEx/index.ts`
  - 适配：`src/volur-lite-view/src/views/admin/competition/tabs/BasicInfoTab.vue`、`src/volur-lite-view/src/views/admin/competition/tabs/AnnounceTab.vue`、`src/volur-lite-view/src/components/common/MarkdownEditor.vue`
  - 全局样式修复：`src/volur-lite-view/src/styles/index.scss`
- **构建测试**：
  - 执行 `npm run build`，编译打包耗时 5.47s，代码 0 退出，无任何 TypeScript 编译错误与样式警告。
