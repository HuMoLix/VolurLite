# CTF 分类管理弹窗输入框与按钮样式对齐施工文档

## 1. 变更背景与问题溯源

### 1.1 问题现象
开发者反馈在 [`src/volur-lite-view/src/views/admin/challenge/components/CtfCategoryModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/components/CtfCategoryModal.vue)（CTF 赛题方向分类管理弹窗）中，顶部新增分类工具栏的输入框和按钮看起来明显偏“扁”，与平台标准页面（如 [`src/volur-lite-view/src/views/admin/challenge/AdminCtfChallengeView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/AdminCtfChallengeView.vue)）中的按钮与输入框视觉不协调。

### 1.2 根因深度对比分析
对比 [`AdminCtfChallengeView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/AdminCtfChallengeView.vue) 与 [`CtfCategoryModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/components/CtfCategoryModal.vue) 的实现：

1. **尺寸规格冲突 (`size="small"` vs 默认 `medium`)**：
   - **`AdminCtfChallengeView.vue`**：筛选栏中的 `<n-input>`、`<n-select>` 与 `<n-button>` 均未显式传递 `size="small"`，继承 Naive UI 平台默认的标准 `medium` 规格：
     - **标准高度**：`34px`
     - **标准字体**：`14px`
     - **内边距与排版**：纵向呼吸感强，图标与文字垂直居中居中均匀。
   - **`CtfCategoryModal.vue`（原代码）**：
     - 原工具栏所有控件 `<n-input>`、`<VolurSvgPicker>`、`<n-button>` 均显式配置了 `size="small"`；
     - Naive UI 的 `small` 规格高度仅为 **`28px`**（且文字仅为 `12px`），导致控件整体被垂直压缩了 6px（缩水约 18%）；
     - `VolurSvgPicker`（基于 `VolurIconPicker.vue`）在接收 `size="small"` 时同样被强制约束为 `height: 28px; font-size: 12px`；
     - 极低的高度结合较小的宽度固定值（140px / 160px / 180px），产生了非常局促、干瘪的“扁平化”失真视觉。
2. **编辑弹窗内部级联缩小**：
   - 原代码中，编辑分类子弹窗 `<n-form>` 同样被标记了 `size="small"`，导致其下属的表单输入项与弹窗底部的“取消”/“保存”按钮也全部退化为 28px 高度，与系统其他模态框标准（34px / 36px）脱节。
3. **主按钮黑夜模式硬编码**：
   - 原组件中的 `.theme-primary-btn` 样式硬编码了纯黑背景 `#000000` 与纯白文字 `#ffffff`，未采用平台主题 CSS 变量（`var(--v-btn-primary-bg, #000000)` 及 `var(--v-btn-primary-text, #ffffff)`），在黑夜模式下缺乏反色倒置响应。

---

## 2. 改造点与对齐方案

1. **移除强制 `size="small"`，恢复平台标准 `medium` 尺寸**：
   - 移除工具栏 `<n-input>`、`<VolurSvgPicker>`、`<n-button>` 上的 `size="small"` 属性；
   - 移除编辑分类子弹窗中 `<n-form>` 与底部 `<n-button>` 上的 `size="small"` 属性；
   - 统一对齐到标准 `34px` 组件高度与 `14px` 正文字号。
2. **优化宽度与交互体验**：
   - 分类名称输入框宽度由 `140px` 增至 `150px`，增加 `clearable` 属性并支持 `@keyup.enter="handleCreateCategory"` 回车提交；
   - 图标选择器宽度由 `160px` 微调至 `170px`，提供充足的图标名与下箭头显示空间；
   - 说明输入框宽度由 `180px` 适度增至 `220px`，支持一键清空与回车提交；
   - 工具栏间距由 `gap: 8px` 调整为 `gap: 10px`，提升横向呼吸感；
   - 子弹窗编辑表单标签宽度调整为 `label-width="80"`，名称字段增加 `clearable` 与回车保存支持。
3. **规范主要按钮设计令牌与主题响应**：
   - 重构 `.theme-primary-btn` SCSS 样式，对接全局 CSS 变量：
     - 背景色：`var(--v-btn-primary-bg, #000000)`
     - 前景色：`var(--v-btn-primary-text, #ffffff)`
     - 悬浮色：`var(--v-btn-primary-hover, #222222)`
     - 激活态：`var(--v-btn-primary-active, #333333)`
   - 严格遵循 `view-Rules.md` 规范，在日间保持纯黑主色，黑夜模式自动反色倒置。

---

## 3. 影响范围与验证

- **受影响文件**：
  - [`src/volur-lite-view/src/views/admin/challenge/components/CtfCategoryModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/components/CtfCategoryModal.vue)
- **编译与类型校验验证**：
  - 运行 `npx vue-tsc --noEmit`：0 错误，类型系统完全通过。
  - 运行 `npm run build`：Vite 完整构建成功，各分包均顺利产出。
