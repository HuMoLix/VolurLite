# VButton 焦点环统一收敛与水波纹冗余清理技术方案

## 变更背景
在对平台前端自研基础组件库 Volur UI 的 `VButton`（[VButton.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/volur-ui/components/button/src/VButton.vue)）进行架构审查与样式精简时，发现如下问题：
1. **焦点环色彩碎片化且缺乏受控开关**：
   - 之前在 `volur-btn--primary`、`volur-btn--normal`、`volur-btn--tonal`、`volur-btn--text`、`volur-btn--danger` 以及 `volur-btn--warning` 中均各自声明了局部 CSS 变量 `--volur-btn-focus-ring`。
   - 其中 `danger` 使用红色焦点环，`warning` 使用黄色焦点环，违背了 Google Workspace 全平台统一蓝色外聚焦轮廓线的无障碍视觉规范；
   - 组件未对外提供控制是否开启键盘无障碍焦点环的 Prop 属性，缺少灵活性。
2. **水波纹扩散变量冗余与无效定义**：
   - 在 `volur-btn--text` 中声明了 `--volur-btn-ripple`，但在底层状态机 `playWave()` 中明确针对 `props.variant === 'text'` 进行了早退拦截，导致文本变体中的水波纹变量完全属于无用的死代码；
   - 在 `volur-btn--warning` 与 `volur-btn--danger` 中，动画帧 `@keyframes volur-wave-spread` 原生支持向 `currentColor` 降级，无需在各变体重复声明硬编码色值。

---

## --volur-btn-ripple 作用机制深度解构剖析

`--volur-btn-ripple` 是 Volur UI 自研水波纹光晕扩散动画系统（Ripple Wave System）的核心动态色彩令牌。

### 1. 触发与状态机流转
- 当用户使用鼠标点击按钮，或通过键盘敲击 `Space` / `Enter` 触发按钮时，会触发 `playWave()` 方法；
- `playWave()` 内部进行快速判定：若 `props.ripple` 为 `false`、变体为 `text`、处于 `disabled` 或 `loading` 状态，则直接终止；
- 状态机利用 Vue `nextTick()` 结合 `offsetWidth` 读取强制触发浏览器 DOM 重绘（Reflow），随后将 `isWaveActive` 设为 `true`，激活内部承载光晕的 DOM 节点：
  ```html
  <span ref="waveRef" aria-hidden="true" class="volur-btn__wave" :class="{ 'is-active': isWaveActive }" />
  ```

### 2. 动效渲染机理
- 当 `.volur-btn__wave` 挂载 `is-active` 类名时，将并行运行两个关键帧动画：
  - `volur-wave-spread 0.6s cubic-bezier(0, 0, 0.2, 1) forwards`：负责将 `box-shadow` 从 `0 0 0.5px 0` 扩散到 `0 0 0.5px 5.5px`；
  - `volur-wave-opacity 0.6s cubic-bezier(0, 0, 0.2, 1) forwards`：负责将外扩光晕的不透明度从 `0.65` 平滑衰减至 `0`。
- 在 `volur-wave-spread` 中：
  ```scss
  @keyframes volur-wave-spread {
    from {
      box-shadow: 0 0 0.5px 0 var(--volur-btn-ripple, currentColor);
    }
    to {
      box-shadow: 0 0 0.5px 5.5px var(--volur-btn-ripple, currentColor);
    }
  }
  ```
- `--volur-btn-ripple` 变量即用于注入扩散光晕的特定色彩。
- 若变体未显式声明 `--volur-btn-ripple`，动画将自然无缝降级至 `currentColor`，自动吸附当前按钮的文字与边框主色调。

---

## 核心改动点与实施细节

### 1. 声明与接入 `isFocusRing` 属性
- 在 [`src/volur-ui/components/button/src/types.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/volur-ui/components/button/src/types.ts) 的 `VButtonProps` 接口中新增：
  ```ts
  isFocusRing?: boolean
  ```
- 在 [`src/volur-ui/components/button/src/VButton.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/volur-ui/components/button/src/VButton.vue) 中通过 `withDefaults` 赋予默认值 `true`；
- 在模板根元素 `<button>` 的 `:class` 对象中动态绑定 `'is-focus-ring': isFocusRing`。

### 2. 焦点环样式收敛与颜色全平台统一
- 移除顶层无条件的 `&:focus-visible` 选择器；
- 新增 `&.is-focus-ring` 修饰类规则，仅在受控开启 `isFocusRing` 时在 `:focus-visible` 状态下呈现聚焦环；
- 将焦点环色彩统一收敛为 Google Workspace 蓝：`light-dark(#1a73e8, #a8c7fa)`，支持暗黑模式自适应高对比呈现；
- 同时在 `box-shadow` 的降级色中也对齐为 `light-dark(#1a73e8, #a8c7fa)`：
  ```scss
  &.is-focus-ring {
    --volur-btn-focus-ring: light-dark(#1a73e8, #a8c7fa);

    &:focus-visible {
      box-shadow: 0 0 0 2px var(--v-bg-base, #ffffff), 0 0 0 4px var(--volur-btn-focus-ring, light-dark(#1a73e8, #a8c7fa));
    }
  }
  ```

### 3. 水波纹开关重构为 `isPressWave` 并默认关闭
- 将历史 `ripple?: boolean` 属性全链路彻底重构为 `isPressWave?: boolean`，杜绝保留遗留别名；
- 在 `VButton.vue` 中将默认值调整为 `isPressWave: false`（默认不开启点击水波纹光晕扩散动画，按需受控开启）；
- 在 `playWave()` 状态机执行入口更新拦截条件：`if (!props.isPressWave || props.variant === 'text' || props.disabled || props.loading) return`。

### 4. 清理各变体中的 `--volur-btn-focus-ring` 与 `--volur-btn-ripple`
- 从 `.volur-btn--primary`、`.volur-btn--normal`、`.volur-btn--tonal`、`.volur-btn--danger`、`.volur-btn--warning` 与 `.volur-btn--text` 中全量删除重复的 `--volur-btn-focus-ring` 声明；
- 从 `.volur-btn--text`、`.volur-btn--warning` 与 `.volur-btn--danger` 中彻底删除 `--volur-btn-ripple`：
  - `text` 变体消除无效死代码；
  - `warning` 与 `danger` 变体自动降级至 `currentColor`，水波纹光晕自然匹配语义警告红/黄色系。

---

## 验证与影响范围

1. **工程构建与类型验证**：
   - 运行 `vue-tsc --noEmit && vite build`，31705 个模块全量编译打包通过，0 错误，0 警告阻断。
2. **影响范围**：
   - [`src/volur-ui/components/button/src/types.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/volur-ui/components/button/src/types.ts)：扩展按钮 Props 规范；
   - [`src/volur-ui/components/button/src/VButton.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/volur-ui/components/button/src/VButton.vue)：精简变体样式，统一 Google 焦点环体系并支持受控开关。
