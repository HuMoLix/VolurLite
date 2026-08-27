# 修复登录及表单错误提示重复弹窗施工记录

**施工日期**：2026-08-26  
**施工模块**：`src/volur-lite-view/src/components/auth/AuthForm.vue`  
**责任主体**：Antigravity AI Agent

---

## 1. 问题背景与现象分析

用户在登录页面输入错误账号密码点击「继续」时，界面上同时弹出两次「账号或密码错误」的 Toast 提示。

### 根本原因排查：
1. **Axios 全局响应拦截器 (`request.ts`)**：
   - 收到后端 HTTP 400/401 或业务 `success: false` 响应时，全局拦截器自动调用了 `window.$message.error(message)` 进行了**第 1 次错误弹窗**，随后抛出 `Promise.reject(err)`；
2. **页面组件 catch 块 (`AuthForm.vue`)**：
   - `handleAction`、`sendEmailVerificationCode` 及 `handleForgotPasswordAction` 在 `try-catch` 的 `catch` 块中再次手动调用了 `message.error(...)` 进行了**第 2 次重复弹窗**。

---

## 2. 修复方案与实施细节

遵循 **Thin View 架构设计原则**：
- 错误提示统一由 Axios 全局响应拦截器（`request.ts`）集中接管并自动 Toast 提醒；
- 页面表单组件（[`AuthForm.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue)）在 `catch` 块中移除了冗余的 `message.error()` 调用，仅保留局部状态清理（如刷新验证码、清空验证码输入框、恢复 loading 状态等）。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行编译构建：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：生产环境打包成功（耗时 ~4.00s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/components/auth/AuthForm.vue`
- **修复效果**：
  - 彻底杜绝了登录、注册、验证码发送和找回密码等场景下的 Double Toast 缺陷，交互体验干净利落。
