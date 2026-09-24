# 彻底废除历史扁平验证码字段与全面统一 CaptchaVerifyRequestDTO 载荷架构施工记录

**日期**：2026-09-22  
**作者**：Antigravity Agent  
**模块**：`volur-auth` / `volur-verification` / `volur-lite-view`  
**状态**：已完成 (Completed) & 全量验证通过  

---

## 1. 变更背景与核心动因

在完成 `CaptchaSDK` 的标准化多模态人机安全验证架构落地后，原系统在部分 DTO 及前端调用中仍保留了兼容旧版的前端扁平/平铺字段：
- 例如在 [`AuthenticateRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/AuthenticateRequestDTO.java) 中既声明了 `captcha` 嵌套对象，又残留了平铺属性 `captchaId`, `captchaCode`, `captchaTicket`, `otpCode` 以及 `resolveCaptchaPayload()` 降级解析方法；
- 在 [`RegisterRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/RegisterRequestDTO.java)、[`ForgotPasswordRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/ForgotPasswordRequestDTO.java)、[`DeleteAccountRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/DeleteAccountRequestDTO.java) 及 [`EmailCaptchaRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/dto/EmailCaptchaRequestDTO.java) 中亦分散存在平铺验证码字段；
- 前端视图层与 Store 中在提交表单时存在属性冗余展开或同时传递嵌套与平铺字段的双写反模式。

此类双写与扁平兼容不仅违背了 Volur Lite 的薄领域模型规范，且导致上层模块职责发散、易滋生逻辑分支遗漏或校验绕过隐患。根据架构决策，全系统必须彻底清理历史平铺技术债，全链路统一为标准的组合对象 `@Valid private CaptchaVerifyRequestDTO captcha`。

---

## 2. 改动点深度剖析

### 2.1 后端领域模型 (DTO) 彻底重构
移除所有平铺历史字段（`captchaId`, `captchaCode`, `captchaTicket`, `otpCode`）以及 `resolveCaptchaPayload()` 冗余逻辑，全面使用 `@Valid private CaptchaVerifyRequestDTO captcha;`：
1. **[`AuthenticateRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/AuthenticateRequestDTO.java)**：
   - 移除 `captchaId`, `captchaCode`, `captchaTicket`, `otpCode` 及 `resolveCaptchaPayload()`；
   - 仅保留核心字段：`username`, `password`, `rememberMe`, `@Valid private CaptchaVerifyRequestDTO captcha`。
2. **[`RegisterRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/RegisterRequestDTO.java)**：
   - 移除平铺字段，引入 `@Valid private CaptchaVerifyRequestDTO captcha`。
3. **[`ForgotPasswordRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/ForgotPasswordRequestDTO.java)**：
   - 移除平铺字段，引入 `@Valid private CaptchaVerifyRequestDTO captcha`。
4. **[`DeleteAccountRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/DeleteAccountRequestDTO.java)**：
   - 移除平铺字段，引入 `@Valid private CaptchaVerifyRequestDTO captcha`。
5. **[`EmailCaptchaRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/dto/EmailCaptchaRequestDTO.java)**：
   - 移除平铺字段，引入 `@Valid private CaptchaVerifyRequestDTO captcha`。

### 2.2 服务层架构解耦 (Thin Service & CaptchaSDK)
1. **[`AuthServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)**：
   - **解耦底层 CaptchaService**：彻底移除对底层 `CaptchaService` 的依赖注入，上层业务模块纯粹依赖 `CaptchaSDK` 高阶门面；
   - **删除样板校验私有方法**：删除 `checkCaptchaIfEnabled` 私有方法；
   - **统一单行核验**：
     - `authenticate`：`captchaSDK.verify(request.getCaptcha(), rawAccount, CaptchaScene.LOGIN);`
     - `register`：`captchaSDK.verify(request.getCaptcha(), normalizedUsername, CaptchaScene.REGISTER);`
     - `processForgotPassword`：`captchaSDK.verify(request.getCaptcha(), request.getUsername(), CaptchaScene.RESET_PASSWORD);`
     - `sendDeleteAccountEmail`：`mailRequest.setCaptcha(request.getCaptcha());`。
2. **[`EmailServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java)**：
   - `checkCaptchaIfEnabled` 重构为基于 `request.getCaptcha()` 进行校验，保持契约一致性。

### 2.3 前端类型系统与业务视图重构
1. **[`src/types/auth/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/auth/index.ts) & [`src/types/verification/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/verification/index.ts)**：
   - 严格 1:1 镜像后端 DTO，移除所有平铺字段，统一暴露 `captcha?: CaptchaVerifyRequestDTO`。
2. **[`AuthForm.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue)**：
   - 清除组件内残存的低阶状态 `captchaImage`, `captchaId`, `captchaType`, `requireBehaviorVerify` 及旧的 `fetchCaptcha()` 函数；
   - 登录流程由 `<VolurCaptcha ref="loginCaptchaRef" />` 统一自驱动，提交时 `loginCaptchaRef.value.getPayload()` 提取标准 `CaptchaVerifyRequestDTO`；
   - 注册、重置密码发信、发送注册邮箱验证码全面采用 `{ captcha: { captchaTicket: ..., scene: ..., account: ... } }`；
   - 彻底删除传给 `userStore.login` 的冗余平铺字段。
3. **[`ProfileView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/profile/ProfileView.vue)**：
   - 改密发信 `sendChangePasswordEmailCodeApi` 与销户发信 `sendDeleteAccountEmailCodeApi` 统一组装 `captcha: { captchaTicket: ticket, scene: 'CHANGE_PASSWORD', account: ... }`。

---

## 3. 安全收益与架构价值

1. **单一真实数据源 (Single Source of Truth)**：
   - 消除表单与接口双写风险，无论是字符图形、行为滑块还是动态 OTP 凭据，全部收敛在 `captcha` 实体中，后端使用统一的 JSR-380 / Jakarta 校验注解 (`@Valid`) 触发深层字段验证。
2. **高阶业务模块零心智负担**：
   - 业务模块（如 `volur-auth`、`volur-competition`）仅需关注业务本身，不再需要了解底层是调用的线段干扰码、扭曲验证码、滑动拼图还是 Redis 缓存，一行 `captchaSDK.verify(...)` 自动完成防刷白名单、自适应风控阶梯决策及异常阻断。
3. **彻底杜绝技术债务累积**：
   - 消除向后兼容冗余字段带来的代码腐化，前后端均保持极简规范。

---

## 4. 验证与测试结论

1. **后端单元测试**：
   - [`AuthVerificationCrossValidationTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/AuthVerificationCrossValidationTest.java)：更新单测断言以适配 `CaptchaSDK.verify` 与 `CaptchaVerifyRequestDTO`，7 个测试用例全部通过；
   - [`EmailServiceVerificationTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/EmailServiceVerificationTest.java)：更新测试入参为组合 `CaptchaVerifyRequestDTO`，所有发信与人机门禁用例全部通过；
   - 执行 `mvn test -pl :volur-verification,:volur-auth`：**38 个测试全部 SUCCESS，0 Failures，0 Errors**。
2. **前端工程构建**：
   - 执行 `npm run build`：**✓ built in 6.40s，0 Errors，0 Warnings**。
