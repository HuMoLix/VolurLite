# 认证布局容器背景色 Token 映射修复记录

## 变更背景
在认证模块布局 `AuthLayout.vue` 中，PC 端（`min-width: 840px`）通过 `var(--sys-color-surface-container, #f0f4f9)` 设置背景底色。该色值在 Google Material 3 规范中应为 Surface Container 浅蓝灰底色（`#f0f4f9`，深色模式 `#1e1f20`），以与纯白卡片容器形成清晰的层级反差。
在先前引入基于 `light-dark()` 的全局 CSS Token 规范 `vcss.scss` 时，`--sys-color-surface-container` 被错误地代理至 `--color-v3-surface-container`：
```scss
--sys-color-surface-container: var(--color-v3-surface-container, light-dark(#f0f4f9, #1e1f20));
```
由于 `--color-v3-surface-container` 在亮色模式下已被定义为纯白 `#fff`，导致 `var()` 的 Fallback 机制失效，`--sys-color-surface-container` 被计算为 `#fff`，使得认证页面整体背景变为纯白，与卡片融为一体，破坏了 Google Material 3 的层级视觉体系。

## 改动点
1. **修正 `src/styles/vcss.scss` 中 `--sys-color-surface-container` 的独立定义**：
   - 彻底解除其对 `--color-v3-surface-container` 的代理引用；
   - 规范化采用 `light-dark()` 原生映射：`--sys-color-surface-container: light-dark(#f0f4f9, #1e1f20);`；
   - 严格遵循代码文件零注释铁律，保持 SCSS Token 定义纯净紧凑。

## 影响范围
- `src/styles/vcss.scss`
- `src/components/layouts/auth/AuthLayout.vue` 以及所有依赖 `--sys-color-surface-container` 的 Surface Container 容器与 Tailwind 工具类。
