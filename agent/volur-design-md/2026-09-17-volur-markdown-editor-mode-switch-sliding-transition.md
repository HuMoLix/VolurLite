# VolurMarkdownEditor 模式切换平滑滑动卡片过渡动画实现

## 1. 变更背景

在将 Markdown 编辑器的“编辑 / 预览”切换重构为自研分段切换器（`.editor-mode-switch`）后，为了进一步提升组件的原生交互质感与操作流畅度，增加点击切换时的背景卡片“滑动”（Sliding Pill）过渡动画。

---

## 2. 实现方案

### 2.1 结构设计
在 `.editor-mode-switch` 中内置绝对定位的卡片滑块 `.mode-slider`，与两个相对定位的文字/图标按钮形成层叠结构：
```html
<div
  class="editor-mode-switch"
  :class="{ 'is-preview': activeMode === 'preview' }"
>
  <!-- 滑动卡片背景指示器 -->
  <div class="mode-slider" />

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

### 2.2 纯 CSS 高性能硬件加速滑动
1. **滑块尺寸与位移**：
   - 容器固定总宽 156px，两端内边距 2px；
   - `.mode-slider` 宽度设定为 `width: calc(50% - 2px)`，高度贴合按钮（`top: 2px; bottom: 2px;`）；
   - 在“编辑”模式下默认位于 `translateX(0)`；
   - 在“预览”模式（`.is-preview`）下触发 `transform: translateX(100%)`，通过 GPU 硬件加速进行水平位移，无重排重绘。
2. **缓动曲线与时序**：
   - 采用原生流畅的缓动曲线：`transition: transform 0.22s cubic-bezier(0.4, 0, 0.2, 1);`；
   - 按钮文字与图标色彩在滑块移过时伴随平滑淡入（`transition: color 0.2s ease`）。
3. **主题分层适配**：
   - **亮色模式**：滑块采用纯白背景 `#ffffff`，搭配双层细致弥散阴影 `box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04)`；
   - **暗色模式**：滑块采用深曜炭黑 `#232733`，搭配低对比度深色微阴影 `box-shadow: 0 1px 3px rgba(0, 0, 0, 0.35)`。

---

## 3. 构建与验证

- 执行 `npm run build`，5.50s 编译打包通过（Exit Code 0），无任何报错与警告。
