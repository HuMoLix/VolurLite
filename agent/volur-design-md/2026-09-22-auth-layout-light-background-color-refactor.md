# AuthLayout 日间模式背景颜色调整与主题兼容记录

## 1. 变更背景与需求
根据 UI 视觉规范调整需求，要求将鉴权与登录页布局组件 [`AuthLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AuthLayout.vue) 在日间（Light）模式下的背景颜色显式调整为 `#fff`（`#ffffff`），替换原先全局统一继承的 `--v-bg-base`（`#f8f9fa`），以提供更加纯粹沉浸的日间登录与注册视觉体验。同时必须确保黑夜模式（Dark Mode）下的深曜炭黑高奢基底保持不变。

---

## 2. 变更改动点

### 2.1 [`src/volur-lite-view/src/public/volurEx/layouts/AuthLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AuthLayout.vue)
- 将根容器 `.auth-page-layout` 的默认背景颜色修改为 `#ffffff`。
- 引入平台自研组件库 SCSS 主题 Mixin `@include theme-dark`，在黑夜模式（`html.dark` 与 `[data-theme='dark']`）下无缝回退至深曜炭黑基底 `var(--v-bg-base)`。

```scss
// 修改前：
.auth-page-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  background-color: var(--v-bg-base);
  color: var(--v-text-primary);
  transition: background-color 0.25s ease, color 0.25s ease;
}

// 修改后：
.auth-page-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  background-color: #ffffff;
  color: var(--v-text-primary);
  transition: background-color 0.25s ease, color 0.25s ease;

  @include theme-dark {
    background-color: var(--v-bg-base);
  }
}
```

---

## 3. 安全隐患修复与兼容性说明
- **主题切换平滑过渡**：保留了 `transition: background-color 0.25s ease, color 0.25s ease;` 动画过渡，在用户点击左下角日夜间主题切换按钮时平滑无突兀闪烁。
- **暗色模式保全**：通过精确的 `@include theme-dark` 规则，确保暗色模式下不会因日间模式修改而泛白，保持平台暗黑美学规范一致性。
- **构建校验**：通过 `vue-tsc --noEmit && vite build` 完整生产级编译校验，无任何 SCSS 解析与编译错误。

---

## 4. 影响范围
- 受影响路由：所有挂载在 `AuthLayout` 下的页面（`/auth/login`、`/auth/register`、`/auth/forgot`、`/auth/reset-password`）。
- 其它系统布局（`AppLayout`、`AdminLayout`、`CompetitionLayout`）不受影响，继续维持全局设计令牌。
