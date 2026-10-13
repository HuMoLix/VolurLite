# VChallengeSearch 赛题搜索框组件对齐 VSearch 视觉样式重构方案

## 1. 变更背景与目标

为保持系统内部搜索输入组件视觉语言的高度一致性，用户要求将 `VChallengeSearch.vue` 的搜索框外观全面对齐全局侧边栏弹窗搜索组件 `src/components/sidebar/VSearch.vue` 的 `search-header` 样式规范。

---

## 2. 核心改动点

### 2.1 样式与结构全面对齐 `VSearch.vue`
- 路径：`src/components/competition/ctf/VChallengeSearch.vue`
- 结构设计：
  - 外层外壳采用 `.search-input-shell`，高度 40px，圆角 20px，细边框（亮色 `#dadce0` / 暗色 `#3c4043`），支持聚焦时 Google Blue 边框与微光投影（`:focus-within`）；
  - 左侧搜索按钮 `.search-action-btn.search-trigger-icon`，采用圆形交互（尺寸 28px，图标 18px），亮色 Google Blue `#1a73e8`，Hover 带浅蓝背景波纹；暗色采用 Google Blue Dark `#a8c7fa`，Hover 带波纹；
  - 中间原生输入框 `.search-native-input`，去除默认边框与 outline，字号 13px，亮色正文 `#202124` / 暗色 `#e3e3e3`，占位符严格映射 Google Gray；
  - 右侧清空按钮 `.search-action-btn.clear-btn`，输入框有内容时展示，带悬停微灰色块提示，点击一键清空并自动恢复焦点。
- 主题适配：
  - 通过 `:global(.dark)` 支持全暗色模式阶梯适配，深曜炭黑背景 `#202124`，细微边框 `#3c4043`。

---

## 3. 规范执行与验证

1. **纯粹代码与零注释铁律**：经 `Select-String` 扫描，`VChallengeSearch.vue` 中无任何行内或模板注释；
2. **构建验证**：执行 `npm run build` (`vue-tsc --noEmit && vite build`)，打包与类型检查 0 错误通过。
