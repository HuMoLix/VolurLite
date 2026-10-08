# 施工记录：移除登录成功后跳转首页冗余 Toast 弹窗

## 1. 变更背景与需求定位

在 Volur Lite 新一代前端工程（`volur-lite-view-NEXT`）中，用户在完成登录挑战（如动态邮箱验证码 `/v1/signin/challenge/etotp`、短信验证码 `/v1/signin/challenge/smsotp` 或密码核验 `/v1/signin/challenge/pwd`）后，会被重定向至赛事大厅/首页（[`HomeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/home/HomeView.vue)）。
在原实现中，挑战视图在执行路由推进前调用了全局消息提示：
```ts
window.$message?.success?.(t('auth.security_verify_success')) // “安全核验通过，登录成功！”
```
由于 Naive UI 全局消息独立挂载在视口顶层且具备 3000ms 默认展示周期，导致跳转进入 [`HomeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/home/HomeView.vue) 后，页面顶端依然悬浮着该 Toast 弹窗，造成 UI 信息冗余并违背了 Google Workspace / Material Design 极简无打扰设计规范。

---

## 2. 改动实施

1. **[`emailotpChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/emailotpChallengeView.vue)**：
   移除登录核验成功时的 `window.$message?.success?.(t('auth.security_verify_success'))` 弹窗调用，依托 Scrim 延迟遮罩平滑流转，登录完成后直接推进目标路由；
2. **[`smsotpChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/smsotpChallengeView.vue)**：
   同步移除短信核验成功分支中的冗余 `window.$message?.success` 弹窗；
3. **[`pwdChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/pwdChallengeView.vue)**：
   同步移除密码验证成功分支中的 `window.$message?.success?.(t('auth.login_success'))` 弹窗，全链路实现无扰平滑登录。

---

## 3. 验证与质量保证

1. **构建与类型检查**：
   运行 `npm run build`（`vue-tsc --noEmit && vite build`），TypeScript 0 错误通过，打包顺利完成（耗时 6.54s）。
2. **规范遵守**：
   全量代码严格恪守零行内注释铁律（Zero Comments Rule），改动保持紧凑自解释。
