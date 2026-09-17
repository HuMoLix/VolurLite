# VolurMarkdownEditor 模式切换组件自研重构与工具栏对齐

## 1. 变更背景

在先前的实现中，Markdown 编辑器的工具栏左侧“编辑 / 预览”切换采用了 Naive UI 的页面级组件 `NTabs (type="segment")`。
由于 Naive UI 的分段 Tab 组件内部存在深达 7 层的嵌套结构（包含 JS 动态监听计算绝对定位的 `.n-tabs-capsule`、`line-height: 1.5`、行内基线对齐机制等），在尝试通过 CSS 强行覆盖其高度和内边距时，导致：
1. **按钮外观畸变（“看着很奇怪”）**：强行设置 `padding: 0` 使内部滑块胶囊尺寸错乱、文字紧贴边框、胶囊阴影剪裁异常；
2. **垂直对齐仍有偏差**：多层容器的基线以及 `width: 155px` 造成的水平挤压无法与工具栏整体建立纯粹的居中几何对齐。

---

## 2. 重构与修复方案

### 2.1 剔除 `NTabs`，采用平台自研分段切换器 `.editor-mode-switch`
将 `n-tabs` 彻底替换为语义化的轻量分段切换器：
```html
<div class="editor-mode-switch">
  <button
    type="button"
    class="mode-btn"
    :class="{ 'is-active': activeMode === 'edit' }"
    :disabled="disabled"
    @click="activeMode = 'edit'"
  >
    <n-icon :component="CreateOutline" />
    <span>{{ t('common.markdown_edit') }}</span>
  </button>
  <button
    type="button"
    class="mode-btn"
    :class="{ 'is-active': activeMode === 'preview' }"
    :disabled="disabled"
    @click="activeMode = 'preview'"
  >
    <n-icon :component="EyeOutline" />
    <span>{{ t('common.markdown_preview') }}</span>
  </button>
</div>
```

### 2.2 精准盒模型与深浅色主题适配
1. **外层胶囊轨道（Track）**：
   - 采用 `display: inline-flex; align-items: center; padding: 2px; border-radius: 6px;`；
   - 亮色底色：`rgba(0, 0, 0, 0.05)`；暗色底色：`rgba(255, 255, 255, 0.07)`。
2. **内层模式按钮（Buttons）**：
   - 高度统一固定为 `26px`，横向内边距 `11px`，两按钮间自然排列；
   - 文字与图标通过 `display: inline-flex; align-items: center; justify-content: center; gap: 6px;` 实现垂直居中；
   - **激活态（Active）**：
     - 亮色：纯白 `#ffffff` 滑块卡片，搭配柔和微投影 `box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08)`，字体加粗，主色黑 `#111111`；
     - 暗色：深曜炭黑 `#232733` 滑块卡片，`#ffffff` 文字；
   - **未激活态（Inactive）**：背景透明，文字颜色遵循规范 `#444444`（暗色 `#94a3b8`），悬浮高亮反馈平滑自然。
3. **工具栏全居中**：
   - 外层轨道总高 30px（26px 按钮 + 2px*2 轨道内边距）；
   - 在 `.editor-toolbar`（`height: 40px; padding: 0 14px;`）中，Flex `align-items: center` 实现绝对数学居中，与右侧的快捷格式化按钮及“支持 Markdown 语法排版”在同一条水平中线上严格居中对齐。

---

## 3. 验证

- 执行 `npm run build`，5.49s 打包成功（Exit Code 0），无任何报错与警告。
