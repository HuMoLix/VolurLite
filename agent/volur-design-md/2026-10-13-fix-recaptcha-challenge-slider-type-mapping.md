# 修复人机挑战服务滑块验证码类型映射与降级缺陷

## 1. 变更背景与问题定位

在用户进行登录认证流转时，当用户忘记密码并选择通过邮箱验证码进行核验时，系统根据安全防刷策略（防止接口遭遇邮件轰炸），在 [`ChallengeServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/ChallengeServiceImpl.java) 中插入了前置人机验证挑战步骤（`nextStep: "recaptcha"`）。

在进入 `/v1/signin/challenge/recaptcha` 挑战页面后，前端调用 `GET /api/v1/captcha/challenge/recaptcha` 获取人机挑战。当风控评估判断需要进行行为滑块验证时，下发的 `challengeType` 为 `PUZZLE`，然而返回给前端的响应体中：
1. `backgroundImage` 是一张 130x48 的字符图形验证码图片，而非全幅滑块拼图背景；
2. 滑块拼图碎片字段 `puzzleImage` 与 `puzzleY` 坐标均为 `null`。

导致前端组件无法正常拼装渲染滑块拼图，用户无法通过人机验证。

## 2. 缺陷根因分析

1. **类型参数不匹配与魔法字符串**：
   在 [`RecaptchaServiceImpl.getRecaptchaChallenge`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/RecaptchaServiceImpl.java) 中，调用底层验证码服务时硬编码传入了 `"SLIDER"`（`captchaService.generateChallenge("LOGIN", "SLIDER", null)`）。
2. **底层枚举反序列化解析失败返回 `null`**：
   在底层枚举 [`CaptchaType`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/enums/CaptchaType.java) 中，滑块拼图对应的枚举常量为 `SLIDE_PUZZLE`。`CaptchaType.fromString(typeStr)` 仅根据枚举常量的 `name()` 进行精确匹配，导致 `"SLIDER"` 无法命中任何常量，直接返回了 `null`。
3. **风控类型裁决隐式降级**：
   在 [`RiskEvaluationEngine.resolveEscalatedType`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/engine/RiskEvaluationEngine.java) 中，由于 `requestedType` 为 `null`，系统将其回退至 `LOGIN` 场景的基准最低类型 `CaptchaType.LINE`（字符图形验证码）。
4. **单元测试 Mock 伪造掩盖缺陷**：
   在 [`RecaptchaServiceImplTest`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/RecaptchaServiceImplTest.java) 中，单测直接对 `generateChallenge("LOGIN", "SLIDER", null)` 进行了 Mock 打桩并返回了假数据，掩盖了真实运行时枚举映射解析失败的问题。

## 3. 改动点与修复方案

1. **枚举语义别名增强与容错 (`CaptchaType`)**：
   - 在 [`CaptchaType.fromString`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/enums/CaptchaType.java) 中增强对语义别名的支持：将 `"SLIDER"`、`"PUZZLE"` 正确映射至 `CaptchaType.SLIDE_PUZZLE`，将 `"IMAGE"` 映射至 `CaptchaType.LINE`。
2. **统一消除魔法字符串 (`RecaptchaServiceImpl`)**：
   - 在 [`RecaptchaServiceImpl.getRecaptchaChallenge`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/RecaptchaServiceImpl.java) 中，将硬编码的 `"SLIDER"` 替换为强类型的 `CaptchaType.SLIDE_PUZZLE.name()`，将 `"IMAGE"` 替换为 `CaptchaType.LINE.name()`。
3. **单元测试同步与覆盖 (`RecaptchaServiceImplTest`, `CaptchaTypeTest`)**：
   - 同步修正 [`RecaptchaServiceImplTest`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/RecaptchaServiceImplTest.java) 中的 Mock 预期参数为 `CaptchaType.SLIDE_PUZZLE.name()`；
   - 新增 [`CaptchaTypeTest`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/CaptchaTypeTest.java)，全面覆盖标准枚举名、大小写混用、常见别名（`SLIDER`, `PUZZLE`, `IMAGE`）及非法入参的解析验证。

## 4. 影响范围

- 影响模块：`volur-verification`（基础验证模块）、`volur-security`（人机挑战与安全认证流）。
- 兼容性：向后完全兼容，既规范了平台内部服务调用的强类型枚举规范，又提升了外部接口对不同验证码模态请求的容错鲁棒性。
