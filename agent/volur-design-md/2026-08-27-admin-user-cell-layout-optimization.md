# 管理端用户单元格排版与全局选择框占位符对齐优化审计记录

## 1. 变更背景与动因
1. **用户单元格排版规范**：管理端用户表格单元格保持左侧原生无渐变头像、右侧上下两行紧凑个人信息（上行用户名、下行邮箱）；
2. **选择框与搜索框占位符居中修复（方案 A）**：针对 Naive UI `NSelect` 及 `n-base-selection` 组件内部占位符（`.n-base-selection-placeholder__inner`）因 `line-height: 1.5` 导致文字垂直不居中（视觉下沉）的底层字体度量与行盒模型问题，在全局样式表 `src/styles/index.scss` 中应用方案 A 进行全局修复。

---

## 2. 改动点详述

### 2.1 全局样式表增强（`src/styles/index.scss`）
- 注入全局选择器规则：
```scss
.n-base-selection {
  .n-base-selection-placeholder__inner,
  .n-base-selection-label__render-label,
  .n-base-selection-input__content {
    line-height: 1 !important;
    display: inline-flex;
    align-items: center;
  }
}

.n-input {
  .n-input__placeholder > span {
    line-height: 1 !important;
  }
}
```
- **原理解析**：消除额外 Half-Leading 导致的基线偏移，将行盒高度精确收敛至字体实际物理高度，配合 Flex `align-items: center` 彻底实现像素级垂直居中。

---

## 3. 影响范围与回归验证
- **受影响文件**：
  - [`src/volur-lite-view/src/styles/index.scss`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/styles/index.scss)
  - [`src/volur-lite-view/src/views/admin/user/AdminUserView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/user/AdminUserView.vue)
- **编译与构建验证**：
  - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），0 错误，打包构建成功通过。
