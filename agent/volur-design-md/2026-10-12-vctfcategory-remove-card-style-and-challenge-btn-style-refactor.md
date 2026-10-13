# VCtfCategory 去除卡片包装并适配 challenge-btn 侧边栏按钮样式方案

## 1. 变更背景与目标

根据用户交互与 UI 层次优化需求，原 `VCtfCategory.vue` 组件带有外层独立卡片白底、边框及内边距（Padding），在 CTF 赛题列表页与外部布局存在多重卡片嵌套感。
本次优化要求：
1. 移除 `VCtfCategory.vue` 组件的卡片背景色（Background）、边框（Border）与内边距（Padding），直接将分类列表项紧凑平铺展示；
2. 将分类项按钮样式全面对齐 `VSideBar.vue` 中经典高频使用的 `n-menu-item` 现代胶囊按钮规范，命名为 `.challenge-btn`；
3. 支持浅色/深色主题下的 Hover 与 Active/Selected 状态响应，并确保全端零注释与编译无报错。

---

## 2. 核心改动点

### 2.1 移除卡片容器背景与 Padding
- 路径：`src/components/competition/ctf/VCtfCategory.vue`
- 原顶层容器带有 `rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark p-3 sm:p-4 shadow-sm`；
- 修改为轻量级容器 `<div class="v-ctf-category w-full">`，移除所有背景、外框与 Padding，使分类列表自然融入侧边区域。

### 2.2 重构列表项为 `.challenge-btn`
- 结构与类名：
  - 按钮容器采用 `.challenge-btn`，高度 40px，圆角 16px，支持 `is-selected` 选中态；
  - 内部内容包装为 `.challenge-btn-content`（`height: 40px; padding: 0 12px;`）；
  - 图标采用 `.challenge-btn-icon`（18px，`margin-right: 12px`）；
  - 标题采用 `.challenge-btn-label`（14px，`font-weight: 500`，支持单行溢出省略）；
  - 徽章采用 `.challenge-btn-badge`（等宽字体数字徽章，居右展示题量）。
- 视觉阶梯样式（1:1 对齐 `VSideBar.vue` 的 `n-menu-item`）：
  - **亮色模式**：
    - 默认背景透明，文字与图标颜色为 `#565b61`；
    - Hover 态：背景 `#efefef`，文字与图标变为 `#1b1b1e`；
    - Selected 态：背景 `#ececef`，文字与图标变为 `#1b1b1e`，字重加粗；
  - **暗色模式 (`:global(.dark)`)**：
    - 默认背景透明，文字与图标颜色为 `#8c8c8c`；
    - Hover 态：背景 `#2a2a2a`，文字与图标变为 `#d4d4d4`；
    - Selected 态：背景 `#2a2a2a`，文字与图标变为 `#d4d4d4`，字重加粗。

---

## 3. 规范执行与验证

1. **纯粹代码与零注释铁律**：经 `Select-String` 模式扫描，`VCtfCategory.vue` 没有任何行内或模板注释；
2. **构建验证**：执行 `npm run build` (`vue-tsc --noEmit && vite build`)，打包与类型检查 0 错误通过。
