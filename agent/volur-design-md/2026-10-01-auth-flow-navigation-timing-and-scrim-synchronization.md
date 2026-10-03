# 认证与注册流程交互时序对齐与全局遮罩/进度条同步规范

## 1. 变更背景与问题剖析

在 `src/volur-lite-view-NEXT` 现代前端工程中，用户认证（Sign-in）与注册（Sign-up）流转交互存在多处时序错位与视觉反馈缺失问题，严重背离了 Google Workspace / Material Design 的顺滑无缝体验：

1. **时序倒置缺陷（先跳转后延迟）**：
   - 在 `OptionsChallengeStepView.vue`、`PasskeyProtectView.vue`、`selectionView.vue` 等组件中，原交互实现将 `await router.push(...)` 包装在 `triggerScrim(action)` 回调内部的第一行执行。
   - 由于 `router.push` 为微任务同步/近即时触发路由切换与 DOM 卸载，导致目标新页面瞬间挂载渲染，而原定 1000ms 的进度条动画与遮罩延迟反而在新页面上继续跑完，产生了“先跳页面、新页面再被盖上遮罩转圈”的严重视觉时序倒置。

2. **遮罩提前掐断缺陷（跳过/继续按钮无遮罩）**：
   - 在 `OptionsChallengeStepView.vue` 的 `handleSkip` / `handleContinue`、`passkeyProtectView.vue` 的 `handleFinish` 以及 `sessionExpireView.vue` 的 `handleRetry` 中，原逻辑在动作回调或方法执行之初就调用了 `flowStore.resetFlow()`。
   - `resetFlow()` 内部无条件执行了 `loading.value = false`，导致 `AuthLayout.vue` 监听的 `isCardLoading` 状态在毫秒级内被强制掐断重置，全屏遮罩与双波浪进度条根本来不及被浏览器渲染流水线呈现。

3. **跨业务流 Store 状态孤岛缺陷（“改用现有账号登录”与“创建账号”无遮罩）**：
   - 在 `AuthLayout.vue` 中，卡片遮罩与全局双波浪进度条原先绑定的是计算属性 `currentFlowStore`，其判定直接依据当前 URL（如以 `/v1/signup` 开头则监听 `signupFlowStore`，否则监听 `signinFlowStore`）。
   - 当用户在 `IdentifierView.vue` 点击“创建账号”时，触发的是 `signinFlowStore.triggerScrim()`（使得 `signinFlowStore.loading = true`）；但路由一旦切换至 `/v1/signup/steps/name`，`AuthLayout` 立即依据新路由把 `currentFlowStore` 切到 `signupFlowStore`，而此时 `signupFlowStore.loading` 为 `false`，导致遮罩瞬间蒸发。反之，在 `NameStepView.vue` 点击“改为现有账号登录”亦同。

---

## 2. 核心架构重构与交互时序标准

### 2.1 双流 Store 聚合监听范式 (Dual-Store Scrim Aggregation)
在 `AuthLayout.vue` 中，彻底解耦遮罩渲染与单纯当前 URL 的弱绑定，将卡片加载态与遮罩可见性统一重构为聚合双 Store 的响应式计算：
```ts
const isCardLoading = computed(() => {
  if (typeof props.loading === 'boolean') {
    return props.loading
  }
  return signinFlowStore.loading || signupFlowStore.loading
})
```
同时保留底层卡片事件代理委托，识别具有 `:isScrim="true"` 或特定语义 class 的操作元素，保证跨路由、跨流程导航期间遮罩与进度条具备无缝生命周期。

### 2.2 纯前端页面导航交互标准范式 (Pre-Navigation Scrim Await Rule)
对于涉及路由切换但无即时后端 API 响应等待的按钮操作（包括“创建账号”、“改用现有账号登录”、“跳过”、“继续”、“返回上一步”、“试试其他方式”等），统一确立先等待当前视图遮罩与进度条完整呈现完毕、再执行状态清理与路由推进的严格顺序：
```ts
async function handleAction() {
  if (flowStore.loading) return
  // 1. 在当前视图页面平稳呈现遮罩与双波浪进度条 1000ms
  await flowStore.triggerScrim(undefined, 1000)
  // 2. 若涉及状态清理，在遮罩延迟结束后执行
  flowStore.resetFlow()
  // 3. 平滑切换至目标路由
  await router.push(targetPath)
}
```
**铁律约束**：严禁在 `triggerScrim` 的回调参数内直接包裹 `router.push`，杜绝一切微任务导致的页面预切换与时序颠倒。

---

## 3. 改动文件与具体改动点清单

1. **`src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue`**：
   - 重构 `isCardLoading` 为 `signinFlowStore.loading || signupFlowStore.loading`，确保 Sign-in 与 Sign-up 跨模块跳转时遮罩平滑不掉线；
   - 完善全局事件委托支持。

2. **`src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue`**：
   - “创建账号”按钮添加 `:isScrim="true"`；
   - `handleCreateAccount` 改为先 `await signinFlowStore.triggerScrim(undefined, 1000)`，再执行 `await router.push(...)`。

3. **`src/volur-lite-view-NEXT/src/views/auth/signup/steps/NameStepView.vue`**：
   - “改为现有账号登录”按钮添加 `:isScrim="true"`；
   - `goToSignIn` 改为先 `await signupFlowStore.triggerScrim(undefined, 1000)`，再执行 `await router.push(...)`。

4. **`src/volur-lite-view-NEXT/src/views/auth/signup/steps/OptionsChallengeStepView.vue`**：
   - 保护选项卡片列表项、跳过按钮与继续按钮添加 `:isScrim="true"`；
   - `handleSelectOption`、`handleSkip`、`handleContinue` 改为先等待 1000ms `triggerScrim` 结束，再执行 `resetFlow()` 与路由跳转，根除先跳后遮缺陷。

5. **`src/volur-lite-view-NEXT/src/views/auth/signup/steps/EmailStepView.vue` 与 `EtotpChallengeStepView.vue`**：
   - `goBackName` 与 `goBackEmail` 统一为先等待 1000ms `triggerScrim` 再切路由。

6. **`src/volur-lite-view-NEXT/src/views/auth/signup/protect/passkeyProtectView.vue`**：
   - `handleBackOptions` 与 `handleFinish` 改为先执行 `triggerScrim` 延迟，再执行 `resetFlow` 与 `router.push`。

7. **`src/volur-lite-view-NEXT/src/views/auth/signup/protect/totpProtectView.vue`**：
   - `handleBackOptions` 改为先等待 `triggerScrim` 结束再跳转。

8. **`src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue` 与 `ChallengeView.vue`**：
   - 顶栏用户名/账号 Chip 点击返回方法（`goBackName`、`goBackIdentifier`）统一为等待 1000ms 遮罩后再路由。

9. **`src/volur-lite-view-NEXT/src/views/auth/signin/challenge/` 子视图组件**：
   - 覆盖 `selectionView.vue`、`pwdChallengeView.vue`、`totpChallengeView.vue`、`deviceChallengeView.vue`、`recaptchaChallengeView.vue`、`rejectView.vue`；
   - 剔除各方法中硬编码的 `setTimeout(resolve, 800)` 冗余逻辑，统一步调为 `await triggerScrim(undefined, 1000)` 后平滑推进路由；各操作按钮与返回按钮补齐 `:isScrim="true"` 属性。

10. **`src/volur-lite-view-NEXT/src/views/auth/signup/steps/PasswordStepView.vue`**：
    - “完成注册”按钮补齐 `:isScrim="true"`。

11. **`src/volur-lite-view-NEXT/src/views/auth/sessionExpireView.vue`**：
    - “重试”按钮补齐 `:isScrim="true"`；
    - `handleRetry` 调整为先执行遮罩等待，再执行双 Store `resetFlow()` 并跳转。

---

## 4. 验证与质量保证

1. **类型安全检验**：
   - 运行 `npx vue-tsc --noEmit`，全项目 TypeScript 类型检查 0 报错通过。
2. **工程打包构建**：
   - 运行 `npm run build`，生产构建顺利完成。
3. **零注释铁律审计**：
   - 所有 `.vue` 与 `.ts` 代码文件保持绝对纯净，未注入任何行内解释性注释或临时备注。
