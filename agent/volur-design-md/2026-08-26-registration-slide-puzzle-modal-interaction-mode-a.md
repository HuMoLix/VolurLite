# 注册滑块拼图弹窗人机核验交互（方案 A）与发信流程重构

## 1. 变更背景与需求
在用户注册流程中，根据平台风控策略与安全规范：
1. **统一采用行为级人机安全挑战**：注册场景（`CaptchaScene.REGISTER`）最低要求提升至 `SLIDE_PUZZLE`（滑块拼图验证码），彻底阻断脚本枚举与机器批量注册；
2. **重构前端交互（方案 A）**：移除注册表单内平铺的静态图形验证码输入框，用户在点击【发送验证码】时，无感唤起 [`SlidePuzzleModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/verification/SlidePuzzleModal.vue) 进行滑块拼图验证，验证成功后自动携带颁发的 `CaptchaTicket` 触发注册发信；
3. **后端注册与发信链路校验解耦**：当开启邮箱验证（`REQUIRE_EMAIL_VERIFICATION`）时，人机核验凭证在发信阶段被消费，注册提交阶段校验邮箱验证码；若未开启邮箱验证，则在提交注册表单时完成人机凭证核销。

---

## 2. 改动点汇总

### 2.1 前端工程 (`volur-lite-view`)
1. **[`src/components/auth/AuthForm.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue)**：
   - **UI 结构瘦身**：移除注册表单中的静态验证码输入项（`<n-input v-model:value="registerForm.captchaCode">` + `<img>`），保持表单清爽整洁；
   - **滑块弹窗联动**：
     - 点击【发送验证码】时，在完成基础邮箱格式检查后，若 `isCaptchaEnabled` 为 `true`，自动设置 `puzzleScene = 'REGISTER'`、`pendingAction = 'SEND_EMAIL_CODE'` 并激活 `showPuzzleModal = true`；
     - 滑块拖动完成并通过后端验证后，回调 [`handlePuzzleSuccess(ticketVO)`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue#L281)，自动携带获取到的 `ticketVO.ticket` 调用 `doSendEmailCode()` 发送邮件验证码并开启 60 秒倒计时；
   - **表单校验精简**：从 `registerRules` 中移除静态图形验证码规则，仅保留用户名、密码、邮箱及邮箱验证码的合规约束；
   - **登录/注册模式切换分离**：切换至登录模式时按需拉取登录验证码，切换至注册模式时不再拉取静态验证码。

### 2.2 后端工程 (`volur-auth` & `volur-verification`)
1. **[`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)**：
   - 重构 `register` 方法中的验证逻辑：在开启 `REQUIRE_EMAIL_VERIFICATION` 时校验邮箱验证码（由于发信时已完成滑块凭证校验），在未开启邮箱验证时核销 `CaptchaTicket`。
2. **[`EmailServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java)**：
   - `sendVerificationEmail` 优先校验 `checkCaptchaIfEnabled`，通过人机凭证后才执行邮箱查重与邮件投递。

---

## 3. 验证与审计结论
- **前端构建**：`vue-tsc --noEmit && vite build` 编译打包 100% 成功。
- **后端单测**：全平台 11 个模块执行 `mvn test` 100% 通过（包含 cross-validation 与 verification 单元测试）。
- **用户体验**：注册表单更聚焦轻量，滑块拼图拖拽顺畅，发信前置防刷全自动流转。
