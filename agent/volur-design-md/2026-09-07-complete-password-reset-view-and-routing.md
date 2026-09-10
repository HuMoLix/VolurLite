# 施工与审计记录：密码重置仅保留 Token 模式与单一 `/reset-password` 路由

## 1. 基本信息

- **任务名称**：密码重置页面全面收敛（仅保留 Token 模式，仅保留单一 `path: '/reset-password'` 路由），彻底废弃 `/reset` 与 `/auth/reset`
- **执行时间**：2026-09-07
- **影响文件**：
  - `volur-lite-view`：
    - [`src/volur-lite-view/src/router/routes.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/router/routes.ts)
    - [`src/volur-lite-view/src/router/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/router/index.ts)
    - [`src/volur-lite-view/src/views/auth/ResetView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/auth/ResetView.vue)
    - [`src/volur-lite-view/src/views/auth/AuthView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/auth/AuthView.vue)
    - [`src/volur-lite-view/src/locales/zh_CN/auth.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/auth.json)
    - [`src/volur-lite-view/src/locales/en_US/auth.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/auth.json)

---

## 2. 变更背景与需求说明

根据平台安全设计与最新产品交互规范：
1. **单一路径收敛**：
   - 密码重置业务只保留唯一的标准路由路径：`path: '/reset-password'`；
   - 彻底删除历史遗留或过渡性的 `/reset`、`/auth/reset` 及相关重定向规则，与邮件链接 `http://localhost:8080/reset-password?token=xxxx` 形成 1:1 绝对映射；
2. **仅限链接令牌模式**：
   - 不支持无链接直接访问重置页面；
   - 不支持手动输入/粘贴 Token；
   - 不支持在重置页使用邮箱验证码重置，仅允许通过邮件中生成的带有时效性 Token 的专属链接重置密码；
3. **无效/空 Token 严格拦截**：
   - 当用户直接访问 `/reset-password`、`/reset-password?token=` 或 `token` 参数为空/空白字符串时，拦截进入密码修改表单，直接返回并提示“无效的 token”，页面呈现 `n-result` 错误状态卡片，并引导用户重新申请找回密码或返回登录。

---

## 3. 具体修改方案

### 3.1 路由层级极简化 ([`routes.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/router/routes.ts) & [`index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/router/index.ts))
1. 在 `AuthLayout` 布局下，配置唯一的根级直通子路由：
   ```typescript
   {
     path: '/reset-password',
     name: 'ResetPassword',
     component: () => import('@/views/auth/ResetView.vue'),
     meta: { title: '重置密码', titleKey: 'auth.reset_password_title' }
   }
   ```
2. 彻底删除：
   - `path: 'reset'`（即 `/auth/reset`）；
   - `path: '/reset'` 重定向；
   - `path: '/reset-password'` 重定向；
3. 在 `router/index.ts` 的维护模式白名单中补充放行 `/reset-password`，确保重置流程不受拦截。

### 3.2 密码重置组件重构与收敛 ([`ResetView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/auth/ResetView.vue))
1. **废弃多余通道与手动输入**：
   - 彻底移除 `NTabs`、`NTabPane` 分段选择器；
   - 彻底移除邮箱验证码重置表单（`codeForm`、`codeRules`、`resetPasswordConfirmApi` 等）；
   - 彻底移除手动输入 Token 的表单项，重置所需的 `token` 仅通过只读响应式计算属性 `token = computed(() => (route.query.token as string || '').trim())` 从 URL 中提取。
2. **空令牌 / 无效访问拦截机制**：
   - 计算属性 `hasValidToken = computed(() => token.value.length > 0)`；
   - 若 `hasValidToken` 为 `false`（例如访问 `/reset-password?token=` 或未带 token 参数）：
     - `onMounted` 钩子即刻触发 `message.error(t('auth.invalid_token'))` 弹出错误 Toast（“无效的 token”）；
     - 视图渲染 `n-result`（`status="error"`），展示“重置链接无效 / 无效的 token”错误卡片，提供“重新申请找回密码”（跳转至 `/auth/forgot`）主按钮与“返回登录”导航；
3. **有效 Token 提交新密码**：
   - 若 `hasValidToken` 为 `true`，仅展示“新密码”与“确认新密码”两个输入框；
   - 严格执行 Jakarta Validation 规范校验（正则包含字母、数字与特殊字符，长度 6-32 位）；
   - 提交调用 `resetPasswordByTokenApi({ token: token.value, newPassword: form.password })`。

### 3.3 模式识别与路由直达优化 ([`AuthView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/auth/AuthView.vue))
- 优化 `currentMode` 计算属性，支持通过路由名称与路径直接激活对应模式（如访问 `/auth/forgot` 或命名路由 `ForgotPassword` 时直接激活 `forgot` 模式）。

### 3.4 国际化词条调整 ([`zh_CN/auth.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/auth.json) & [`en_US/auth.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/auth.json))
- 修正 `invalid_token` 为精准文案“无效的 token”（英文为 "Invalid token"）；
- 增加 `invalid_token_title`、`invalid_token_subtitle`、`invalid_token_description` 与 `reapply_reset`；
- 移除不再使用的 Tab 及手动 Token 输入相关冗余键名。

---

## 4. 验证与审计结论

- **前端工程构建**：
  - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程 TypeScript 强类型校验与 Vite 打包通过，0 错误 0 警告。
- **场景测试矩阵**：
  1. 访问 `/reset-password?token=`：触发 `message.error("无效的 token")` 提示，展示错误结果卡片与重新申请按钮，表单被拦截不显示；
  2. 访问 `/reset-password`：判定为无有效 token，展示拦截错误卡片并弹出提示；
  3. 访问 `/reset-password?token=valid_token`：正常展示重置新密码表单，输入合法新密码后调用 `POST /api/v1/auth/password-resets` 完成重置并安全重定向；
  4. 访问 `/reset` / `/auth/reset`：均已废除，走 404 兜底处理。
