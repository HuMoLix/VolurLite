# 验证码与发信模块接口隔离 (ISP) 解耦与邮件底层传输层下沉施工记录

**日期**：2026-09-23  
**作者**：Antigravity Agent  
**模块**：`volur-verification` / `volur-auth`  
**状态**：已完成 (Completed) & 全量测试通过  

---

## 1. 变更背景与动因深度溯源

在 Volur Lite 演进过程中，人机核验能力已全面标准化收敛为 [`CaptchaSDK`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/sdk/CaptchaSDK.java) 高阶门面。然而在审查发信模块实现时发现：
1. **违反接口隔离原则 (Interface Segregation Principle, ISP)**：
   原 [`EmailService`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/EmailService.java) 接口职责严重发散，既承载了高阶业务发信（`sendVerificationEmail`、`sendChangePasswordEmail` 等带人机校验的流程），又混杂了底层邮件投递原语（`sendEmail`、`testSmtpConnection`）以及 Redis 动态验证码管理（`verifyCode`）。
2. **产生潜在的 Spring Bean 循环依赖风险**：
   高阶门面 [`CaptchaSDKImpl`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/sdk/CaptchaSDKImpl.java) 在处理极高风险带外邮件安全码下发（`sendLoginOtp`）与核验（`verifyOutOfBandOtp`）时，直接注入了 `EmailService`。若业务发信服务 [`EmailServiceImpl`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java) 也直接注入 `CaptchaSDK`，就会导致 Spring 构造器级双向死锁（`CaptchaSDKImpl <--> EmailServiceImpl`）。
3. **残留私有校验样板代码**：
   受限于循环依赖，`EmailServiceImpl` 只能自行保留私有方法 `checkCaptchaIfEnabled`，手动调用底层的 `CaptchaService` 与 `RiskEvaluationEngine`，未能享受到 `CaptchaSDK` 提供的设备指纹、TLS 研判与多模态自适应阶梯防护。

为此，依据架构决策实施**方案一：接口隔离与能力下沉（ISP 解耦）**，彻底切断反向依赖，收敛安全核验逻辑。

---

## 2. 详细改动点剖析

### 2.1 底层发信传输网关抽象 ([`MailTransportService`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/service/MailTransportService.java))
从高阶发信业务中下沉纯粹的邮件参数解析、发信日志审计及传输调度逻辑：
1. **新建接口 [`MailTransportService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/service/MailTransportService.java)**：
   - `sendEmail(String to, String subject, String content)`
   - `sendEmail(String to, String subject, String content, String type)`
   - `testSmtpConnection()`
2. **新建实现类 [`MailTransportServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/service/impl/MailTransportServiceImpl.java)**：
   - 依赖注入 `SystemSettingService`, `FailoverMailDispatcher`, `EmailLogRepository`；
   - 独立封装系统发件人配置校验、`MailMessageBO` 构造、调度及发信日志持久化。

### 2.2 验证码生命周期管理组件抽象 ([`EmailVerificationCodeService`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/EmailVerificationCodeService.java))
将基于 Redis 的邮件验证码存取与原子核销职责解耦：
1. **新建接口 [`EmailVerificationCodeService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/EmailVerificationCodeService.java)**：
   - `generateAndStoreCode(String email)`
   - `storeCode(String email, String code, Duration duration)`
   - `verifyCode(String email, String code)`
   - `getStoredCode(String email)`
2. **新建实现类 [`EmailVerificationCodeServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailVerificationCodeServiceImpl.java)**：
   - 收敛 `vredis:verification:email_code:` 键名与 5 分钟默认 TTL；
   - 封装随机 6 位数字码生成与“核验成功即销毁”的原子消费机制。

### 2.3 `CaptchaSDKImpl` 彻底切断对 `EmailService` 的依赖
- 移除：`private final EmailService emailService;`
- 引入：
  - `private final MailTransportService mailTransportService;`
  - `private final EmailVerificationCodeService emailVerificationCodeService;`
- 重构 `sendLoginOtp`：使用 `emailVerificationCodeService.storeCode` + `mailTransportService.sendEmail`；
- 重构 `verifyOutOfBandOtp`：使用 `emailVerificationCodeService.verifyCode`。
- **依赖拓扑纯化**：`CaptchaSDKImpl` 仅向下依赖底层能力组件，与高阶 `EmailService` 零物理耦合。

### 2.4 `EmailServiceImpl` 全面接入 `CaptchaSDK`
- 移除：
  - `private final CaptchaService captchaService;`
  - `private final FailoverMailDispatcher mailDispatcher;`
  - `private final EmailLogRepository emailLogRepository;`
  - `private final RiskEvaluationEngine riskEngine;`
  - 彻底删除私有样板方法 `checkCaptchaIfEnabled(...)` 与 `generateVerificationCode(...)`。
- 引入：
  - `private final CaptchaSDK captchaSDK;`
  - `private final MailTransportService mailTransportService;`
  - `private final EmailVerificationCodeService emailVerificationCodeService;`
- 发信安全门禁全面收敛为单行 SDK 调用：
  - `sendVerificationEmail`：`captchaSDK.verify(request.getCaptcha(), request.getEmail(), CaptchaScene.REGISTER);`
  - `sendChangePasswordEmail`：`captchaSDK.verify(request.getCaptcha(), request.getEmail(), CaptchaScene.CHANGE_PASSWORD);`
  - `sendDeleteAccountEmail`：`captchaSDK.verify(request.getCaptcha(), request.getEmail(), CaptchaScene.CHANGE_PASSWORD);`
- 底层发信与验证码核验委派：
  - `sendEmail` 委派给 `mailTransportService.sendEmail`；
  - `testSmtpConnection` 委派给 `mailTransportService.testSmtpConnection`；
  - `verifyCode` 委派给 `emailVerificationCodeService.verifyCode`。

### 2.5 统一前置增强型 `@Email` 校验并清理 Service 层冗余验证与黑名单
1. **[`EmailCaptchaRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/dto/EmailCaptchaRequestDTO.java)**：
   - 切换导入为平台增强型 `@com.humolix.volur.framework.common.validation.annotation.Email`；
   - 在 HTTP 控制器请求入参阶段即自动触发基于 `SystemEmailWhitelistProvider` 的系统邮箱域名白名单校验。
2. **彻底清理 [`EmailServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java) 冗余代码**：
   - 移除 `DISPOSABLE_EMAIL_DOMAINS` 硬编码黑名单集合；
   - 移除 `validateEmailDomain` 私有方法；
   - 移除 `sendVerificationEmail` 中重复手写的白名单后缀读取与 stream 遍历逻辑；
   - 严格遵循**前置声明式白名单拦截（Declarative Whitelist Validation）**原则，Service 层仅专注纯粹的业务邮件投递与限频。

---

## 3. 架构收益与安全加固

1. **彻底粉碎循环依赖隐患 (Zero Circular Dependency Risk)**：
   解耦后的依赖拓扑为清晰的自顶向下单向有向无环图（DAG）：
   `EmailServiceImpl` $\rightarrow$ `CaptchaSDK` $\rightarrow$ `MailTransportService` & `EmailVerificationCodeService`。
2. **多模态自适应风控全链路覆盖**：
   发信接口（如 `/api/v1/email/captcha/register`）不再运行简陋的独立私有校验，而是全面享受 `CaptchaSDK` 提供的「IP + 账号 + 设备指纹 + TLS 协议一致性」综合风控研判、动态阶梯跃迁与自动封禁防线。
3. **接口职责高度聚焦与单一数据源**：
   邮件传输、验证码存取、业务发信三大职责被清晰隔离，且对外暴露的 `EmailService` 与 Controller 契约完全向后兼容，外部调用方（如 `AuthServiceImpl`）完全无感知。

---

## 4. 验证与测试结论

1. **后端单元测试**：
   - 适配并更新 [`EmailServiceVerificationTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/EmailServiceVerificationTest.java)；
   - 修正 [`RegisterRequestDTOValidationTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/RegisterRequestDTOValidationTest.java) 断言匹配规则；
   - 执行 `mvn test -pl :volur-verification,:volur-auth`：**38 个 verification 用例 + 32 个 auth 用例全部 SUCCESS，0 Failures，0 Errors**。
   - 执行全平台各模块 `mvn test-compile`：**12 个模块全部 SUCCESS**。
