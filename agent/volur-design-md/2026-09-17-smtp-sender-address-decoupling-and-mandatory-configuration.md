# SMTP 发件人邮箱地址解耦与全系统非空强制校验设计与施工报告

## 1. 变更背景与需求动因

### 1.1 背景与现状
在早期的邮件管理系统设计中，系统配置仅包含「发信人邮箱账号（`system.mail.smtp_user`）」与「发件人显示昵称（`system.mail.sender_name`）」。
对于传统邮箱（如 QQ 邮箱、163 邮箱），发信账号即为发件人邮箱（`xxx@qq.com`）。然而在对接现代云发信中间件与 SMTP 中继（如 **Resend**、SendGrid、AWS SES、Mailgun 等）时：
- **SMTP 认证用户名**：具有固定标识（如 Resend 固定为 `resend`，SendGrid 固定为 `apikey`）；
- **发件人地址（From）**：必须是已在云平台验证过 DNS 域名的合法邮件地址（如 `notify-noreply@volurctf.com`）。

原系统由于未将「SMTP 认证账号」与「发件人邮箱地址」解耦，在配置 Resend 时会导致邮件消息将 `resend` 字符串作为发件人地址组装并投递，进而被 SMTP 中继网关当场拦截报错。

### 1.2 用户明确约束
1. **取消回退策略**：明确废除发件人地址向发信人账号的回退兜底逻辑，强制要求独立配置；
2. **强制全必填**：在发信人邮箱账号下方新增「发件人邮箱地址」，全链路要求非空，不允许留空；
3. **占位符规范**：输入框占位符（Placeholder）统一填写为：`“QQ等邮箱保持与发信人邮箱账号一致即可”`。

---

## 2. 核心架构设计与改动点

### 2.1 系统常量与持久化层 (`volur-system`)
- **[SystemSettingKeys.java](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/constant/SystemSettingKeys.java)**：
  - 新增标准层级键名常量：
    ```java
    public static final String MAIL_FROM_ADDRESS = "system.mail.from_address";
    ```
- **[SystemSettingInitializer.java](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/init/SystemSettingInitializer.java)**：
  - 在 `MIGRATION_MAPPINGS` 迁移字典中注册 `SMTP_FROM_ADDRESS` -> `SystemSettingKeys.MAIL_FROM_ADDRESS` 映射，确保历史配置清洗平滑。

### 2.2 发信核心与服务层重构 (`volur-verification`)
- **[SmtpMailDeliveryProvider.java](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/provider/SmtpMailDeliveryProvider.java)**：
  - **服务可用性探活（`isAvailable`）**：强化校验条件，将 `system.mail.from_address` 纳为必填判定项，任一关键项缺失则标记渠道不可用。
  - **发信人装配（`applySender`）**：彻底剔除向 `smtp_user` 的降级回退；从 `message.getFrom()` 或 `system.mail.from_address` 提取后进行非空与 RFC 格式合规性强校验（`contains("@") && !hasWhitespace`），不合规直接抛出异常阻断。
  - **客户端初始化（`createMailSender`）**：在构建 `JavaMailSenderImpl` 时校验 `fromAddress` 必填性。
- **[EmailServiceImpl.java](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java)**：
  - 注册验证码（`sendVerificationCodeEmail`）、重置密码（`sendResetPasswordEmail`）、通用发信（`sendEmail`）全面切为读取 `SystemSettingKeys.MAIL_FROM_ADDRESS`。若未配置直接抛出 `IllegalStateException`，日志中真实记录发信地址。
- **[AdminEmailServiceImpl.java](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/admin/impl/AdminEmailServiceImpl.java)**：
  - 管理员手动发信（`sendManualEmail`）与重发均读取 `SystemSettingKeys.MAIL_FROM_ADDRESS`，执行非空严格拦截。

### 2.3 前端视图与多语言国际化 (`volur-lite-view`)
- **[MailSettingsTab.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/tabs/MailSettingsTab.vue)**：
  - 在「发信人邮箱账号（`smtpUser`）」项下方新增「发件人邮箱地址（`fromAddress`）」表单项，添加 `required` 标红标记；
  - placeholder 绑定多语言 `t('admin.system.mail.from_address_placeholder')`。
- **[AdminSystemView.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue)**：
  - 在 `settingsForm` 状态中纳入 `fromAddress`；
  - 在 `fetchSettings()` 与 `handleResetDefault()` 中对齐键名 `system.mail.from_address`；
  - 在 `handleSaveSettings()` 中增加保存前校验拦截：`smtpHost`、`smtpUser`、`fromAddress` 均不得为空，为空立即弹出 Toast 阻断。
- **国际化字典**：
  - [zh_CN/admin/system.json](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/system.json)：配置 `from_address: "发件人邮箱地址"` 与 `from_address_placeholder: "QQ等邮箱保持与发信人邮箱账号一致即可"`。
  - [en_US/admin/system.json](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/system.json)：配置英文对应翻译。

---

## 3. 测试与验证

### 3.1 自动化单元测试
- **[SmtpMailDeliveryProviderTest.java](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/SmtpMailDeliveryProviderTest.java)**：
  - 覆盖 `testApplySenderThrowsExceptionWhenFromAddressMissing`：验证发件人地址缺失时准确抛出 `IllegalStateException`；
  - 覆盖 `testIsAvailableRequiresFromAddress`：验证发件人地址为空时 `isAvailable()` 返回 `false`；
  - 验证遗留纯昵称传递场景下正确自愈并读取 `from_address`。
- **[EmailServiceVerificationTest.java](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/EmailServiceVerificationTest.java)**：
  - 覆盖 `testSendVerificationEmailThrowsWhenFromAddressNotConfigured`：验证未配置时阻断。

### 3.2 运行结果
- **后端测试**：全平台全部模块 `mvn test` 测试全部通过（14/14 tests in verification, 0 failures, BUILD SUCCESS）；
- **前端构建**：`vue-tsc -b && vite build` 编译打包 0 错误通过。

---

## 4. 影响范围与审计说明
- **变更影响模块**：`volur-system`, `volur-verification`, `volur-lite-view`。
- **兼容性说明**：升级后管理端配置更加严密，彻底解除发信账号与发件地址耦合，可原生支持 Resend、SendGrid、AWS SES 等所有主流现代云邮件服务商。
