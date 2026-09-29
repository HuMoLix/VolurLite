# 施工设计与审计记录：修复 GoogleTextField 报错态焦点颜色与对齐错误提示

- **文件编号**：`2026-09-28-fix-google-text-field-error-focus-color-and-align-errormsg`
- **日期**：2026-09-28
- **影响范围**：
  - `src/volur-lite-view-NEXT/src/components/common/GoogleTextField.vue`

---

## 1. 变更背景与问题定位

在上一阶段对 `GoogleTextField.vue` 进行 Google Accounts 风格重构后，经过交互验证发现两个细节瑕疵：
1. **报错态下聚焦颜色竞争 Bug**：
   - 当点击“下一步”或“注册”按钮触发校验失败时，输入框边框变红（`#ea4335`）；但当用户点击该输入框准备纠错时，由于 `:focus` 选择器未对错误态做隔离拦截（`&__input:focus ~ &__label` 声明了带有 `!important` 的 Google Blue `#0b57d0`），导致浮动在边框上的 placeholder 瞬间被覆盖染成蓝色，产生“边框为红、标签为蓝”的视觉违和感。在 Google Accounts 规范中，**只要输入框处于报错状态，无论是静止还是聚焦编辑，标签文字、边框与光标都必须全流程保持红色**。
2. **错误提示文案边距偏离**：
   - 此前 `.google-text-field__error-msg` 设置了 `margin-left: 12px`，导致报错图标和说明文字向右缩进，未与输入框的左侧外边框保持垂直平齐对齐。

---

## 2. 核心改动点与实现方案

### 2.1 报错态与聚焦态样式解耦与权重重构
在 `GoogleTextField.vue` 的 SCSS 规则中重构了状态权重：
1. **状态选择器隔离**：
   - 将原无条件生效的聚焦变蓝规则限定在非报错分支下：
     ```scss
     &:not(&--error) &__input:focus ~ &__label {
       color: #0b57d0 !important;
       font-weight: 500;
     }
     ```
   - 报错状态（`--error`）拥有最高视觉优先级：无论是否 focus、是否有输入值，均强制维持 Google Red：
     ```scss
     &--error &__label,
     &--error &__input:focus ~ &__label,
     &--error &__input:not(:placeholder-shown) ~ &__label {
       color: #ea4335 !important;
       font-weight: 500;
     }

     &--error,
     &--error.google-text-field__box--focused {
       border-color: #ea4335 !important;
       box-shadow: 0 0 0 1px #ea4335 !important;
     }

     &--error &__input {
       caret-color: #ea4335;
     }
     ```
2. **暗色模式同步适配**：
   - 暗色模式下报错色统一为高可读性 Google Red 200（`#f28b82`），确保暗色聚焦时同样锁定红色边框、红色上浮标签与红色光标。

### 2.2 取消 `errorMsg` 的 `margin-left` 并严格左对齐
- 将 `.google-text-field__error-msg` 与 `.google-text-field__helper-text` 中的 `margin-left: 12px` 移除，显式声明 `margin-left: 0; padding-left: 0;`；
- 使警告圆标 SVG 和错误提示文本起始坐标与输入框外层边框盒的 x=0 基准线完美对齐。

---

## 3. 构建验证

- 执行 `npm run build` (`vue-tsc --noEmit && vite build`)；
- TypeScript 类型检查 100% 通过，产物构建用时 2.79s，无任何告警。
