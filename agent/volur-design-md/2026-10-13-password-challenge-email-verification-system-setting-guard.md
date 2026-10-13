# 登录密码验证后邮件二次安全核验前置系统开关校验施工记录

## 1. 变更背景

在原认证流程中，用户在通过密码挑战（`PasswordController.verify` / `PasswordServiceImpl.verifyPassword`）后，系统会探测设备是否受信任（`userTrustedDeviceService.isTrustedDevice`）。若设备不受信任且账号存在邮箱，原逻辑会无条件触发邮箱二次安全挑战（`etotp`），并发起邮件发送请求。

### 1.1 缺陷与安全隐患溯源
1. **未校验系统邮箱验证开关 (`system.auth.require_email_verification`)**：当平台运维或管理员在全局设置中关闭邮箱验证时，登录流程仍强行派发验证码并阻断用户登录；
2. **未校验邮件服务可用性开关 (`system.mail.enabled`)**：若当前平台未启用邮件服务（默认 `system.mail.enabled = false`）或未配置 SMTP 服务，强行下发邮件会导致调用底层邮件服务异常，进而导致新设备用户陷入既收不到验证码又无法推进登录流程的死循环。

---

## 2. 施工改动详述

### 2.1 服务层前置开关守卫 (`volur-security`)
在 [`PasswordServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/PasswordServiceImpl.java) 的密码核销方法中，新增读取并联合校验两项系统动态配置：
- `SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION` (`"system.auth.require_email_verification"`，默认 `true`)
- `SystemSettingKeys.MAIL_ENABLED` (`"system.mail.enabled"`，默认 `false`)

**触发 ETOTP 邮件验证的充分必要条件重构为**：
```java
boolean isTrusted = StringUtils.hasText(deviceHash)
        && userTrustedDeviceService.isTrustedDevice(user.getId(), deviceHash);

boolean requireEmailVerification = systemSettingService.getBoolean(SystemSettingKeys.AUTH_REQUIRE_EMAIL_VERIFICATION, true);
boolean mailEnabled = systemSettingService.getBoolean(SystemSettingKeys.MAIL_ENABLED, false);

if (requireEmailVerification && mailEnabled && !isTrusted && StringUtils.hasText(user.getEmail())) {
    // 派发邮箱验证码并推进至 etotp 步骤
    ...
}
```

### 2.2 单元测试覆盖 (`PasswordServiceImplTest.java`)
在 [`PasswordServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/impl/PasswordServiceImplTest.java) 中补充测试用例：
1. `verifyPassword_untrustedDevice_shouldTriggerEtotp`：验证两项配置均为 `true` 且设备不受信任时，正常派发邮件并推进至 `etotp`；
2. `verifyPassword_untrustedDevice_mailDisabled_shouldAdvanceToAuthenticate`：验证邮件服务关闭（`mail.enabled = false`）时，不受信任设备自动跳过 `etotp`，直接进入最终认证；
3. `verifyPassword_untrustedDevice_requireEmailDisabled_shouldAdvanceToAuthenticate`：验证未开启强制邮箱验证（`require_email_verification = false`）时，自动跳过 `etotp`。

---

## 3. 效益与影响范围

1. **鲁棒性提升**：彻底解决了未配置/未开启邮件服务时，新设备用户登录被死锁在邮箱验证步骤的问题；
2. **符合单一真实源与动态配置规范**：严格响应管理后台系统设置开关，避免业务硬编码；
3. **影响范围**：
   - 核心密码挑战核销逻辑：[`PasswordServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/PasswordServiceImpl.java)
   - 单元测试：[`PasswordServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/impl/PasswordServiceImplTest.java)
