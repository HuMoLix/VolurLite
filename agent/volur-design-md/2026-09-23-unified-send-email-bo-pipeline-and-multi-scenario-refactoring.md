# 统一业务发信管道 (SendEmailBO) 抽象与多场景发信重构记录

**日期**：2026-09-23  
**作者**：Antigravity Agent  
**模块**：`volur-verification`  
**状态**：已完成 (Completed) & 全量测试通过  

---

## 1. 变更背景与动因深度溯源

在 [`EmailServiceImpl`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java) 的原有实现中，存在大量按业务场景划分的硬编码发信方法：
- `sendVerificationEmail(EmailCaptchaRequestDTO)`（注册场景）
- `sendChangePasswordEmail(EmailCaptchaRequestDTO)`（修改密码场景）
- `sendDeleteAccountEmail(EmailCaptchaRequestDTO)`（注销账号场景）
- `sendResetPasswordEmail(EmailCaptchaRequestDTO)`（重置密码场景）

**存在的问题与痛点**：
1. **代码膨胀与高重复度**：
   每个场景化发信方法中均重复实现了发信频控计数（`checkRateLimit`）、发件人系统配置读取与判空、邮件正文/模板硬编码拼接、底阶 `MailMessageBO` 实体构建与队列投递逻辑，重复代码占比超过 70%。
2. **缺乏跨模块业务发信统一入口**：
   如果外部业务模块（如 `volur-competition` 比赛通知、`volur-system` 告警、工单客服回复等）需要发送带有模板变量、抄送、密送或自定义主题的邮件，无法直接复用现有的发信基础设施，迫使外部模块要么直接访问底层的 `MailTransportService`（缺乏模板、变量替换与安全限流），要么只能在各自模块中重复造轮子。
3. **领域模型语义边界模糊**：
   外部调用方请求邮件发送时，若直接使用暴露给前端 HTTP 请求的 DTO（如 `*RequestDTO`）跨模块流转，违反了分层架构原则。**DTO 属于面向用户的表现层输入契约，而服务层与跨模块之间流转的复杂业务对象必须统一采用业务对象（BO, Business Object）**。

---

## 2. 详细重构方案与落地点

### 2.1 业务内面发信模型规范化设计 ([`SendEmailBO`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/bo/SendEmailBO.java))
遵循 [`Rules.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-skills/Rules.md) 领域模型与 BO 规范，在 `com.humolix.volur.verification.domain.bo` 包下新建业务对象：
- **注解声明**：`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`，带默认值属性显式添加 `@Builder.Default`。
- **核心字段**：
  - `to`：收件人邮箱（必填）；
  - `cc` / `bcc`：抄送与密送邮箱列表；
  - `subject`：邮件主题（必填）；
  - `content`：直接邮件正文内容（与模板二选一）；
  - `html`：是否为 HTML 格式（默认为 `true`）；
  - `templateName`：邮件模板标识（如 `register_captcha`、`password_reset` 等）；
  - `templateVariables`：模板占位符参数映射字典（`Map<String, Object>`）；
  - `type`：业务类型标识（如 `VERIFICATION`, `NOTIFICATION`, `SYSTEM`, `MANUAL` 等，默认为 `GENERAL`）；
  - `attachments`：邮件附件列表；
  - `checkRateLimit`：是否进行多阶梯发信频控（默认为 `true`，系统级/管理级发信可置为 `false` 旁路）；
  - `async`：是否异步投递（默认为 `true`）；
  - `operatorId` / `operatorIp`：触发操作人与客户端 IP。

### 2.2 高阶发信接口契约升级 ([`EmailService`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/EmailService.java))
在 `EmailService` 中声明统一发信核心管道方法，并提供便捷重载与向后兼容适配：
```java
// 核心通用发信入口 (业务内面统一使用 SendEmailBO)
void sendEmail(SendEmailBO bo);

// 便捷默认重载
default void sendEmail(String to, String subject, String content) {
    sendEmail(SendEmailBO.builder()
            .to(to)
            .subject(subject)
            .content(content)
            .html(true)
            .checkRateLimit(true)
            .build());
}
```

### 2.3 五合一发信管道实现 ([`EmailServiceImpl.sendEmail`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java))
在 `EmailServiceImpl` 中集中收敛发信通用核心流程：
1. **参数严格前置校验**：校验 `bo` 非空、`bo.getTo()` 格式及非空，且 `content` 与 `templateName` 至少具备其一。
2. **多阶梯发信频控安全门禁**：
   当 `bo.isCheckRateLimit()` 为 true 时，自动根据收件人邮箱及客户端 IP 执行阶梯频控（1 分钟单次、1 小时上限、单日上限拦截），杜绝恶意刷信与邮件轰炸风险；当为管理员发信或关键系统通知时，可通过设置为 `false` 安全旁路。
3. **动态模板渲染引擎**：
   - 提取或根据 `templateName` 加载模板；
   - 自动执行占位符动态替换（替换 `${key}` 变量）；
   - 若未匹配到注册模板，自动回退到平台通用的标准卡片式响应式 HTML 邮件骨架，保证全平台邮件视觉风格高度统一。
4. **发件人环境装配**：
   自动从 `SystemSettingService` 读取 `system.mail.from_address` 与 `system.mail.sender_name` 键配置，调用方完全解耦，无需感知底层 SMTP 发件邮箱。
5. **异步/同步投递调度**：
   统一构筑底层 `MailMessageBO`，并委派给 `mailTaskExecutor.executeAsyncMail(message)`（通过虚拟线程或异步任务池）执行发信重试、降级与发信日志持久化。

### 2.4 各场景化方法精简收敛
重构原先所有冗余的场景化发信方法，使其职责完全收敛为：
**「CaptchaSDK 前置人机核验 $\rightarrow$ 验证码生成并存入 Redis $\rightarrow$ 构筑 SendEmailBO $\rightarrow$ 委托 `sendEmail(bo)`」**：
```java
@Override
public void sendVerificationEmail(EmailCaptchaRequestDTO request) {
    captchaSDK.verify(request.getCaptcha(), request.getEmail(), CaptchaScene.REGISTER);
    String code = emailVerificationCodeService.generateAndStoreCode(request.getEmail());
    
    sendEmail(SendEmailBO.builder()
            .to(request.getEmail())
            .subject("Volur 平台账号注册验证码")
            .type("VERIFICATION")
            .templateName("register_captcha")
            .templateVariables(Map.of("code", code, "ttlMinutes", 5))
            .operatorIp(RequestUtil.getRemoteIp())
            .build());
}
```
`sendChangePasswordEmail`、`sendDeleteAccountEmail`、`sendResetPasswordEmail` 均采用同等模式进行了彻底的瘦身重构，单方法代码量由原先的 50+ 行锐减至 10 行以内。

---

## 3. 跨模块调用示范 (Example Usage)

其他业务模块（如 `volur-competition`、`volur-system`、`volur-user`）只需通过 Spring 依赖注入 `EmailService`，即可一行代码完成各种定制发信需求：

### 示例 1：比赛通告与工单邮件提醒（使用模板与参数）
```java
emailService.sendEmail(SendEmailBO.builder()
        .to("player@example.com")
        .subject("【Volur 赛事通知】比赛即将开始提醒")
        .type("NOTIFICATION")
        .templateName("competition_notice")
        .templateVariables(Map.of(
                "competitionTitle", "Volur 极客安全挑战赛",
                "startTime", "2026-10-01 09:00:00"
        ))
        .checkRateLimit(false) // 官方通告免除用户端发信限频
        .build());
```

### 示例 2：系统告警与关键运维邮件（直接正文纯文本/HTML）
```java
emailService.sendEmail(SendEmailBO.builder()
        .to("ops-admin@volur.io")
        .subject("[ALERT] 服务器容器资源告警")
        .content("<p>节点 <strong>Node-01</strong> 内存利用率已达 92%，请及时处置。</p>")
        .type("ALERT")
        .checkRateLimit(false)
        .build());
```

---

## 4. 验证与测试结论

1. **单元测试深度覆盖**：
   在 [`EmailServiceVerificationTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/EmailServiceVerificationTest.java) 中新增了针对 `sendEmail(SendEmailBO)` 管道的系列单元测试：
   - `testSendEmail_SuccessWithDirectContent`：直接内容发送与异步调度验证；
   - `testSendEmail_SuccessWithTemplateVariables`：模板占位符渲染与变量替换断言；
   - `testSendEmail_RateLimitBypassWhenDisabled`：旁路限频开关 `checkRateLimit(false)` 行为验证；
   - `testSendEmail_MissingRecipientThrowsException`：收件人空参防呆断言；
   - `testSendEmail_MissingContentAndTemplateThrowsException`：内容模板双空非法参数阻断断言。
2. **测试与构建通过情况**：
   - `mvn test -pl :volur-verification`：**41 个单元测试用例全部 SUCCESS，0 Failures, 0 Errors**。
   - `mvn test-compile`：平台 12 个模块全部编译通过。
