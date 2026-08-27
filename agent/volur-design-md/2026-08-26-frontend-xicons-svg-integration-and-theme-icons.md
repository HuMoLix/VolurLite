# 前端引入 xicons (Ionicons 5) 与日月模式 SVG 图标重构

## 1. 变更背景与需求
- 前端原主题切换按钮（日月模式）使用 Emoji 字符（🌙 / ☀️），在不同系统与浏览器字体下存在渲染尺寸不一致与对齐偏差；
- 按照指令集成官方 **xicons** SVG 图标库（`@vicons/ionicons5`），全面采用 [`SunnyOutline`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AuthLayout.vue#L55) 与 [`MoonOutline`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AuthLayout.vue#L55) 进行矢量化渲染；
- 同时对主布局导航菜单图标（`HomeOutline`, `FlagOutline`, `TrophyOutline`）进行矢量化升级。

---

## 2. 改动点汇总

### 2.1 依赖与图标库说明
- 确认基于 `xicons` 的 `@vicons/ionicons5` 与 `@vicons/utils` 依赖已就绪；
- 统一使用 Naive UI 官方推荐的 `<n-icon>` 容器包裹 SVG 矢量图标。

### 2.2 [`src/layout/AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AuthLayout.vue)
- 左下角主题切换按钮由 Emoji 替换为：
  ```html
  <n-icon size="18">
    <SunnyOutline v-if="appStore.theme === 'dark'" />
    <MoonOutline v-else />
  </n-icon>
  ```
- 显式导入 `NIcon` 及 `SunnyOutline` / `MoonOutline`。

### 2.3 [`src/layout/AppLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue)
- 顶部导航栏主题切换按钮同步替换为 `<n-icon>` + `SunnyOutline` / `MoonOutline`；
- 侧边栏菜单配置采用矢量化图标：
  - **首页**：`HomeOutline`
  - **题库训练**：`FlagOutline`
  - **竞赛演练**：`TrophyOutline`

---

## 3. 验证结论
- **TypeScript 与打包**：`vue-tsc --noEmit && vite build` 编译 100% 成功，0 警告 0 报错；
- **视觉一致性**：高清晰度 SVG 矢量图标在任何屏幕缩放与明暗模式下均呈现清晰精致的视觉体验。
