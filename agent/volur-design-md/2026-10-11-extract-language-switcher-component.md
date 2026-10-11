# LanguageSwitcher 组件独立提取与加载卡片背景重构审计记录

## 1. 变更背景与架构动机
在 `volur-lite-view-NEXT` 前端工程中，全局多语言切换的加载遮罩（Mask）、居中加载卡片（Loading Card）及其淡入淡出动画此前内联在根组件 `src/App.vue` 模板与样式中。
为了遵循组件化职责分离、降低 `App.vue` 复杂度，并提升全局状态视觉呈现的纯粹度：
1. 将语言切换的 DOM 结构、状态监听和过渡动画从 `src/App.vue` 中解耦并独立提取至 `src/components/common/LanguageSwitcher.vue`；
2. 对加载卡片的视觉细节进行优化：彻底删除加载卡片（`.lang-loading-card`）的实体背景颜色（`background-color: transparent` 与 `box-shadow: none`），仅保留居中的 `n-spin` 旋转指示器与文本提示在毛玻璃遮罩层中柔和呈现，视觉更具沉浸感与现代感；
3. 在 `src/App.vue` 中直接导入并引用 `<LanguageSwitcher />`，精简根组件代码。

---

## 2. 核心改动点

### 2.1 新建独立组件 `LanguageSwitcher.vue`
- 文件路径：`src/components/common/LanguageSwitcher.vue`；
- 结构与逻辑：
  - 接入 `useAppStore`，响应式计算 `isLangSwitching` 与 `langSwitchingText`；
  - 包含 `<transition name="lang-mask-fade">` 动画外层与全屏遮罩容器 `.global-lang-loading-mask`；
  - 包含居中内容卡片 `.lang-loading-card`，内嵌 Naive UI 的 `<n-spin size="large" />` 与文本提示；
  - 通过 `defineExpose` 导出 `isLangSwitching` 与 `langSwitchingText`；
- 样式优化：
  - 彻底移除 `.lang-loading-card` 的背景颜色与阴影（设置 `background-color: transparent; box-shadow: none;`，暗黑模式同步对齐）；
  - 保留 `.global-lang-loading-mask` 的毛玻璃模糊（`backdrop-filter: blur(4px)`）与半透明磨砂底色；
  - 保留 `.lang-mask-fade` 的 0.25s 贝塞尔曲线淡入淡出过渡动画。

### 2.2 重构根组件 `src/App.vue`
- 模板改动：将原本内联的 `<transition name="lang-mask-fade">` 块替换为语义化的 `<LanguageSwitcher />`；
- 脚本改动：移除 `naive-ui` 中多余的 `NSpin` 导入，显式导入 `LanguageSwitcher from '@/components/common/LanguageSwitcher.vue'`；
- 样式精简：从 `App.vue` 中彻底移除 `.global-lang-loading-mask`、`.lang-loading-card`、`.lang-loading-text` 以及 `.lang-mask-fade-*` 样式规则，保持根组件的极简。

---

## 3. 规范检查与纯粹代码铁律 (Zero Comments Rule)
- `src/components/common/LanguageSwitcher.vue` 与 `src/App.vue` 严格遵守零注释铁律，代码内无任何冗余注释；
- 组件遵循 Vue 3 Composition API `<script setup lang="ts">` 规范。

---

## 4. 验证结果
- **类型检查**：执行 `npx vue-tsc --noEmit`，结果代码 0，无任何类型缺失或属性未对齐问题；
- **全量构建打包**：执行 `npm run build`，Vite 编译打包成功输出。
