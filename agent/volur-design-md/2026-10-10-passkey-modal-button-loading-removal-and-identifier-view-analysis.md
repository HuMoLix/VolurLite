# PasskeyModal 按钮加载动画移除与 IdentifierView 按钮交互规范分析

## 1. 任务背景

用户提出两点诉求：
1. 取消 [`PasskeyModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/security/PasskeyModal.vue) 中的按钮加载动画；
2. 深度分析 [`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue) 中的按钮是否也均没有加载动画。

---

## 2. PasskeyModal 按钮加载动画移除

### 2.1 变更根因与设计规范
在 Google Material Design / Accounts 设计体系中，卡片或弹窗的加载状态由**顶部的 Indeterminate 线性进度条**（Linear Progress Bar）与**事件阻断遮罩（Blocker / Scrim）**统一呈现：
- 此前在 `PasskeyModal.vue` 中，主按钮额外绑定了 `:loading="isScrimLoading && activeAction === 'primary'"`，导致在触发通行密钥流程时，模态框顶部既有双轨线性流动条（`passkey-modal__progress`），按钮内部又出现环形旋转 Spinner，造成重复动画干扰；
- 按钮内部出现旋转 Spinner 会推挤内部文字、导致按钮排版抖动，与整体 Google 认证体系的克制反馈风格不符。

### 2.2 修改实现
1. 在模板中彻底移除主按钮的 `:loading` 属性绑定，保留 `:disabled="isScrimLoading"` 与 `:isScrim="true"`；
2. 清理仅用于控制按钮 `:loading` 的响应式状态 `const activeAction = ref(...)` 及其在 `handleClose`、`handleOtherDevice`、`handlePrimaryAction` 和 `runWithScrim` 中的赋值。

---

## 3. IdentifierView 按钮加载机制深度分析

经全面审查 [`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue) 及其依赖的组件与状态机：

### 3.1 结论
**是的，`IdentifierView.vue` 中的所有按钮与可点击链接完全没有按钮内部的局部 Spinner 加载动画。**

### 3.2 详细依据与交互链路
1. **底部操作栏组件属性传递**：
   在 `IdentifierView.vue` 中，底部按钮通过 `<AuthActionBar>` 组织：
   ```html
   <AuthActionBar
     :primary-text="t('auth.next')"
     :secondary-text="t('auth.create_account')"
     :primary-disabled="loading"
     :secondary-disabled="loading"
     primary-class="btn-next-step"
     secondary-class="btn-create-account"
     @primary="handleNext"
     @secondary="handleCreateAccount"
   />
   ```
   - 显式透传了 `:primary-disabled="loading"` 与 `:secondary-disabled="loading"`；
   - **未传入 `:primary-loading` 或 `:secondary-loading`**（在 `AuthActionBar` 的 Props 默认值中两者均默认为 `false`）；
   - 因此底层渲染的 `<n-button>` 内部绝不会出现旋转加载菊花（Spinner）。

2. **全局容器与加载状态联动**：
   当用户在 `IdentifierView.vue` 中点击“下一步”时，调用 `signinFlowStore.submitIdentifier(trimmed)`：
   - Store 内部通过 `withLoading(...)` 管控请求，将 `signinFlowStore.loading` 置为 `true`（至少持续 1000ms 最小过渡时长）；
   - 外层布局 [`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue) 感知到 Store 的 `loading === true`，激活以下三重交互：
     1. **顶部 Material 双轨线性进度条**（`auth-card__progress`）：在白色卡片正上方边缘展示 Google 标志性的横向双段交替循环移动动画（蓝色流动条）；
     2. **全屏 Scrim 遮罩层**（`auth-scrim`）：在页面背景上渐变浮现一层遮罩蒙层（`auth-scrim-fade`）；
     3. **卡片事件阻断器**（`auth-card__blocker`）：在卡片表面生成透明阻止层，截断用户的重复点击或鼠标键盘操作；
     4. **按钮禁用态**：`IdentifierView.vue` 中的 `:primary-disabled="loading"` 和 `:secondary-disabled="loading"` 变为 `true`，主按钮呈现禁用变灰透明度（`opacity: 0.6; cursor: not-allowed`），但文字保持为“下一步”。

3. **其他辅助交互元素**：
   - “忘记了用户名？”链接（`auth-forgot-link`）：原生 `<a>` 标签，点击后触发 Scrim 页面平滑过渡，无按钮动画；
   - “选择账号”链接（`auth-forgot-link`）：原生 `<a>` 标签，同上；
   - “了解详情”链接（`auth-learn-more-link`）：原生 `<a>` 标签，同上。

---

## 4. 验证与质量保证

1. **零注释铁律恪守**：全量改动代码完全遵循零行内注释规范；
2. **前端编译打包测试**：运行 `npm run build`（`vue-tsc --noEmit && vite build`），2916 个模块转换通过，0 TypeScript 错误。
