# 找回重置密码发送重置链接滑块验证码弹窗联动修复

## 1. 现象与根因分析

### 1.1 异常现象
在找回密码页面输入用户名和邮箱后，点击【发送重置链接】按钮，系统直接弹出报错：
`400 BAD_REQUEST: 请先完成人机安全验证`，而页面上没有任何验证码弹出。

### 1.2 根因排查
1. **后端安全风控强校验**：
   在 [`AuthServiceImpl.java:268`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java#L268) 中，`processForgotPassword` 强制执行了：
   `checkCaptchaIfEnabled(..., CaptchaScene.RESET_PASSWORD)`。
   当系统开启人机验证时（`ENABLE_CAPTCHA = true`），若请求未携带有效 `captchaTicket`，后端抛出 `InvalidCaptchaException("请先完成人机安全验证")`；
2. **前端忘记密码提交未联动滑块组件**：
   在 [`AuthForm.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue) 中：
   - 忘记密码表单未像注册流程那样唤起 [`SlidePuzzleModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/verification/SlidePuzzleModal.vue)；
   - 直接向后端提交了空凭证的 `forgotPasswordApi({ email, username })`；
   - 且 `pendingAction` 和 `handlePuzzleSuccess` 未接入 `FORGOT_PASSWORD` 动作回调。

---

## 2. 修复措施与实施

### 2.1 [`AuthForm.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue) 联动滑块核验
1. **唤起滑块弹窗**：
   当用户点击【发送重置链接】时，若 `isCaptchaEnabled` 为 `true`，设置 `puzzleScene = 'RESET_PASSWORD'`、`pendingAction = 'FORGOT_PASSWORD'` 并将 `showPuzzleModal` 置为 `true`；
2. **凭证自动流转**：
   用户拖动滑块完成拼图后，回调获取 `CaptchaTicket`，自动携带 `captchaTicket` 调用 `doSendForgotPassword()` 触发发信；
3. **关闭与提示**：
   发信成功后提示“重置链接已发送至您的邮箱！”，平滑切换回登录模式。

---

## 3. 验证结论
- **前端构建**：`vue-tsc --noEmit && vite build` 编译 100% 成功。
- **全链路闭环**：输入账号邮箱 $\to$ 唤起滑块拼图 $\to$ 验证通过自动提交 $\to$ 邮件成功下发。
