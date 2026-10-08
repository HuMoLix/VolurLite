# 施工记录：修复登录核验流程 (/v1/signin/challenge/etotp) 中验证按钮 Scrim 遮罩与延迟时序缺陷

## 1. 缺陷背景与现象定位

在 Volur Lite 新一代前端工程（`volur-lite-view-NEXT`）中，用户在进入邮箱动态验证码核验页面（`/v1/signin/challenge/etotp`）并输入 6 位验证码后点击“验证”按钮时，出现以下交互与时序异常：
1. **Scrim 遮罩与进度条无感知或闪烁消失**：用户点击“验证”提交后，界面上的全局 Scrim 遮罩层与卡片顶部线性进度条（Progress Bar）未维持预期的 1000ms 最小加载周期，而是在 20~40ms 瞬间闪退，随后界面呈现长达近 1 秒的无遮罩无进度假死状态，最后突然切页；
2. **模板主按钮缺失声明式 `:isScrim="true"` 属性**：在 `emailotpChallengeView.vue`、`pwdChallengeView.vue` 等核验视图中，次要按钮（“尝试其他方式”）均具备 `:isScrim="true"`，但主操作“验证/下一步”按钮缺失该属性；
3. **按钮缺失 `cursor: pointer` 与禁用态规范样式**：在 Tailwind 禁用 Preflight 的环境下，主按钮未显式声明 `cursor: pointer`，且禁用态光标与透明度缺失；
4. **多步骤流转跳转缺失**：在 `emailotpChallengeView.vue` 中，若服务端下发后续安全核验流程（`nextStep`），未进行路由推进判断。

---

## 2. 根因深度剖析 (Root Cause Analysis)

### 2.1 Pinia Store `resetFlow()` 提前注销 `loading` 导致 Scrim 提前夭折
在 [`signinFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts) 的 `submitChallenge` 方法中，外部使用 `withLoading(action, 1000)` 控制最小加载周期。
然而，当接口返回 `status === 'SUCCESS'` 或第二步认证成功时，代码在 `action` 回调内部直接调用了 `resetFlow()`。
在原 `resetFlow()` 实现中，硬编码执行了 `loading.value = false`：
1. 本地接口请求仅需 20~40ms，`resetFlow()` 立即将 `loading.value` 设为 `false`；
2. [`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue) 计算属性 `isScrimVisible = computed(() => isCardLoading.value)` 瞬间响应为 `false`，全局遮罩与顶部进度条在第 40ms 被强制销毁；
3. 随后 `withLoading` 内部继续执行剩余 ~960ms 的保底睡眠（`setTimeout`）；在此期间，由于 `loading.value` 已被提前卸载，界面失去了所有加载反馈，造成严重的“无 Scrim 遮罩和延迟”体验割裂。

### 2.2 按钮模板属性与指针交互规范缺失
`emailotpChallengeView.vue`、`pwdChallengeView.vue`、`smsotpChallengeView.vue`、`recaptchaChallengeView.vue`、`passkeyChallengeView.vue` 以及 `IdentifierView.vue` 的主操作按钮缺少 `:isScrim="true"` 语义绑定、缺少 `:loading="isSubmitting"` 及 `:disabled="loading || isSubmitting"`，且在 Scss 中缺失了 `cursor: pointer` 与 `&:disabled { cursor: not-allowed; opacity: 0.6; }`。

---

## 3. 具体修改实施

### 3.1 `signinFlow.ts` 时序保护与 `resetFlow` 重构
- 为 [`resetFlow(keepLoading = false)`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts) 增加 `keepLoading` 可选参数，允许仅清理认证票据与会话凭证，而不中断外部异步流程管控的 `loading` 状态；
- 在 `submitChallenge` 成功分支中改用 `resetFlow(true)`，使 `loading.value = true` 持续稳定贯穿整个 1000ms 周期，最终由 `withLoading` 的 `finally` 阶段统一回收，彻底消除遮罩提前闪退问题。

### 3.2 `emailotpChallengeView.vue` 模板与样式全面对齐
- 为 `.btn-verify-action` 补全 `:isScrim="true"`、`:loading="isSubmitting"` 与 `:disabled="loading || isSubmitting"`；
- 为 `.btn-try-another-way` 补齐 `:disabled="loading || isSubmitting"`；
- 在 `handleVerifyChallenge` 中增加 `else if (res.nextStep)` 分支，支持连续核验跳转；
- 补充 `cursor: pointer`、`&:disabled { cursor: not-allowed; opacity: 0.6; }` 及暗色模式适配。

### 3.3 登录安全挑战族组件交互样式协同加固
- 对齐 [`pwdChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/pwdChallengeView.vue)、[`smsotpChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/smsotpChallengeView.vue)、[`recaptchaChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/recaptchaChallengeView.vue)、[`passkeyChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/passkeyChallengeView.vue) 及 [`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue) 的 `:isScrim="true"` 声明、禁用态联动、光标规范与样式穿透。

---

## 4. 验证与质量保证

1. **类型检查与前端编译**：
   运行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程 TypeScript 0 错误通过，打包成功（2911 个模块转化，耗时 6.22s）。
2. **规范恪守**：
   全量代码严格恪守零行内注释铁律（Zero Comments Rule），所有设计与技术分析完整归档至本文档。
