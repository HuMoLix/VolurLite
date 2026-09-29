# 施工与修改记录：SigninServiceImpl Identifier 用户核验与 Google 风格阶梯挑战流转

**日期**：2026-09-30  
**作者**：Antigravity Agent  
**模块**：`volur-auth`、`volur-security`

---

## 1. 变更背景与需求目标

在 Google Accounts 风格的阶段化登录流转体系（VLIF）中，为强化第一阶段（`identifier`）的用户真实性判定，并杜绝攻击者利用字典对系统进行批量账号枚举（Username Enumeration Attack），对服务端登录业务逻辑进行了重构升级：

1. **用户名/邮箱存在性校验与阻断**：
   - 在第一阶段输入标识符后，优先检索数据库确认用户是否存在；
   - 若用户不存在，立即阻断流程推进，不在服务端流转至密码阶段（不生成密码 TL，不递增 `cid`），抛出业务异常使前端直观提示“用户名或邮箱不存在”；
2. **基于 IP + FP 联合指纹的暴力枚举风控阶梯**：
   - 结合客户端真实网络 IP 与设备/环境指纹（`DeviceFingerprint`）计算唯一的联合主体标识 `SubjectKey`；
   - 在 Redis 中维护滑动失败计数器（TTL 15分钟）；
   - **阶梯跃迁机制**：
     - **$< 5$ 次失败**：正常免验通行，直接核验账号并给予明确提示；
     - **$5 \sim 9$ 次失败**：触发中风险人机验证，将登录流程流转至 `recaptcha` 挑战（`captchaType = "IMAGE"`，字符图形验证码）；
     - **$\ge 10$ 次失败**：触发高风险行为验证，将登录流程流转至 `recaptcha` 挑战（`captchaType = "SLIDER"`，滑动拼图验证码）；
3. **Google 风格挑战流转与闭环校验**：
   - 触发人机验证后，流转状态机至 `/challenge/recaptcha`；
   - 用户在挑战阶段提交并通过验证码后，服务端方才执行数据库账号存在性核验：
     - 若核验成功且账号存在：重置该客户端的失败计数，流转进入密码挑战（`pwd`）；
     - 若账号依然不存在：继续累加失败计数并阻断。

---

## 2. 核心架构与时序设计

```
[前端客户端]                                   [后端 SigninServiceImpl]                   [Redis / DB]
     │                                                     │                                   │
     │── POST /api/v1/auth/signin/identifier ─────────────►│                                   │
     │   (identifier, dsh, fp)                             │── 计算 IP+FP 联合 SubjectKey ─────►│
     │                                                     │── 读取失败计数 failures ──────────►│
     │                                                     │                                   │
     │                                                     │── [failures < 5] 查库校验账号 ────►│
     │                                                     │   ├─ 账号不存在: 计数+1, 报错 ────►│ (Redis INCR)
     │◄── 400 "用户名或邮箱不存在" ────────────────────────┤   └─ 账号存在: 清零计数, 进 pwd ──►│
     │                                                     │                                   │
     │                                                     │── [failures >= 5] 触发流转拦截 ───►│ (不查库，防慢查询/枚举)
     │                                                     │   ├─ 5~9次: captchaType=IMAGE     │
     │                                                     │   └─ >=10次: captchaType=SLIDER   │
     │◄── 200 nextStep="recaptcha", cid=1, TL ─────────────┤                                   │
     │                                                     │                                   │
     │── 路由跳转至 /v1/signin/challenge/recaptcha ───────►│                                   │
     │                                                     │                                   │
     │── POST /api/v1/auth/signin/challenge ──────────────►│                                   │
     │   (challengeType="recaptcha", code/ticket)          │── 核销验证码 (CaptchaSDK) ────────►│
     │                                                     │── 核销成功后查库校验账号 ─────────►│
     │                                                     │   ├─ 账号存在: 清零计数, 进 pwd ──►│
     │◄── 200 nextStep="pwd", cid=2, TL ───────────────────┤   └─ 账号不存在: 计数+1, 报错 ────►│
```

---

## 3. 具体修改细节清单

### 3.1 领域模型与会话扩展
1. **`AuthFlowSession.java` (`volur-security`)**：
   - 增加 `private String deviceFingerprint;` 字段，持久化客户端设备指纹。
2. **`SignInFlowResponseVO.java` (`volur-auth`)**：
   - 增加 `private String captchaType;`（`"IMAGE"` / `"SLIDER"`），指示前端当前人机挑战所需的验证码交互模态。
3. **`SignInChallengeRequestDTO.java` (`volur-auth`)**：
   - 增加 `captchaId` 与 `captchaTicket` 字段，同时支持字符图形验证码与滑动拼图通行凭证回传。

### 3.2 登录业务实现重构 (`SigninServiceImpl.java`)
1. **联合指纹计算**：
   - `buildClientSubjectKey(clientIp, rawFp)`：使用 `DigestUtil.md5Hex(cleanIp + "#" + cleanFp)`。
2. **失败计数器与 TTL**：
   - Redis Key：`vredis:auth:identifier_failures:{subjectKey}`；
   - 滑动窗口生命周期：`Duration.ofMinutes(15)`；
   - 提供 `getIdentifierFailures`、`recordIdentifierFailure`、`resetIdentifierFailures`。
3. **`identifier` 逻辑重构**：
   - 若 `failures >= 5`，装配 `captchaType`（`"IMAGE"` 或 `"SLIDER"`）写入 Session，签发 TL 推进至 `recaptcha` 挑战；
   - 若 `failures < 5`，校验数据库，不存在则累加计数并抛出 `BusinessException(HttpStatus.BAD_REQUEST, "用户名或邮箱不存在")`，存在则清零计数并推进至 `pwd`。
4. **`challenge` recaptcha 分支联动**：
   - 严密校验 `recaptcha` 对应的验证码（图形码或滑块 ticket）；
   - 验证通过后方才校验数据库中 `identifier` 是否存在；不存在则继续累计失败计数并抛出“用户名或邮箱不存在”，存在则清零失败计数并签发 `pwd` 步骤 TL，`cid` 推进至下一步。

### 3.3 自动化测试用例 (`SigninServiceImplTest.java`)
- `identifier_userExists_shouldAdvanceToPwd`：校验存在用户正常流转至密码；
- `identifier_userNotFound_shouldRecordFailureAndThrow`：校验不存在用户累加失败计数并报错阻断；
- `identifier_failuresReachFive_shouldAdvanceToRecaptchaWithImage`：校验 5 次失败后流转到图形验证码挑战；
- `identifier_failuresReachTen_shouldAdvanceToRecaptchaWithSlider`：校验 10 次失败后流转到滑动拼图挑战；
- `challenge_recaptcha_success_shouldAdvanceToPwd`：人机验证成功且用户存在推进至密码；
- `challenge_recaptcha_userNotFound_shouldThrow`：人机验证成功但用户不存在继续拦截累加。

执行 `mvn test -pl volur-auth -am` 全量 9 个模块 38 个用例全部通过（`BUILD SUCCESS`）。
