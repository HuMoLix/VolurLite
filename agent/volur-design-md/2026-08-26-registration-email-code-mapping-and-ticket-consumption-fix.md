# 注册链路邮箱验证码入参对齐与人机核验凭证单次消费解耦修复

## 1. 现象与根因分析

### 1.1 异常现象
用户在前端完成了滑块拼图验证、成功接收并填入邮箱验证码后，点击【立即注册】按钮时，后端拦截并抛出：
`400 BAD_REQUEST, errorCode=400, message=人机验证凭证已失效，请重新验证`

### 1.2 根因排查
1. **凭证的单次消费特性（One-Time Token）**：
   - 用户在点击【发送验证码】时，完成了滑块拼图并获得 `CaptchaTicket`；
   - 在触发 `sendVerificationEmail` 时，后端已将该 `CaptchaTicket` 进行原子核销（从 Redis 中删除）；
2. **前后端实体字段命名偏差 (`emailCode` vs `emailCaptcha`)**：
   - 前端发送的注册 JSON 为 `{ username, password, email, emailCode }`；
   - 后端 [`RegisterRequestDTO`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/RegisterRequestDTO.java) 字段名为 `emailCaptcha`，导致 Spring MVC 反序列化时 `request.getEmailCaptcha()` 为 `null`；
3. **注册阶段校验逻辑误走回退人机核验**：
   - 在 [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java) 中，由于 `REQUIRE_EMAIL_VERIFICATION` 默认可能为 `false`，且 `emailCaptcha` 因命名未匹配而为 `null`，逻辑误流转到 `else` 分支中的 `checkCaptchaIfEnabled`；
   - `checkCaptchaIfEnabled` 再次尝试从 Redis 中校验已被发信步骤消费删除的 `captchaTicket`，从而抛出“人机验证凭证已失效”。

---

## 2. 修复措施实施

### 2.1 请求实体别名注解兼容
在 [`RegisterRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/RegisterRequestDTO.java) 中为 `emailCaptcha` 添加 `@JsonAlias({"emailCaptcha", "emailCode"})`，确保无论是 `emailCode` 还是 `emailCaptcha` 均能 100% 精确映射。

### 2.2 注册验证流程智能适配
在 [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java) 中：
```java
boolean requireEmailVerification = systemSettingService.getBoolean("REQUIRE_EMAIL_VERIFICATION", false);
String emailCaptcha = request.getEmailCaptcha();

if (requireEmailVerification || StringUtils.hasText(emailCaptcha)) {
    if (!emailService.verifyCode(normalizedEmail, emailCaptcha)) {
        throw new InvalidCaptchaException("邮箱验证码错误或已过期");
    }
} else {
    checkCaptchaIfEnabled(request.getCaptchaId(), request.getCaptchaCode(), request.getCaptchaTicket(), CaptchaScene.REGISTER);
}
```
- 当用户填写并提交了邮箱验证码（或系统开启强制邮箱验证）时，注册阶段核验邮箱验证码；
- 发信阶段已通过滑块人机凭证，注册阶段不再二次要求核验已销毁的 `CaptchaTicket`。

---

## 3. 验证结论
- **全量测试**：`mvn test` 全平台 11 个模块 100% 编译并通过单元测试；
- **前端构建**：`npm run build` 100% 编译通过；
- **全链路闭环**：滑块拼图 $\to$ 消费凭证发信 $\to$ 输入邮箱验证码 $\to$ 成功创建账号。
