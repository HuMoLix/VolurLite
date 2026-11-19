# 2026-10-06 Flow 模块领域模型收敛与事务锁管理器 API 演进方案

## 1. 变更背景与目标

为严格遵守 `GEMINI.md` 与 `Rules.md` 领域模型规范（第 2 条：领域模型统一收敛于 `domain` 包路径下，严禁在模块根路径平铺），以及提升现代 Google 风格状态机事务锁（Transition Lock）生命周期 API 的语义自解释性与一致性，本次对 `volur-security` 下的 `flow` 子包及其关联业务模块实施重构：
1. **FlowNames 移入 domain 包**：将平铺在 `com.humolix.volur.security.flow` 根路径下的 `FlowNames.java`（及其内部枚举 `FlowEntry`）完整迁移至 `com.humolix.volur.security.flow.domain`，与 `Flow.java` 模型保持高内聚，并全链路无遗留更新所有引用与单测。
2. **TransitionLockManager 方法重命名与语义升级**：
   - 将原底层生成方法 `generateTL` 升级为面向业务领域语义的 `createTransLock`；
   - 将状态机推进方法 `advanceTransitionTicket` 升级为紧凑且与事务锁强关联的 `nextTransLock`；
   - 将校验方法 `validateTransitionTicket` 与 `validateStepAndTl` 参考 `createTransLock` 与 `nextTransLock` 的命名规范，统一收敛并简化重构为重载方法 **`validateTransLock`**；
   - 全链路同步修改 `TransitionLockManager`、`PasskeyServiceImpl`、`RecaptchaServiceImpl`、`SignInServiceImpl`、`SignUpServiceImpl` 以及对应单元测试类，杜绝兼容性桥接与历史包袱。
3. **关键安全校验与生命周期深度剖析**：
   - 对比剖析 `validateTransLock` 4 参（强状态机步进与核销）与 3 参（阶段内挑战与辅助操作核验）的区别与安全边界；
   - 深入剖析 `burnAndCompleteSession` 的会话生命周期终局原子核销与一次性票据防护价值。

---

## 2. 变更内容与改动清单

### 2.1 领域模型包路径调整 (Domain Model Relocation)
- **迁移源文件**：`com.humolix.volur.security.flow.FlowNames` -> `com.humolix.volur.security.flow.domain.FlowNames`；
- **迁移测试文件**：`com.humolix.volur.security.flow.FlowNamesTest` -> `com.humolix.volur.security.flow.domain.FlowNamesTest`；
- **调用方依赖全链路更新**：
  - `com.humolix.volur.security.flow.TransitionLockManager`：新增 `import com.humolix.volur.security.flow.domain.FlowNames`；
  - `com.humolix.volur.security.flow.flows.SigninFlow`：更新为引入 `domain.FlowNames`，补充 `STEP_ETOTP = "etotp"` 契约；
  - `com.humolix.volur.security.session.SessionManager`：更新为引入 `domain.FlowNames`；
  - `com.humolix.volur.auth.service.impl.SigninServiceImpl`：更新引入 `domain.FlowNames`；
  - `com.humolix.volur.auth.service.impl.SignupServiceImpl`：更新引入 `domain.FlowNames`；
  - `com.humolix.volur.security.flow.flows.SigninFlowTest`：更新为引入 `domain.FlowNames`。

### 2.2 TransitionLockManager API 升级与方法收敛
- **`createTransLock`**（原 `generateTL`）：
  - 签名：`public String createTransLock(String dsh, int cid, String step, String identifier)`
  - 职责：委托 `TransitionLockGenerator` 生成具备 HMAC-SHA256 签名、绑定 `dsh|cid|step|identifier|timestamp` 的 60 位高熵 `ADG-` 事务锁票据。
- **`nextTransLock`**（原 `advanceTransitionTicket`）：
  - 签名：`public String nextTransLock(String dsh, String currentTl, int currentCid, String completedStep, int nextCid, String nextStep, String identifier)`
  - 职责：核验前置阶段票据合法性 -> 标记前置步骤完成 -> 生成下一阶段 `TransLock` 并刷新会话状态 -> 持久化会话至 Redis。
- **`validateTransLock`（重载收敛原 `validateTransitionTicket` 与 `validateStepAndTl`）**：
  - 强步进校验签名：`public void validateTransLock(String dsh, String tl, int cid, String expectedStep)`
  - 阶段内只读校验签名：`public void validateTransLock(String dsh, String tl, String expectedStep)`
  - 简化优势：统一消除 `TransitionTicket` 与 `StepAndTl` 等冗余命名，使事务锁管理器的生命周期接口在命名上达到极致对称（`createTransLock` -> `validateTransLock` -> `nextTransLock` -> `burnAndCompleteSession`）。
- **调用方与单测同步重构**：
  - `SigninServiceImpl.java` 中 10 处推进调用统一更名为 `nextTransLock(...)`，7 处校验统一更名为 `validateTransLock(...)`；
  - `SignupServiceImpl.java` 中 4 处推进调用统一更名为 `nextTransLock(...)`，6 处校验统一更名为 `validateTransLock(...)`；
  - `PasskeyServiceImpl.java`（2处）与 `RecaptchaServiceImpl.java`（2处）统一更名为 `validateTransLock(...)`；
  - `TransitionLockManagerTest.java`、`SessionServiceImplTest.java`、`SigninServiceImplTest.java` 覆盖的所有 Mock 与断言同步更新完毕。

---

## 3. 核心机制对比与深度技术剖析

### 3.1 `validateTransLock` 4 参重载与 3 参重载的区别剖析

在 `TransitionLockManager` 中，设计了两个重载的 `validateTransLock` 方法以应对不同的业务交互粒度：

| 维度 | `validateTransLock(dsh, tl, cid, step)` (原 validateTransitionTicket) | `validateTransLock(dsh, tl, step)` (原 validateStepAndTl) |
| :--- | :--- | :--- |
| **方法入参** | `(dsh, tl, cid, expectedStep)` | `(dsh, tl, expectedStep)` |
| **cid 序号校验** | **强制强一致校验**：`!Objects.equals(flow.getCid(), cid)` 立即阻断并抛出 FORBIDDEN | **不校验 cid**：允许在当前步骤内发起多次只读或挑战请求 |
| **步骤校验策略** | **全流程无条件严格校验**：`!Objects.equals(flow.getCurrentStep(), expectedStep)` 即阻断 | **条件校验**：仅在 `VLIF_WEB_LOGIN` 且声明 `expectedStep` 时核对步骤 |
| **设计目的** | **状态机单向原子推进（Step Transitioning）**：用于客户端提交完成某个阶段数据时的门禁核验 | **步骤内辅助子操作（In-step Auxiliary Challenge）**：用于阶段内的凭证获取、挑战生成或模态弹窗校验 |
| **典型应用场景** | 1. 提交密码完成校验并推入下一阶段；<br>2. 提交邮箱验证码推进至密码设置；<br>3. 终局认证核销 (`burnAndCompleteSession`)。 | 1. `RecaptchaServiceImpl.getChallenge`（获取当前人机验证挑战参数）；<br>2. `PasskeyServiceImpl.getAssertionOptions`（获取 WebAuthn 协商参数）；<br>3. 步骤内多次错误重试。 |

**核心设计哲学**：
- 4 参 `validateTransLock` 构成了严格的**单向步进锁**。一旦客户端意图改变状态机状态（前进、核销），必须出示严格匹配的 `(activeTL, cid, currentStep)` 三元组，防止重放（Replay）、跳步（Step Skipping）以及并发竞态。
- 3 参 `validateTransLock` 构成了**步骤内会话租约核验器**。当客户端处于某一阶段（如 `recaptcha` 挑战中）需要多次拉取题目或校验单项子凭证时，无需递增 `cid`，仅确保客户端持有合法且属于当前步骤的 `activeTL` 即可。

### 3.2 `burnAndCompleteSession` 的作用与安全意义

`burnAndCompleteSession` 定义如下：
```java
public void burnAndCompleteSession(String dsh, String currentTl, int currentCid, String expectedStep) {
    validateTransLock(dsh, currentTl, currentCid, expectedStep);
    sessionManager.destroySession(dsh);
    log.info("[TransitionLockManager] 流程认证完成，已销毁会话与事务锁: dsh={}", dsh);
}
```

其在系统架构中具备以下核心作用：
1. **终局门禁原子核验 (Terminal Gatekeeping)**：在向客户端下发长期凭证（如无状态双 JWT `access_token` / `refresh_token`）之前，强制对终局步骤（如 `authenticate` 或 `pt/options`）做最后一次严格的 `TL + cid + step` 验真，确保非法的后门注入或跳过最后一步无法成真。
2. **彻底作废一次性锁与防重放 (One-Time-Pad Enforcement & Anti-Replay)**：由于 `activeTL` 绑定在 Redis 中的临时 Session 内，直接销毁 `dsh` 对应的会话将使该 `TL` 在全平台范围内彻底失效。攻击者即便在传输层截获了最后的 `TL`，也绝无法发起二次请求重复签发 Token。
3. **临时认证生命周期与已认证会话的物理隔离 (Lifecycle Boundary)**：多步认证流（`Session` + `Flow` + `TransitionLock`）属于**临时握手生命周期**，而签发后的 JWT 属于**长期访问生命周期**。认证一旦达成，临时会话必须被“物理焚毁（Burn）”，实现两个生命周期的清晰解耦与闭环。
4. **即时资源释放 (Immediate Redis Resource Cleanup)**：避免依赖 Redis 的 TTL 自然过期产生内存驻留与残留脏会话，高并发场景下能极大地降低 Redis 内存压力。

---

## 4. 验证与测试结果

在平台多模块环境下执行 Maven 测试套件验证：
1. `volur-security` 模块全量执行：73 个用例全部通过（`BUILD SUCCESS`，0 Failures，0 Errors）；
   - `FlowNamesTest`：11 个枚举多场景映射与校验用例 100% 通过；
   - `TransitionLockManagerTest`：重构后的 `createTransLock`、`nextTransLock`、重载 `validateTransLock`、跳步防护与 `burnAndCompleteSession` 全部通过；
   - `SessionServiceImplTest`：会话初始化与强校验全部通过。
2. `volur-auth` 模块全量执行：58 个用例全部通过（`BUILD SUCCESS`，0 Failures，0 Errors）；
   - `SignInServiceImplTest`：包括人机自适应降级、ETOTP 邮箱挑战、密码验证与最终 `burnAndCompleteSession` 签发 Token 全部验证通过。
