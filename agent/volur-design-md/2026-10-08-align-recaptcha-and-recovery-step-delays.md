# 账号找回全流程人机核验延迟与路由流转优化方案

## 1. 变更背景与核心问题

在 Volur Lite 新一代前端（`volur-lite-view-NEXT`）中，账号找回流转与安全核验体验存在以下交互与时序缺陷：
1. **reCAPTCHA 复选框验证加载过快**：当用户在 `recaptcha.vue` 中点击“我不是机器人”复选框时，由于本地接口响应极快（通常 10~30ms），复选框在眨眼间就由旋转加载器切换为通过对勾，缺乏原生 reCAPTCHA 应有的安全核验判定停留感。此处的 loading 需维持至少 1500ms 的旋转加载状态，且仅在复选框组件内部进行延迟，不触发页面或卡片的全局 `triggerScrim` 遮罩层。
2. **安全挑战页（`recaptchaChallengeView.vue`）跳转时序提前**：在 `/v1/recovery/challenge/recaptcha` 页面中，用户完成人机验证后点击“下一步”，页面在 1000ms 的按钮/卡片遮罩延迟尚未结束前即触发了 `router.push` 页面跳转，导致遮罩动画被生硬截断，破坏平滑流转。
3. **输入邮箱页（`EmailView.vue`）提交流程时序提前**：在 `/v1/recovery/username/email` 页面中，用户输入邮箱并点击“下一步”时，接口请求完成后立即切换了 `isSubmitted` 状态，未能保证至少 1000ms 的卡片进度与遮罩持续时间。
4. **冗余 Toast 弹窗与指针样式缺失**：
   - 邮箱提交成功后，除页面内已有的成功卡片面板外，还弹出了 `window.$message?.success` 弹窗，造成 UI 信息冗余；
   - 成功面板中的“返回登录”主按钮样式未声明 `cursor: pointer`，在禁用 Tailwind Preflight 的环境下呈现为默认文本指针箭头（`cursor: default`）。

---

## 2. 深入原因分析与排查

1. **reCAPTCHA 独立组件时钟隔离设计**：
   `recaptcha.vue` 内部拥有自包含的沙箱动画机制（旋转加载器 `.rc-spinner`）。若将其与 `AuthLayout.vue` 的 `triggerScrim` 全局遮罩层绑定，会导致用户仅点击人机复选框时整个卡片上方出现全局线性进度条及全屏灰色半透明遮罩，违背了轻量级复选框验证的视觉交互。因此，应使用纯组件内部的时钟控制函数 `withMinimumDelay` 维持 1500ms 的加载状态，不侵入外部遮罩体系。
2. **`handleCardActionClick` 与局部异步操作的竞态穿透**：
   在 `AuthLayout.vue` 中，当带有 `:isScrim="true"` 的按钮被点击时，若其类名未包含在排除列表中（如 `.btn-primary-action` 与 `.btn-secondary-action`），`handleCardActionClick` 会并发执行无动作的 `triggerScrim(undefined, 1000)`，将全局 `loading.value` 置为 `true`。此时，按钮自身绑定的异步方法执行 `await signinFlowStore.triggerScrim(undefined, 1000)` 时，命中 `if (loading.value) return undefined` 立即返回，使得后续的 `router.push` 几乎在 0ms 瞬间触发，造成“延迟未结束就已切页”的时序异常。

---

## 3. 具体改动实施

### 3.1 `recaptcha.vue` 采用独立 1500ms 最小加载时钟（无全局 Scrim 遮罩）
在 `src/volur-lite-view-NEXT/src/components/security/recaptcha.vue` 中：
- 剥离对全局 `triggerScrim` 的复用与遮罩注入；
- 实现纯净的局部时钟控制函数 `withMinimumDelay(action, 1500)`；
- 当 iframe 沙箱发送 `VM_CHALLENGE_SUBMIT` / `VM_CHALLENGE_SUCCESS` 时，在 `withMinimumDelay` 内执行人机挑战获取与验证，仅保持复选框自身的 `.rc-spinner` 旋转加载态至少 1500ms；
- 只有在 1500ms 满期后，才向沙箱发送 `VM_CONFIRM_PASS` 动画指令并派发 `@success` 事件，既实现了原生质感的旋转等待，又保持了外部界面的洁净无遮罩。

### 3.2 `AuthLayout.vue` 按钮排除名单补全
在 `src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue` 的 `handleCardActionClick` 过滤器中，将 `.btn-primary-action` 与 `.btn-secondary-action` 纳入排除类名列表，杜绝卡片代理点击与各 View 视图内显式管控的异步时钟发生冲突。

### 3.3 `recaptchaChallengeView.vue` 时序修复与光标对齐
在 `src/volur-lite-view-NEXT/src/views/auth/recovery/challenge/recaptchaChallengeView.vue` 中：
- `handleVerifyChallenge` 改用 `signinFlowStore.withLoading(action, 1000)` 显式包裹人机挑战提交 API；
- 确保延迟结束并退出 loading 状态后，再执行 `router.push` 进行路由切换；
- 为 `.auth-action-btn` 与 `.btn-primary-action` 明确补齐 `cursor: pointer` 及禁用态光标样式。

### 3.4 `EmailView.vue` 提交流程优化、移除 Toast 及样式修复
在 `src/volur-lite-view-NEXT/src/views/auth/recovery/username/EmailView.vue` 中：
- `handleNext` 使用 `signinFlowStore.withLoading(action, 1000)` 包裹 `submitRecoveryEmailApi`，确保满 1000ms 后才切换为 `isSubmitted = true` 成功卡片；
- 彻底移除 `window.$message?.success?.(...)` 冗余弹窗，完全依赖卡片内的自解释提示文案；
- 为 `.auth-action-btn` 与 `.btn-primary-action` 补齐 `cursor: pointer`、flex 居中与禁用态规范。

### 3.5 `RecoveryServiceImpl.java` 邮箱匹配日志与防枚举对齐
在 `src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/RecoveryServiceImpl.java` 中：
- 对传入邮箱补充 `.trim().toLowerCase()` 规范化清洗；
- 引入 `@Slf4j` 详细记录后端审计日志：
  - 命中已注册绑定用户时输出 `INFO [Recovery] 邮箱匹配成功，准备投递找回用户名邮件: email=..., username=...`；
  - 未匹配到任何用户时输出 `WARN [Recovery] 邮箱未匹配到任何已注册用户: email=...`，清晰溯源原因同时保持对外 200 响应防枚举安全标准。

---

## 4. 验证与质量保证

1. **类型安全性检查**：
   执行 `npx vue-tsc --noEmit`，TypeScript 0 错误通过。
2. **构建与打包测试**：
   执行 `npm run build`，Vite 生产构建编译成功（耗时 2.86s）。
3. **后端单元测试**：
   执行 `mvn test -pl volur-auth -Dtest=RecoveryServiceImplTest`，6 个测试用例全部通过。
4. **架构规范遵守**：
   严格恪守全代码零注释铁律（Zero Comments Rule），所有变更均无行内注释，技术方案统一收敛至本说明文件。
