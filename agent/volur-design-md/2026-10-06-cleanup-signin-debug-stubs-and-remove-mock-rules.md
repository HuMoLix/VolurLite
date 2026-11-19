# 登录服务早期调试桩代码清理与 Zero-Mock 规范对齐记录

## 1. 变更背景与原因
在早期的登录脚手架和流程调试阶段，`SignInServiceImpl` 中遗留了两处临时调试桩代码：
1. **关键词伪判定**：在 `identifier` 函数中，存在 `lowerIdentifier.contains("bot") || lowerIdentifier.contains("captcha") || lowerIdentifier.contains("robot")` 判断，强行触发人机验证；
2. **用户名伪两步验证触发**：在 `pwd` 挑战处理中，存在 `boolean require2fa = lusername.contains("2fa") || lusername.contains("totp");`，强行注入 `2fa` 挑战步骤。

上述桩代码不仅对真实的黑产爆破没有任何防护能力，而且会严重误伤包含此类词根的正常合法用户名（例如以 `bot` 结尾的真实账号），违背了《GEMINI.md》中的 **Zero-Mock & Fail-Fast 原则**。

## 2. 核心变更内容
- **清理 `identifier` 中的关键词假判定**：
  移除所有包含 `bot`、`captcha`、`robot` 词根的判定分支，仅保留纯粹基于真实风控引擎评级的判定：
  ```java
  boolean isEnvSuspicious = riskLevel == RiskLevel.HIGH;
  ```
- **清理 `challenge("pwd")` 中的假 2FA 触发逻辑**：
  移除基于用户名是否包含 `2fa` 或 `totp` 的强行拦截分支；密码校验通过后直接进入下一步（现阶段收敛至 `authenticate` 终态，后续接入真正的用户受信设备库与自适应 MFA 决策体系）。

## 3. 测试与验证
- 针对 `volur-auth` 模块执行全量单元测试：
  ```bash
  mvn test -pl volur-auth -am
  ```
- 结果：**57 个测试用例 100% 通过**，无回归错误，代码严格遵循纯粹无注释规范。
