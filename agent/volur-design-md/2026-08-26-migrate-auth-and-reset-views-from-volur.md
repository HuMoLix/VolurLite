# Volur 登录认证与密码重置全流程迁移施工记录

**施工日期**：2026-08-26  
**施工模块**：`src/volur-lite-view/src/views/auth/AuthView.vue`、`src/volur-lite-view/src/views/auth/ResetView.vue`、`src/volur-lite-view/src/components/auth/AuthForm.vue`、`src/volur-lite-view/src/router/routes.ts`、`src/volur-lite-view/src/api/auth/index.ts`、`src/volur-lite-view/src/types/auth/index.ts`  
**参考来源**：`H:\Dev\src\VolurView\src\features\auth\views\AuthView.vue`、`H:\Dev\src\VolurView\src\features\auth\views\ResetView.vue`  
**责任主体**：Antigravity AI Agent

---

## 1. 迁移背景与需求目标

根据 `VolurView` 原型实现并结合 Volur Lite 前后端规范体系，完成认证中心的全闭环功能迁移与落地：
1. **统一认证入口 (`AuthView.vue` & `AuthForm.vue`)**：
   - 支持登录（`login`）、注册（`register`）、找回密码申请（`forgot`）三合一模态切换；
   - 支持 URL Query 参数控制（如 `/auth/login?mode=register` 自动预选模式）；
   - 登录成功后自动回跳至来源路径（`route.query.redirect || '/'`）。
2. **密码重置闭环 (`ResetView.vue`)**：
   - 从邮件重置链接中提取 `token` 查询参数（`route.query.token`）；
   - 提供新密码（`password`）与确认新密码（`confirmPassword`）双重强度校验；
   - 调用后端 `POST /api/v1/auth/password-resets` 完成密码重置，并附带平滑倒计时回跳登录页。
3. **前后端接口与 DTO 100% 严格对齐**：
   - `ForgotPasswordRequestDTO` (`email`, `username`)
   - `ForgotPasswordResetRequestDTO` (`token`, `newPassword`)
   - `ResetPasswordRequestDTO` (`email`, `code`, `password`)

---

## 2. 核心改动点与实施细节

### 2.1 API 与类型层定义
- **[`types/auth/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/types/auth/index.ts)**：补齐 `ForgotPasswordRequestDTO` 与 `ForgotPasswordResetRequestDTO` 结构；
- **[`api/auth/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/api/auth/index.ts)**：
  - `forgotPasswordApi` -> `POST /api/v1/auth/forgot-password`
  - `resetPasswordByTokenApi` -> `POST /api/v1/auth/password-resets`
  - `resetPasswordConfirmApi` -> `POST /api/v1/auth/reset-password-confirm`

### 2.2 视图层组件迁移
- **[`AuthView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/auth/AuthView.vue)**：作为认证中心主入口，承载 `AuthForm`，提供路由 Query 同步与成功重定向；
- **[`ResetView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/auth/ResetView.vue)**：实现独立的设置新密码页面，包含两行等大（`24px` / `font-weight: 400`）标题、一致性密码校验、Token 校验与完成提示。

### 2.3 路由配置升级 ([`router/routes.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/router/routes.ts))
- 增加 `/auth/login`、`/auth/register`、`/auth/forgot`、`/auth/reset` 路由并统一交由 `AuthLayout` 渲染。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量构建：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：生产环境打包成功（耗时 ~5.06s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/views/auth/AuthView.vue`
  - `src/volur-lite-view/src/views/auth/ResetView.vue`
  - `src/volur-lite-view/src/views/auth/LoginView.vue`
  - `src/volur-lite-view/src/components/auth/AuthForm.vue`
  - `src/volur-lite-view/src/router/routes.ts`
  - `src/volur-lite-view/src/api/auth/index.ts`
  - `src/volur-lite-view/src/types/auth/index.ts`
  - `src/volur-lite-view/src/locales/zh_CN/auth.json`
  - `src/volur-lite-view/src/locales/en_US/auth.json`
- **业务完整性**：
  - 完整实现了用户登录、注册、发起找回密码申请以及重置新密码的全链路闭环。
