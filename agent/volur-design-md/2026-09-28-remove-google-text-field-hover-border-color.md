# 施工设计与审计记录：移除 GoogleTextField 悬停态边框变色交互

- **文件编号**：`2026-09-28-remove-google-text-field-hover-border-color`
- **日期**：2026-09-28
- **影响范围**：
  - `src/volur-lite-view-NEXT/src/components/google/GoogleTextField.vue`

---

## 1. 变更背景与需求分析

在 `volur-lite-view-NEXT` 的 `GoogleTextField.vue` 组件设计中，输入框容器外边框此前配置了鼠标悬停（Hover）加深轮廓颜色的交互样式（亮色下加深为 `#202124`，暗色下加深为 `#e8eaed`）。

根据用户明确指令要求：
- 取消输入框在 hover 状态下改变边框颜色的交互行为；
- 保持常态未聚焦、非报错时的边框颜色（亮色为 `var(--v-border-base, #dadce0)`，暗色为 `#3c4043`）在鼠标悬停时始终稳定不变；
- 继续完整保留输入框在聚焦激活（Focused）、校验失败（Error）以及禁用（Disabled）状态下的边框颜色与过渡动画规范。

---

## 2. 核心改动点与实现方案

### 2.1 移除亮色与暗色模式下的 Hover 边框颜色覆盖规则

在 [`GoogleTextField.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/google/GoogleTextField.vue) 的样式定义中，分别移除了亮色与暗色模式下对 `&__box` 的悬停样式块：

1. **亮色模式**：
   - 移除了以下规则：
     ```scss
     &:hover:not(&--focused):not(&--error):not(&--disabled) {
       border-color: #202124; // 悬停态适度加深轮廓
     }
     ```
2. **暗色模式（`:global(html.dark)`）**：
   - 移除了以下规则：
     ```scss
     &:hover:not(&--focused):not(&--error):not(&--disabled) {
       border-color: #e8eaed;
     }
     ```

### 2.2 影响分析与回归保证

- **常态（Normal）**：鼠标移入/移出输入框区域时，边框颜色保持固定底色，不再发生明暗变化；
- **聚焦态（Focused）**：获得焦点时保持标准 Google 蓝高亮描边；
- **错误态（Error）**：输入错误时依然强制展示 Google 红色边框并高亮；
- **禁用态（Disabled）**：保持半透明与不可交互光标。

---

## 3. 构建与验证

在 `src/volur-lite-view-NEXT` 根目录下执行生产构建与类型检查：
```bash
npm run build
```
执行结果：`vue-tsc --noEmit && vite build` 检查 100% 通过，产物构建无报错。
