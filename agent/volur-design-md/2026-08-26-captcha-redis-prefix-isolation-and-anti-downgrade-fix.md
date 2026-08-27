# 验证码 Redis 键前缀隔离与滑块核验降级拦截漏洞修复

## 1. 问题根因分析

用户在完成滑块拼图核验时，遇到以下两类报错：
1. **“该场景要求更高级别的人机验证，禁止降级”**
2. **“人机验证未通过或已过期，请重新验证”**

### 根因链路排查：
- **同质化 Redis 键前缀造成 Provider 串扰**：
  [`LineCaptchaProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/LineCaptchaProvider.java)、[`ShearCaptchaProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/ShearCaptchaProvider.java)、[`MathCaptchaProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/MathCaptchaProvider.java) 和 [`SlidePuzzleCaptchaProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/SlidePuzzleCaptchaProvider.java) 全部使用了同一个 Redis 前缀常量：`vredis:verification:captcha:image:{scene}:{captchaId}`。
- **校验轮询执行顺序击穿**：
  在 [`CaptchaServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/CaptchaServiceImpl.java) 中，`verifyAndGetMatchedType` 按照 `CaptchaType.values()` 顺序循环测试各个 Provider。
  由于 `CaptchaType.LINE` 位于数组第一位，且其 Redis 前缀与 `SLIDE_PUZZLE` 完全相同：
  1. **情况 A（滑块坐标与目标值完全一致）**：
     `LineCaptchaProvider.verify` 抢先读取了 Redis 里的 `targetX`，字符串比对判定相等并返回 `true`，导致本次验证被误判为 `LINE`（Level 1 静态验证码）。而注册场景要求最低必须为 `SLIDE_PUZZLE`（Level 3），触发防降级机制并抛出 **“该场景要求更高级别的人机验证，禁止降级”**；
  2. **情况 B（滑块坐标存在微小偏差但在容差范围内）**：
     `LineCaptchaProvider.verify` 抢先读取 Redis 并在比对失败（因字符串不完全相等）后**直接删除了 Redis 键**。当循环轮询到 `SlidePuzzleCaptchaProvider` 时，Redis 键已被提前销毁，导致滑块 Provider 判定过期失败，抛出 **“人机验证未通过或已过期，请重新验证”**。

---

## 2. 修复方案实施

### 2.1 彻底分离各个 Provider 的 Redis 键前缀
在各个 Provider 中引入专属独立前缀，彻底杜绝跨 Provider 的键碰撞与误读误删：
- **`LineCaptchaProvider`**：`vredis:verification:captcha:line:{scene}:{captchaId}`
- **`ShearCaptchaProvider`**：`vredis:verification:captcha:shear:{scene}:{captchaId}`
- **`MathCaptchaProvider`**：`vredis:verification:captcha:math:{scene}:{captchaId}`
- **`SlidePuzzleCaptchaProvider`**：`vredis:verification:captcha:slide:{scene}:{captchaId}`
- **`CloudTurnstileCaptchaProvider`**：`vredis:verification:captcha:turnstile:{captchaId}`

---

## 3. 验证结论
- **Provider 隔离**：滑块挑战只被 `SlidePuzzleCaptchaProvider` 处理与容差判断（$\pm 6\text{px}$），其他 Provider 无法访问滑块键；
- **类型判定**：滑块验证通过后稳定返回 `CaptchaType.SLIDE_PUZZLE`，满足 `REGISTER` 场景最低基准（Level 3），防降级拦截不再误触发；
- **全量测试**：`mvn test` 全平台 11 个模块 100% 通过。
