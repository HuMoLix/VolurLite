# 重构审计：统一采用 Naive UI Quaternary 按钮重构前端所有次操作按钮

## 1. 变更背景与动机

在 `volur-lite-view-NEXT` 前端工程中，认证体系涵盖登录（Identifier / AccountChooser）、挑战验证（Password, Email OTP, SMS OTP, Passkey, ReCAPTCHA, Selection, Reject）、注册流程（Name, Email, Etotp, Options, Passkey/TOTP Protect）、找回流程（Username / Password Recovery）以及安全弹窗（PasskeyModal）。

此前，页面中的主要操作按钮统一采用了 Naive UI 的实体胶囊按钮：
```html
<n-button type="primary" :round="true" class="auth-action-btn btn-next-step" ...>
```
然而，所有次级操作按钮（如“创建账号”、“尝试其他方式”、“移除账号”、“改用其他设备”、“返回登录”等）此前大量采用了原生 HTML `<button type="button">` 标签。这导致：
1. **组件库生态割裂**：未能充分享受 Naive UI 统一的主题状态机、水波纹动效（Ripple）、键盘无障碍（Focus ring）以及受控 `loading`/`disabled` 行为；
2. **样式一致性维护成本高**：原生按钮需手工定义外边距、内边距、圆角与触摸反馈，且容易因浏览器默认样式差异导致排版偏差；
3. **架构规范对齐需求**：为了严格对齐 `view-NEXT-rules.md` 中的 Google Material Design 现代设计规范与 Naive UI 全局组件体系，需将全站所有次级操作按钮统一重构为 Naive UI 的透明无边框四级按钮（`quaternary`）。

---

## 2. 方案设计与实现规范

### 2.1 统一组件范式 (方案一)

所有透明次级操作按钮统一重构为：
```html
<n-button
  quaternary
  type="primary"
  :round="true"
  class="auth-action-btn btn-secondary-action"
  :disabled="isLoading"
  @click="handleAction"
>
  {{ $t('...') }}
</n-button>
```

#### 关键属性设计：
- `quaternary`：Naive UI 专用的四级透明背景模式（默认背景与边框均为 `transparent`，Hover 时自动激活微高亮水波底色）；
- `type="primary"`：绑定主色语义（Google Blue），便于组件库内部派发正确的状态与色彩变量；
- `:round="true"`：保持与主操作胶囊按钮完全一致的高品质圆角曲线；
- `class="auth-action-btn [btn-class]"`：保留语义化类名，无缝兼容 `AuthLayout.vue` 的全局 Scrim 遮罩监听机制；
- 继承 `:disabled` / `@click` 事件绑定，杜绝任何功能逻辑降级。

### 2.2 样式穿透与色彩精准覆盖

Naive UI 的 `<n-button>` 将文本内容包裹于内部的 `.n-button__content` 容器中，状态边框包裹于 `.n-button__state-border` 中。为严格实现 Google Material 视觉标准（亮色 `#0b57d0`，暗色 `#8ab4f8`），在对应单文件组件的 scoped SCSS 中统一增强了样式穿透规则：

```scss
.auth-action-btn.btn-secondary-action,
.auth-action-btn.btn-create-account,
.auth-action-btn.btn-try-another-way {
  border: none !important;
  outline: none;
  background: transparent;
  padding: 0 16px;
  height: 40px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 9999px;
  cursor: pointer;
  transition: background-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  :deep(.n-button__content) {
    color: #0b57d0 !important;
    font-size: 14px;
    font-weight: 500;
    transition: color 0.2s ease;
  }

  :deep(.n-button__state-border) {
    border: none !important;
  }

  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
  }

  &:active {
    background-color: rgba(11, 87, 208, 0.16) !important;
  }

  &.n-button--disabled {
    cursor: not-allowed;
    background: transparent !important;
    opacity: 0.38;

    :deep(.n-button__content) {
      color: #3c4043 !important;
    }
  }
}

:global(body.dark) {
  .auth-action-btn.btn-secondary-action,
  .auth-action-btn.btn-create-account,
  .auth-action-btn.btn-try-another-way {
    :deep(.n-button__content) {
      color: #8ab4f8 !important;
    }

    &:hover {
      background-color: rgba(138, 180, 248, 0.08) !important;
    }

    &:active {
      background-color: rgba(138, 180, 248, 0.16) !important;
    }

    &.n-button--disabled {
      :deep(.n-button__content) {
        color: #e8eaed !important;
      }
    }
  }
}
```

---

## 3. 涉及重构文件清单 (共 22 个文件)

### 3.1 登录模块 (Signin)
1. `src/views/auth/signin/IdentifierView.vue`：
   - 将“创建账号”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-create-account">`。
2. `src/views/auth/signin/AccountChooserView.vue`：
   - 将“移除账号”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-remove-account">`。

### 3.2 挑战流程模块 (Signin Challenge)
3. `src/views/auth/signin/challenge/pwdChallengeView.vue`：
   - 将“尝试其他方式”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-try-another-way">`。
4. `src/views/auth/signin/challenge/emailotpChallengeView.vue`：
   - 将“尝试其他方式”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-try-another-way">`。
5. `src/views/auth/signin/challenge/passkeyChallengeView.vue`：
   - 将“尝试其他方式”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-try-another-way">`。
6. `src/views/auth/signin/challenge/smsotpChallengeView.vue`：
   - 将“尝试其他方式”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-try-another-way">`。
7. `src/views/auth/signin/challenge/recaptchaChallengeView.vue`：
   - 将“尝试其他方式”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-try-another-way">`。
8. `src/views/auth/signin/challenge/rejectView.vue`：
   - 显式引入 `NButton`，并将返回登录重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-try-another-way">`。
9. `src/views/auth/signin/challenge/selectionView.vue`：
   - 显式引入 `NButton`，并将取消返回重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-try-another-way">`。

### 3.3 注册模块 (Signup)
10. `src/views/auth/signup/steps/NameStepView.vue`：
    - 将“改用现有账号登录”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-signin-instead">`。
11. `src/views/auth/signup/steps/EmailStepView.vue`：
    - 将“返回”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-secondary-action">`。
12. `src/views/auth/signup/steps/EtotpChallengeStepView.vue`：
    - 将“返回修改邮箱”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-secondary-action">`。
13. `src/views/auth/signup/steps/OptionsChallengeStepView.vue`：
    - 将“返回”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-secondary-action">`。
14. `src/views/auth/signup/protect/passkeyProtectView.vue`：
    - 将“以后再说”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-secondary-action">`。
15. `src/views/auth/signup/protect/totpProtectView.vue`：
    - 将“以后再说”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-secondary-action">`。

### 3.4 找回密码与账号模块 (Recovery)
16. `src/views/auth/recovery/challenge/recaptchaChallengeView.vue`：
    - 将“返回”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-secondary-action">`。
17. `src/views/auth/recovery/password/EmailOtpView.vue`：
    - 将“尝试其他方式”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-secondary-action">`。
18. `src/views/auth/recovery/password/ResetView.vue`：
    - 将“返回”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-secondary-action">`。
19. `src/views/auth/recovery/password/SelectionView.vue`：
    - 显式引入 `NButton`，并将“返回”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-secondary-action">`。
20. `src/views/auth/recovery/username/EmailView.vue`：
    - 将返回操作重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-secondary-action">`；
    - 同步修复成功视图中误用的原生 `<button class="auth-action-btn btn-primary-action">` 为标准的 `<n-button type="primary" :round="true">`。
21. `src/views/auth/recovery/username/SelectionView.vue`：
    - 显式引入 `NButton`，并将“返回”重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-secondary-action">`。

### 3.5 安全通用组件 (Security Components)
22. `src/components/security/PasskeyModal.vue`：
    - 将弹窗底部的“取消”按钮与“改用其他设备”按钮重构为 `<n-button quaternary type="primary" :round="true" class="auth-action-btn btn-create-account">`。

---

## 4. 安全评估与回归验证

1. **Scrim 全局遮罩机制兼容性**：
   - `AuthLayout.vue` 中对加载动画遮罩的监听通过 `event.target.closest('[isScrim="true"]')` 及特定样式类名（如 `btn-secondary-action`、`btn-create-account`、`btn-try-another-way`）实现。
   - Vue 3 对自定义组件根节点具备属性自动透传特性（Attribute Fallthrough），`<n-button>` 渲染后的真实 HTML 根节点包含对应类名及 `:isScrim="true"` 属性，遮罩逻辑完全兼容并稳定运作。
2. **事件传播与状态防重**：
   - 所有按钮均绑定真实响应式 `isLoading` / `isSubmitting` 禁用状态（`:disabled`），且在全局过渡遮罩激活时保持禁用，防止双击或重放请求。
3. **零注释铁律验证**：
   - 所有 22 个代码文件均保持极致纯净，无任何违规行内注释或设计说明残留。
4. **编译与类型检查**：
   - 执行 `npm run build`（包含 `vue-tsc --noEmit && vite build`），2916 个模块转换顺利通过，零 TypeScript 诊断告警，构建产物正常生成。
