# 基于后端下发 nextStep 与 status 的动态拼接挑战状态机重构

## 1. 变更背景与架构演进

在原有的登录挑战流（`signinFlow.ts`）中，前端在提交用户挑战核验时，采用了传统的静态 `if-else if` 条件分支，针对 `pwd`、`etotp`、`totp`、`recaptcha`、`device` 等每一个挑战步骤分别硬编码调用不同的 API 函数。

这种硬编码模式存在明显的维护隐患与扩展瓶颈：
- 每次后端新增或演进一种安全挑战方式，前端 Store 必须同步修改并追加静态分支；
- 挑战类型名称或别名（如 `device` 与 `pk`、`sms` 与 `totp`）一旦前端遗漏，就会直接抛出“不支持的安全核验挑战类型”阻断用户流程；
- 破坏开闭原则（Open-Closed Principle）。

根据前后端契约规范，后端的挑战控制器统一位于 `/api/v1/captcha/challenge/*` 下，其路径与后端响应中的 `nextStep` 以及 `status` 具备天然的同构映射关系。因此，前端与后端全面重构为**基于后端下发 `nextStep` 和 `status` 的纯动态拼接与自适应流转架构**。

---

## 2. 核心设计与改动点

### 2.1 前端 API 层：提取统一动态挑战网关
在 `src/volur-lite-view-NEXT/src/api/auth.ts` 中新增通用的动态挑战提交 API：
```ts
export function submitDynamicChallengeApi(
  step: string,
  data: Record<string, any>
): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>(`/captcha/challenge/${step}`, data, { skipToast: true })
}
```

### 2.2 前端状态机：`signinFlow.ts` 彻底消除硬编码分支
在 `src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts` 中重构 `submitChallenge`：
1. **端点动态拼接**：
   直接提取当前活跃挑战目标步骤 `targetStep = (challengeType || activeChallengeType.value).trim().toLowerCase()`，动态请求 `/captcha/challenge/${targetStep}`；
2. **载荷自适应装配**：
   统一装配通用挑战载荷（`dsh`, `tl`, `cid`, 加密后的 `password`, `code`, `recaptchaTicket`, `credential`, `rememberMe` 等），由 Jackson 在服务端按需反序列化映射；
3. **基于 `status` 动态判定**：
   - 若 `data.status === 'SUCCESS'`：直接判定为认证成功，重定向至目标页面；
4. **基于 `nextStep` 动态流转**：
   - 若 `data.nextStep === 'authenticate'`：动态流向认证端点 `POST /auth/signin/${data.nextStep}`，完成 JWT 令牌颁发与建会话收尾；
   - 若 `data.nextStep !== 'authenticate'`：动态更新 `tl`, `cid`, `activeChallengeType`，并返回给路由跳转到 `/v1/signin/challenge/${data.nextStep}`。

### 2.3 后端控制器：多别名同构路径绑定
为了确保前端根据各种标准别名动态拼接的端点能 100% 命中，在后端控制器的 `@RequestMapping` 中统一配置了别名数组：
- `PasswordController`：`@RequestMapping({"/api/v1/captcha/challenge/pwd", "/api/v1/captcha/challenge/password"})`
- `EmailOTPChallengeController`：`@RequestMapping({"/api/v1/captcha/challenge/etotp", "/api/v1/captcha/challenge/email"})`
- `SMSOTPChallengeController`：`@RequestMapping({"/api/v1/captcha/challenge/totp", "/api/v1/captcha/challenge/sms", "/api/v1/captcha/challenge/smsotp", "/api/v1/captcha/challenge/2fa"})`
- `ReCAPTCHAChallengeController`：`@RequestMapping({"/api/v1/captcha/challenge/recaptcha", "/api/v1/captcha/challenge/captcha"})`
- `PasskeyController`：`@RequestMapping({"/api/v1/captcha/challenge/passkey", "/api/v1/captcha/challenge/pk", "/api/v1/captcha/challenge/device"})`，且断言核验支持 `@PostMapping(value = {"", "/verify"})`，注册支持 `@PostMapping(params = {"dsh", "tl"})`，精准区分。

---

## 3. 安全防护与无感兼容说明

1. **防重放与跨步跃迁校验**：
   无论前端如何动态拼接路径，后端的 `TransitionLockManager.validateTransLock` 均严格强制校验当前活跃 TL、dsh、cid 与后端当前步骤，保证了动态拼接不会绕过安全状态机检查；
2. **密码信封加密保持**：
   当请求载荷包含密码明文时，仍会在客户端自动执行 `encryptFlowCredential` 动态信封加密，绝不泄露明文凭据；
3. **零硬编码扩展性**：
   未来后端若新增挑战（如 `backup_code`、`push` 等），前端 Store 无需修改任何代码即可实现动态路由、拼接与状态推进。

---

## 4. 全工程验证结果

- **后端测试**：
  - 13 个子模块 `mvn test` 100% 通过（258+ 测试用例全部绿灯，0 失败，0 错误，耗时 24.8s）；
- **前端构建**：
  - `src/volur-lite-view-NEXT` 执行 `npm run build` 成功（0 错误，耗时 2.63s）；
  - `src/volur-lite-view` 执行 `npm run build` 成功（0 错误，耗时 6.04s）。
