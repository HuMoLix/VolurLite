# 审计与施工记录：修复登录流转中 reCAPTCHA 核验后回退至密码挑战 (pwd) 的会话元数据覆盖缺陷

## 1. 缺陷背景与现象定位

在用户选择“更多登录方式”并点击“电子邮箱验证”进入人机安全核验流程（`/v1/signin/challenge/recaptcha`）后，完成人机身份验证并提交挑战时，页面并未如期跳转至邮箱验证码输入页（`/v1/signin/challenge/etotp`），而是意外跳转到了密码验证挑战页（`/v1/signin/challenge/pwd`）。

---

## 2. 根因深度剖析 (Root Cause Analysis)

### 2.1 会话元数据覆盖与内存对象实例失步
在 `ChallengeServiceImpl.selectChallenge` 方法中：
1. 方法入口处通过 `Session session = sessionManager.getSession(dsh)` 从 Redis 反序列化加载当前内存会话对象 `session`（此时 `session.getMetadata()` 尚无 `pendingChallenge` 键）；
2. 当判定需要前置人机验证（`requireRecaptcha = true`）时，原代码调用了 `sessionManager.setMetadata(dsh, "pendingChallenge", pendingStep)`；
3. `SessionManager.setMetadata` 内部通过单独的 `getSession(dsh)` 获取了一份新的会话实例并写回 Redis；
4. 然而，在 `selectChallenge` 方法末尾，继续执行了 `sessionManager.saveSession(session)`，传入的却是方法第一步加载的本地变量 `session`；
5. 该本地 `session` 的 `metadata` 并不包含 `pendingChallenge`，导致随后的 `saveSession(session)` 将 Redis 中刚刚写入的 `pendingChallenge` **完全覆盖清空**；
6. 最终，当客户端完成 reCAPTCHA 并发起 `/captcha/challenge/recaptcha` 提交时，`verifyReCAPTCHA` 从 Redis 查询 `pendingChallenge` 为 `null`，状态机未能匹配到待定挑战，从而兜底回退签发了密码挑战（`nextStep: "pwd"`），导致前端路由至 `/v1/signin/challenge/pwd`。

---

## 3. 修复方案与安全加固

### 3.1 同步更新本地会话元数据与单次原子持久化
在 [`ChallengeServiceImpl.selectChallenge`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/ChallengeServiceImpl.java) 中：
- 在保存会话前，直接在当前 `session` 实例的 `metadata` 中设入 `pendingChallenge`（`session.getMetadata().put("pendingChallenge", pendingStep)`)；
- 避免后续 `sessionManager.saveSession(session)` 发生元数据丢失与覆盖。

### 3.2 增加双重兜底探测与生命周期安全清理
在 [`ChallengeServiceImpl.verifyReCAPTCHA`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/ChallengeServiceImpl.java) 中：
1. 优先从 `sessionManager.getMetadata(dsh, "pendingChallenge")` 提取待定挑战；
2. 若为空，进一步从当前 `session.getMetadata()` 探测；
3. 若仍为空，通过状态机 `SigninFlow.getWaitingSteps()` 进行结构化容灾推导：当发现 `waitingSteps` 中紧随 `recaptcha` 之后为 `etotp` 或 `smsotp` 时，精准识别并恢复待定挑战目标，杜绝任何意外回退到 `pwd`；
4. 挑战消费后，同时从 Redis 与本地 `session.getMetadata()` 中移除 `pendingChallenge`，确保生命周期原子核销。

---

## 4. 验证与测试结果

1. **后端单元与集成测试**：
   - 补全测试用例 `verifyReCAPTCHA_withWaitingStepsFallback_shouldSendEmailAndAdvanceToEtotp`，断言在元数据丢失边缘场景下依仗状态机步骤仍能安全恢复目标流程；
   - 运行 `mvn test -pl volur-security,volur-auth`，全部 166 个测试用例通过（`volur-security`: 104 用例全绿，`volur-auth`: 62 用例全绿）。
2. **前端类型与编译检查**：
   - 运行 `npx vue-tsc --noEmit`，TypeScript 0 错误 0 警告。
