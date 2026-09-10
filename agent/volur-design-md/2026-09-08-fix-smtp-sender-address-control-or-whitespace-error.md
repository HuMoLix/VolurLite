# SMTP 邮件发件人地址格式解析异常 (Local address contains control or whitespace) 修复方案

## 1. 变更背景与问题定位

### 1.1 错误现象
管理员在平台前端「系统设置 -> 发信与 SMTP 设置」完成邮件配置后，在执行发送测试邮件或系统触发邮件投递（如注册验证码、找回密码、手动发信）时，后台抛出以下异常：

```
2026-09-08T01:05:23.463+08:00  WARN 77672 --- [volur-server] [nio-8080-exec-3] c.h.v.v.m.p.SmtpMailDeliveryProvider     : [Volur-Verification] SMTP 邮件 HTML 投递失败，尝试降级纯文本: Local address contains control or whitespace
2026-09-08T01:05:23.464+08:00  WARN 77672 --- [volur-server] [nio-8080-exec-3] c.h.v.v.m.d.FailoverMailDispatcher       : [Volur-Verification] 发信渠道 [SMTP] 投递失败，尝试自动容灾降级: Local address contains control or whitespace
2026-09-08T01:05:23.466+08:00  WARN 77672 --- [volur-server] [nio-8080-exec-3] .m.p.LoggingFallbackMailDeliveryProvider : [Volur-Verification] [Fallback] 未激活外部真实邮件服务，已在日志中记录邮件 -> to: humolix@163.com, subject: test, content: test
```

### 1.2 根本原因溯源
1. **概念倒置与键名混淆**：
   - 动态配置项 `system.mail.smtp_user` 对应发件人邮箱账号（RFC 5321 / RFC 5322 addr-spec，如 `service@volur.local` 或 `xxx@163.com`）。
   - 动态配置项 `system.mail.sender_name` 对应发件人对外显示昵称（Personal Display Name，如 `Volur Lite 安全演练平台` 或 `Volur Lite Security Team`）。
   - 原业务实现类 `EmailServiceImpl` 与 `AdminEmailServiceImpl` 中，错误地使用了以下优先提取逻辑：
     ```java
     String from = systemSettingService.getSettingValue("system.mail.sender_name", "");
     if (!StringUtils.hasText(from)) {
         from = systemSettingService.getSettingValue("system.mail.smtp_user", "SYSTEM");
     }
     ```
     只要管理员在前端配置了「发件人显示昵称」（默认或自定义，如 `"Volur Lite Security Team"`），`from` 变量就会直接被赋值为该昵称字符串。
2. **JavaMail 协议层地址校验失败**：
   - `SmtpMailDeliveryProvider` 直接调用 `helper.setFrom(from)`。
   - Spring Framework 的 `MimeMessageHelper.setFrom(String from)` 会通过 `new InternetAddress(from)` 进行地址解析。
   - 当 `from` 中不包含 `<>` 且含有空格字符（`' '`，ASCII 32，八进制 040）或非 ASCII 控制字符时，Jakarta Mail 的 `InternetAddress.checkAddress` 方法会判定其为非法的 local-part 并直接抛出：
     `AddressException: Local address contains control or whitespace`。
   - HTML 发送失败后降级为纯文本发信时，由于使用同一个 `from` 变量并再次调用 `helper.setFrom(from)`，导致二次失败，最终触发 `FailoverMailDispatcher` 降级到日志记录。

---

## 2. 核心改动点

### 2.1 业务对象增强 (`MailMessageBO`)
- [MailMessageBO.java](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/bo/MailMessageBO.java)：
  - 增加 `private String senderName;` 字段，严格区分「发件人邮箱地址（from）」与「发件人显示昵称（senderName）」，彻底解除两者混用耦合。

### 2.2 服务层发信逻辑修正 (`EmailServiceImpl` & `AdminEmailServiceImpl`)
- [EmailServiceImpl.java](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java)：
  - 修正 `sendVerificationCodeEmail`、`sendResetPasswordEmail` 及 `sendEmail` 方法。
  - 从 `system.mail.smtp_user` 获取发件邮箱地址，从 `system.mail.sender_name` 获取昵称，并分别注入 `MailMessageBO.from` 与 `MailMessageBO.senderName`。
  - 在 `emailLogRepository.save` 中规范记录实际的发信邮箱账号（而非显示昵称）。
- [AdminEmailServiceImpl.java](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/admin/impl/AdminEmailServiceImpl.java)：
  - 修正 `sendManualEmail` 方法，与 `EmailServiceImpl` 保持一致的规范。

### 2.3 SMTP 投递通道健壮性重构 (`SmtpMailDeliveryProvider`)
- [SmtpMailDeliveryProvider.java](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/provider/SmtpMailDeliveryProvider.java)：
  - 新增 `applySender(MimeMessageHelper helper, JavaMailSenderImpl mailSender, MailMessageBO message)` 方法：
    1. **智能邮箱提取与容错**：检测 `rawFrom` 是否包含 `<...>` 复合格式；若包含则提取尖括号内的真实邮箱并将尖括号前的内容作为昵称；
    2. **历史脏数据自愈**：若 `message.getFrom()` 误传入了不带 `@` 的纯文本昵称，自动将其移交为 `senderName`，发信地址回退到 `mailSender.getUsername()` 或 `system.mail.smtp_user`，杜绝任何格式崩溃；
    3. **双参数合规设置**：当存在显示昵称时，调用 `helper.setFrom(fromAddress, senderName)`，利用 Jakarta Mail 的 RFC 2047 自动编码（Base64/Q-encoding），生成合法规范的 `From: =?UTF-8?B?...?= <service@volur.local>` 标头；无昵称时调用 `helper.setFrom(fromAddress)`；
    4. **收件人空白清洗**：对 `message.getTo()` 进行强制 `trim()`，防止收件人首尾空格触发相同异常；
    5. **降级通道同步修复**：纯文本 fallback 发送流程同步接入 `applySender` 与收件人清洗；
    6. **SSL 与加密连接强化**：在 `createMailSender` 中显式读取 `system.mail.smtp_ssl` 开关并配置 SSL SocketFactory 与主机信任属性。

---

## 3. 自动化测试与验证

### 3.1 新增单元测试
创建 [SmtpMailDeliveryProviderTest.java](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/SmtpMailDeliveryProviderTest.java)，覆盖以下关键用例：
1. `testApplySenderWithWhitespaceAndChineseDisplayNameDoesNotThrowException`：包含中文与空格的显示昵称（`Volur Lite 安全演练平台`）正常解析为 `InternetAddress`，不抛出异常；
2. `testApplySenderHandlesLegacyNicknameInFromGracefully`：遗留历史代码在 `from` 传入包含空格的昵称（`Volur Lite Security Team`），系统自动将其识别为显示昵称并提取正确发件邮箱，安全自愈；
3. `testApplySenderWithAngleBracketsInFrom`：支持标准 `Name <email@domain>` 格式的复合发件人解析；
4. `testApplySenderWithoutSenderName`：无昵称时仅输出裸邮箱。

### 3.2 验证结果
执行 `mvn test`：
- `volur-verification` 模块：11 个测试全部通过（0 失败，0 错误）；
- 平台全模块测试：全部构建并测试通过（12/12 SUCCESS）。

---

## 4. 影响范围与审计结论

- **影响模块**：`volur-verification`
- **向后兼容性**：100% 兼容既有数据库实体、API 接口和前端交互逻辑，无破坏性变更。
- **安全与稳定性提升**：消除了管理员配置发件人昵称后 SMTP 发信必定失败的致命缺陷，修复了纯文本降级逻辑并防止邮件日志中将发件人邮箱记录为杂乱中文昵称。
