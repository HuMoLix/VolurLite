# Vue SFC Scoped CSS 编译陷阱排查与暗色模式选择器全面修复记录文档

本文档详细记录针对前端“暗色模式修改后页面未更新”现象的底层根因排查过程、Vue 3 SFC 编译机制分析，以及全代码库的系统化修复方案。

---

## 1. 故障现象与排查背景

在完成暗色模式颜色调整（背景 `#1e1f20`、卡片 `#0e0e0e`、文字 `#e3e3e3`、输入框光标 `#fff` 与边框 `#1e1f20`、主次按钮高对比度配色等）后，开发环境页面刷新后未见样式更新。

经过对 Vite 编译产物（`dist/assets/*.css`）及本地服务环境的深度逆向审查，定位到了两个维度的根本原因：

---

## 2. 根本原因剖析

### 2.1 核心根因：Vue SFC `<style scoped>` 编译机制陷阱 (Compiler Bug)
在 Vue 3 单文件组件（SFC）的 `<style scoped>` 块中，此前采用了如下 SCSS 嵌套语法：
```scss
:global(html.dark) {
  .auth-layout { background-color: #1e1f20; }
  .auth-card { background-color: #0e0e0e !important; }
}
```
或：
```scss
:global(html.dark) .auth-scrim { ... }
:global(.dark) .rc-modal-card { ... }
```

**编译结果逆向分析**：
Vue 的 `@vue/compiler-sfc` PostCSS Scoped 插件在解析 `:global(selector) child` 或嵌套在 `:global(selector) { child { ... } }` 时，其词法转换器会**直接丢弃后续的子选择器**，将整个声明块直接挂载在 `:global()` 内部的目标选择器上。
编译产物显示：
- `dist/assets/GoogleTextField-*.css`：编译为了 `html.dark { border-color: #1e1f20; caret-color: #fff; }`，`.google-text-field__box` 与 `.google-text-field__input` 完全丢失；
- `dist/assets/AuthLayout-*.css`：编译为了 `html.dark { background-color: #0e0e0e !important; }`，`.auth-card`、`.auth-footer` 完全丢失；
- `dist/assets/PasskeyModal-*.css` / `dist/assets/recaptcha-*.css`：编译为了 `.dark { background-color: #282a2d; }`，卡片与输入框选择器完全丢失。

**正确处理方式**：
在 Vue 3 Scoped CSS 中，当样式受祖先节点（如 `<html class="dark">`）控制时，**严禁使用 `:global()` 包裹**。直接编写：
```scss
html.dark .auth-card {
  background-color: #0e0e0e !important;
}
```
Vue SFC Scoped 编译器会正确将其转换为：
```css
html.dark .auth-card[data-v-xxxx] {
  background-color: #0e0e0e !important;
}
```
既精确限定在当前组件的作用域属性 `data-v-xxxx`，又能精准响应 `<html>` 标签上的 `.dark` 类名。

### 2.2 环境因素：HMR 客户端端口配置与多前端服务端口隔离
1. **HMR 客户端配置**：`vite.config.ts` 中声明了 `hmr: { clientPort: 443 }`，用于适配生产反向代理。若开发者在本地直接通过 `http://127.0.0.1:3001` 打开页面，浏览器尝试与 `wss://localhost:443` 建立 WebSocket 热更新连接会失败，导致 Vite 无法自动触发页面热重载（需要手动刷新或强刷 `Ctrl + F5`）；
2. **双前端工程运行**：
   - 端口 `3000`：运行旧版前端 `volur-lite-view`；
   - 端口 `3001`：运行新一代前端 `volur-lite-view-NEXT`；
   - 需确保访问的是 `http://localhost:3001` 对应的新前端工程。

---

## 3. 全局重构与修复清单

对全端所有包含 `:global(html.dark)` 与 `:global(.dark)` 的组件与样式文件进行了彻底清洗与重构：

1. **`src/components/layouts/auth/AuthLayout.vue`**：
   - 将全屏流转遮罩 `.auth-scrim`、卡片级阻塞遮罩 `.auth-card__blocker`、卡片背景 `.auth-card`（`#0e0e0e`）、文字（`#e3e3e3`）、底栏 `.auth-footer`（`#e8eaed`）统一收敛至标准 `html.dark { ... }` 作用域；
   - 消除所有 `:global(html.dark)` 语法。

2. **`src/components/google/GoogleTextField.vue`**：
   - 将暗色模式输入框样式全部迁移至原生 `html.dark { .google-text-field { ... } }`；
   - 确保光标颜色（`caret-color: #fff`）、边框颜色（`border-color: #1e1f20`）、文字颜色（`var(--v-text-primary, #e3e3e3)`）以及移动端与桌面端自适应浮动标签背景（`#0e0e0e` / `#1e1f20`）精准编译为带有组件 scope 的 CSS 规则。

3. **`src/App.vue`**：
   - 移除 `:global(html.dark)`，改写为 `html.dark .global-lang-loading-mask`、`html.dark .lang-loading-card`、`html.dark .lang-loading-text`。

4. **`src/components/security/PasskeyModal.vue`**：
   - 清洗全部 6 处 `:global(.dark)`，替换为 `html.dark .passkey-modal-*`。

5. **`src/components/security/RecaptchaModal.vue`**：
   - 清洗全部 18 处 `:global(.dark)`，替换为 `html.dark .rc-*`。

6. **`src/components/security/recaptcha.vue`**：
   - 修复错误提示 `html.dark .recaptcha-error-msg`。

7. **`src/views/competition/CompetitionListView.vue`**：
   - 修复进度条暗色选择器 `html.dark { .progress-bar-primary, .progress-bar-secondary { ... } }`。

8. **`src/views/auth/signin/AccountChooserView.vue`**：
   - 修复账号选择列表中移除图标与状态标签的暗色选择器。

9. **`src/styles/auth-list.scss`**：
   - 清除全局 SCSS 中的非标准 `:global(...)` 包装，统一采用标准 `html.dark`。

---

## 4. 产物审查与验证

1. **编译构建**：执行 `npm run build`，零报错，全链路 4259 模块成功打包；
2. **CSS 产物逆向校验**：
   - `dist/assets/GoogleTextField-*.css`：确认输出 `html.dark .google-text-field__input[data-v-...]{caret-color:#fff}` 与 `html.dark .google-text-field__box[data-v-...]{border-color:#1e1f20}`；
   - `dist/assets/AuthLayout-*.css`：确认输出 `html.dark .auth-card[data-v-...]{background-color:#0e0e0e!important;color:#e3e3e3}` 与 `html.dark .auth-footer[data-v-...]{color:#e8eaed}`；
   - `dist/assets/PasskeyModal-*.css` 与 `dist/assets/recaptcha-*.css`：确认组件各级类名完全保留并绑定 `data-v-...`，彻底解决选择器降级脱落问题。
