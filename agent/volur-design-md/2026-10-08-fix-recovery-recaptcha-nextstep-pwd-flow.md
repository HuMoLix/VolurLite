# 修复密码恢复流程中 ReCAPTCHA 响应 nextStep 为 pwd 的缺陷分析与修复记录

## 1. 缺陷现象
在执行密码恢复流程（`/v1/recovery/password/selection` -> `/v1/recovery/challenge/recaptcha`）过程中，用户完成人机验证后，前端控制台抛出异常并阻断：
```
未知的下一步挑战流程: pwd
```

---

## 2. 根本原因深入推导 (Root Cause Analysis)
经过全链路代码时序与数据流追踪，该缺陷由后端两处关键状态保存与流转判定时序问题共同导致：

1. **分布式会话对象内存态与 Redis 存储覆盖冲突 (In-memory Session Desynchronization & Clobbering)**：
   - 当用户在 `/v1/recovery/password/selection` 选择找回方式时，调用了 `/api/v1/auth/recovery/password/select`；
   - 在 `RecoveryServiceImpl.selectPasswordRecoveryMethod` 中，通过 `sessionManager.recoverySession(dsh, ...)` 读取了当前会话实例 `session` 到内存中；
   - 随后代码调用 `sessionManager.setMetadata(session.getDsh(), "recoveryType", "password")`。该方法在底层重新从 Redis 加载了一个新的反序列化会话对象，将 `recoveryType` 写入并写回 Redis；
   - 但在随后为生成 TL 挑战时，由于进入了未带旧 TL 的分支，执行了 `sessionManager.saveSession(session)`。而此时入参中的内存 `session` 对象为最初加载的对象（其 `metadata` 仍为 `null` 或旧值），导致刚写入 Redis 的 `recoveryType` 元数据被全量覆盖、彻底丢失。
2. **人机验证接口的多态分流失效，回退至普通登录挑战逻辑**：
   - 当用户在 `/v1/recovery/challenge/recaptcha` 提交人机验证凭据时，调用 `/captcha/challenge/recaptcha`，由 `ChallengeServiceImpl.verifyReCAPTCHA` 处理；
   - 此时由于此前账号在进入密码挑战时已经输入过账户标识符，`session.getFlow().getIdentifier()` 存在且不为空；
   - 系统尝试通过 `sessionManager.getMetadata(dsh, "recoveryType")` 读取找回类型，因上述覆盖问题读到了 `null`；
   - 系统误认为这是一次普通的登录人机挑战（例如密码登录前触发的风险核验），因此核验通过后直接执行普通登录流转——签发面向密码输入的 TL，并返回了 `nextStep: "pwd"`；
   - 前端接收到本不属于找回密码流程的 `"pwd"`，依据 Fail-Fast 原则抛出 `未知的下一步挑战流程: pwd`。

---

## 3. 修复方案实施细节

### 3.1 后端会话状态原子同步 (`RecoveryServiceImpl`)
- 在 `selectPasswordRecoveryMethod` 和 `selectRecoveryMethod` 中，废除在存在本地会话引用时脱离当前引用去调 `sessionManager.setMetadata` 的反模式；
- 改为直接就地修改当前内存引用 `session.getMetadata().put("recoveryType", "password")`，并在统一由 `sessionManager.saveSession(session)` 刷回 Redis 时，原子性保证流转状态（`currentStep="recaptcha"`、`activeTL`、`cid`）与元数据（`recoveryType`、`recoveryMethod`）完整持久化，杜绝覆盖。

### 3.2 人机核验流转判定增强与双重保险 (`ChallengeServiceImpl` & `ReCAPTCHAChallengeSubmitDTO`)
- 在 `ReCAPTCHAChallengeSubmitDTO` 中增加可选字段 `@Trimmed private String recoveryType`；
- 在 `ChallengeServiceImpl.verifyReCAPTCHA` 中采用双保险判定：优先使用请求体显式传递的 `recoveryType`，其次回退读取 `sessionManager.getMetadata(dsh, "recoveryType")`；
- 优先判定密码恢复（`"password".equalsIgnoreCase(recoveryType)`），若命中则签发 `emailotp` 票据、更新会话 flow 步骤为 `emailotp`，并返回 `nextStep: "emailotp"`；
- 判定用户名恢复（`"username".equalsIgnoreCase(recoveryType) || !StringUtils.hasText(identifier)`），签发 `email` 票据并返回 `nextStep: "email"`；
- 彻底隔离恢复流程与常规登录人机挑战的分流边界。

### 3.3 前端 API 与核验调用对齐 (`captcha.ts` & `recaptchaChallengeView.vue`)
- 在 `submitRecaptchaChallengeApi` 中补充可选 `recoveryType?: string` 强类型定义；
- 在 `recaptchaChallengeView.vue` 提交核验时，从路由参数中提取 `recoveryType` 一并送至后端。

---

## 4. 验证结果
- **后端单元测试**：
  - `mvn test -pl volur-security -Dtest=Challenge*`：6/6 全部通过；
  - `mvn test -pl volur-auth -Dtest=Recovery*`：12/12 全部通过；
- **前端生产构建**：
  - `npm run build`：0 错误，类型推断全部通过。
