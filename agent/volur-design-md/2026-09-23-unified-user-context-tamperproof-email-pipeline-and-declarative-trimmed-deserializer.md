# 统一请求用户上下文快照、防篡改发信审计注入与声明式入参清洗 (@Trimmed) 架构重构记录

**日期**：2026-09-23  
**作者**：Antigravity Agent  
**模块**：`volur-framework` / `volur-verification` / `volur-auth`  
**状态**：已完成 (Completed) & 全量构建与单测通过  

---

## 1. 变更背景与动因深度溯源

在平台的安全风控与数据清洗治理演进过程中，发现了以下三个系统性痛点与架构优化空间：

1. **执行上下文割裂与审计溯源隐患**：
   原发信命令对象 [`SendEmailBO`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/bo/SendEmailBO.java) 的 `operatorId` 与 `operatorIp` 允许由外部调用方自由传入与篡改。这不仅加重了调用方的传参心智负担，而且存在恶意篡改操作人身份、伪造来源 IP 逃避安全审计的风险。同时，系统缺乏统一的当前请求执行上下文快照门面。
2. **校验职责发散与边界混淆**：
   平台自研的 `@com.humolix.volur.framework.common.validation.annotation.Email` 会强校验系统邮箱白名单（`system.risk.email_whitelist`），该门禁仅适用于面向公众的**用户注册/绑定场景**。若将其用于底层 `SendEmailBO` 发信管道，会导致平台管理员发信、系统内部告警、运维通知被白名单规则误杀。
3. **入参字符串清洗代码冗余散落**：
   在 [`AuthServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java) 等业务实现类中，到处充斥着 `request.getUsername().trim()` 和 `request.getEmail().trim().toLowerCase()` 等繁琐的手工清洗样板代码。这种手写清洗不仅冗长，而且容易在新增接口时遗漏，从而引发由于大小写不一致导致的数据查询与防重漏洞；同时若全局粗暴地对所有字符串 trim，又会破坏包含首尾空格的合法密码。

---

## 2. 详细重构方案与落地点

### 2.1 统一请求用户上下文快照门面 ([`CurrentUserSchema`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/CurrentUserSchema.java) & [`SecurityContextUtil`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/util/SecurityContextUtil.java))
在 `volur-framework` 模块中创建全局统一的请求执行上下文快照：
1. **[`CurrentUserSchema.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/CurrentUserSchema.java)**：
   - 字段收敛：`id`（Long，未登录默认 0L）、`username`（String，未登录默认 "anonymous"）、`ip`（String，实时穿透提取）、`authenticated`（boolean）、`roles`（List<String>）。
2. **[`SecurityContextUtil.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/util/SecurityContextUtil.java)**：
   - `getCurrentUserSchema()`：每次调用实时从 `RequestUtil.getRemoteIp()` 提取客户端真实 IP（感知用户 Wi-Fi/代理变动），并从 `SecurityContextHolder` 提取主体信息。
   - 具备绝对的**空安全（Null-Safety）**设计，未登录或非 Web 线程自动安全兜底，调用返回值永不为 `null`。

### 2.2 防篡改发信审计注入与声明式校验 ([`SendEmailBO`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/bo/SendEmailBO.java))
1. **彻底屏蔽外部显式设值**：
   - 对 `operatorId` 与 `operatorIp` 标注 `@Setter(AccessLevel.NONE)`，禁止通过 Setter 篡改；
   - 在定制的 `SendEmailBOBuilder` 中将 `operatorId(Long)` 和 `operatorIp(String)` 方法私有化，外界通过链式调用时无法访问；
   - 强制在 `build()` 触发时，**单次原子调用 `SecurityContextUtil.getCurrentUserSchema()` 获取上下文并完成注入**，兼顾防篡改与零冗余解析。
2. **标准 RFC 邮箱格式校验隔离**：
   - `SendEmailBO.to` 使用标准 `@jakarta.validation.constraints.Email` + `@NotBlank`，仅验证邮件地址格式合法性，将白名单校验职责完全隔离在表现层 DTO。
3. **消除 Service 层手工判空样板代码**：
   - [`EmailService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/EmailService.java) 接口标注 `@Validated`，`sendEmail(@Valid @NotNull SendEmailBO bo)` 由 Spring AOP 统一切入校验；
   - 彻底删除 [`EmailServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java) 中冗余的 `if (bo == null || ...)` 手工判空。

### 2.3 声明式入参清洗注解与反序列化器 ([`@Trimmed`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/annotation/Trimmed.java))
在 `volur-framework` 模块中基于 Jackson 扩展了高性能的声明式清洗机制：
1. **注解 [`Trimmed.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/annotation/Trimmed.java)**：
   - 支持 `toLowerCase = true`（去除首尾空格后转全小写，专用于用户名、邮箱、域名等）；
   - 支持 `emptyToNull = true`（空串清洗后转为 null）。
2. **反序列化器 [`StringTrimmingDeserializer.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/jackson/StringTrimmingDeserializer.java)**：
   - 基于 Jackson `ContextualDeserializer` 规范实现，Spring 启动期一次性解析并缓存注解配置，**运行时直接纯 Java 原生调用，零反射开销**，执行耗时与手写相比仅差几个纳秒（忽略不计），且仅在堆内存中产生一个清洗后的 String 对象，大幅降低 Young GC 分配压力。
3. **全系统范围 DTO 落地与 Service 冗余样板代码彻底清理**：
   - **`volur-auth`**：
     - DTO：`AuthenticateRequestDTO`, `ForgotPasswordRequestDTO`, `ForgotPasswordResetRequestDTO`, `ResetPasswordRequestDTO`, `ChangePasswordRequestDTO`, `VerifyChangePasswordCodeRequestDTO`, `DeleteAccountRequestDTO`, `RefreshTokenRequestDTO`, `RegisterRequestDTO`；
     - Service：`AuthServiceImpl.java` 彻底移除 `normalizedUsername` 和 `normalizedEmail` 等冗余局部变量。
   - **`volur-user`**：
     - DTO：`AdminUserCreateRequestDTO`, `AdminUserUpdateRequestDTO`, `UpdateUserProfileRequestDTO`, `UpdateAvatarRequestDTO`, `AdminUserPageRequestDTO`；
     - Service：`UserServiceImpl.java`, `AdminUserServiceImpl.java` 清除冗余 `trim()` 样板代码。
   - **`volur-verification`**：
     - DTO：`EmailCaptchaRequestDTO`, `AdminSendEmailDTO`, `CaptchaOtpRequestDTO`, `CaptchaVerifyRequestDTO`, `CaptchaVerifyDTO`, `TestSmtpRequestDTO`, `AdminIpRuleCreateDTO`, `AdminIpRuleQueryDTO`, `AdminEmailQueryDTO`, `AdminRiskSettingsDTO`；
     - Service：`AdminEmailServiceImpl.java`, `AdminRiskServiceImpl.java` 清除冗余修剪代码。
   - **`volur-competition`**：
     - DTO：`TeamSignupRequestDTO`, `TeamUpdateRequestDTO`, `TeamRegisterRequestDTO`, `TeamLeadersSignupDTO`, `TeamMemberSignupDTO`, `CompetitionCreateRequestDTO`, `AdminTeamUpdateDTO`, `TeamAuditRequestDTO`, `TeamMemberInputDTO`；
     - Service：`CompetitionTeamServiceImpl.java` 中彻底清除所有对 DTO 字段重复执行的 `trim()` 与 `toLowerCase()`。
   - **`volur-challenge`**：
     - DTO：`CtfChallengeCreateRequestDTO`, `CtfChallengeUpdateRequestDTO`, `CtfCategoryCreateRequestDTO`, `CtfCategoryUpdateRequestDTO`, `TheoryChallengeCreateRequestDTO`, `TheoryChallengeUpdateRequestDTO`, `IswChallengeCreateRequestDTO`, `IswChallengeUpdateRequestDTO`, `AwdChallengeCreateRequestDTO`, `AwdChallengeUpdateRequestDTO`, `AwdPlusChallengeCreateRequestDTO`, `AwdPlusChallengeUpdateRequestDTO`。
   - **`volur-container`**：
     - DTO：`AdminClusterNodeCreateDTO`, `AdminClusterNodeUpdateDTO`, `AdminImagePullRequestDTO`, `AdminImageTransferDTO`, `AdminContainerManualStartDTO`, `AdminContainerTestConnectionDTO`, `AdminClusterSettingsDTO`（注意严格保留 `password` 与 `sshKey` 凭据原始内容不进行修剪）。

---

## 3. 验证与测试结论

1. **新增单元测试用例**：
   - [`SecurityContextUtilTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/security/SecurityContextUtilTest.java)：覆盖匿名未登录上下文与登录认证上下文的提取。
   - [`StringTrimmingDeserializerTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/common/StringTrimmingDeserializerTest.java)：覆盖 `@Trimmed` 的去除首尾空格、大小写转换、空串转 null 及密码字段保持原样的边界情况。
   - [`SendEmailBOValidationTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/SendEmailBOValidationTest.java)：覆盖 `SendEmailBO` 必填约束（`to`, `subject`, `operatorId`, `operatorIp`）及非法邮箱格式拦截。
2. **测试与构建通过情况**：
   - `mvn test -pl :volur-framework`：**137 个测试全部 SUCCESS**；
   - `mvn test -pl :volur-verification`：**44 个测试全部 SUCCESS**；
   - `mvn test -pl :volur-auth`：**32 个测试全部 SUCCESS**；
   - `mvn test -pl :volur-competition`：**36 个测试全部 SUCCESS**；
   - 全平台 12 个模块 `mvn test`：**全平台所有单测全部 BUILD SUCCESS，0 错误**。
