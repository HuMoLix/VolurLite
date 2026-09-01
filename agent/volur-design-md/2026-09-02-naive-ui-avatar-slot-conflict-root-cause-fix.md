# 施工与审计记录：Naive UI Avatar 默认插槽与图片源冲突根因修复及调试日志清理

## 1. 基本信息

- **任务名称**：根治 Naive UI `<n-avatar>` 插槽内容劫持图片渲染、清理控制台临时调试日志
- **执行时间**：2026-09-02
- **影响模块**：
  - `volur-lite-view`：
    - `src/views/profile/ProfileView.vue`（按条件仅在无头像时提供 slot 字符、移除调试日志）
    - `src/layout/AppLayout.vue`（按条件仅在无头像时提供 slot 字符、移除调试日志）
    - `src/layout/AdminLayout.vue`（按条件仅在无头像时提供 slot 字符）
    - `src/views/admin/user/AdminUserView.vue`（表格中仅在无头像时提供 default slot）

---

## 2. 根本原因剖析 (Root Cause)

根据 Naive UI `Avatar.mjs` 源码实现：
```js
else img = resolveWrappedSlot($slots.default, children => {
  if (children) return ... <span>{{ children }}</span>;
  else if (src || imgProps.src) return h("img", { src: this.src, ... });
});
```
- **Naive UI 的设计机制**：当使用 `<n-avatar :src="src">{{ username }}</n-avatar>` 时，组件检测到 `default slot` 存在内容（`children` 非空），**会直接进入 `if (children)` 分支渲染文字 `<span>`，完全跳过 `else if (src)` 分支，根本不会在 DOM 中创建 `<img>` 标签，更不会向后端发起图片网络请求**！
- **修复措施**：
  - 将所有 `<n-avatar>` 的内部文字插槽改为条件渲染 `<template v-if="!avatarUrl">文字</template>`；
  - 只有当 `avatarUrl` 为空或不存在时，才向 `<n-avatar>` 提供文本插槽；当 `avatarUrl` 存在时，插槽为空，Naive UI 即可 100% 正确创建并渲染 `<img :src="avatarUrl">` 标签。

---

## 3. 验证与审计结论

- **前端构建**：执行 `npm run build` 打包构建，TypeScript 类型检查全部通过（0 错误）。
- **后端测试**：全平台 12 个模块执行 `mvn test` 单元测试全部通过（0 失败，0 错误）。
