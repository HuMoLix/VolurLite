# SigninFlow 领域模型继承与 waitingSteps 状态机管理架构

## 1. 变更背景与架构动机

在认证状态机推进过程中，除了当前执行的步骤（`currentStep`）与已完成的历史步骤（`completedSteps`）外，服务端需要对该认证流预期的后续待完成步骤（`waitingSteps`）进行显式编排与动态跟踪：
1. **步骤预期显式化**：客户端和业务层需明确知道当前会话后续还需要依次完成哪些挑战（如密码认证 `pwd`、两步验证 `2fa`、人机验证 `recaptcha`、最终登入完成 `authenticate`）；
2. **领域模型强类型继承**：登录工作流（`VlifWebLogin`）具有其特定的状态推进模型，通过具体的领域类 `SigninFlow` 继承通用 `Flow` 基类，预置默认的等待步骤队列 `["pwd", "authenticate"]`；
3. **动态挑战编排**：当检测到风险环境或认证失败次数触发安全阈值时，能够将 `recaptcha` 或 `2fa` 动态优先插入等待队列首部（`waitingSteps`），并在各步骤通过时平滑核销与推进。

---

## 2. 核心架构设计

### 2.1 继承层次与模型设计

```
Flow (基类: 通用认证流程状态机模型)
  ├── flowName, flowEntry, identifier, currentStep, cid, activeTL
  ├── completedSteps (已完成步骤列表)
  └── waitingSteps (待完成步骤队列)
        │
        ▲ (extends)
        │
SigninFlow (登录流程专用领域实体)
  ├── 默认 flowName: "VlifWebLogin"
  ├── 默认 flowEntry: "WebServiceLogin"
  └── 预设 waitingSteps: ["pwd", "authenticate"]
```

#### 关键 API 与扩展：
1. **`Flow.java`**：
   - 采用 Lombok `@SuperBuilder` 配合 `@NoArgsConstructor`、`@AllArgsConstructor` 支持继承；
   - 增加 `addWaitingStep(step)`：安全去重追加待办步骤；
   - 增加 `addWaitingStepFirst(step)`：高优先级插入待办队列首部（如触发人机验证或 2FA）；
   - 增加 `completeStep(step)`：原子级从 `waitingSteps` 移除，并记录至 `completedSteps`。
2. **`SigninFlow.java`**：
   - 继承自 `Flow`，声明常量：`STEP_PWD` ("pwd")、`STEP_AUTHENTICATE` ("authenticate")、`STEP_RECAPTCHA` ("recaptcha")、`STEP_2FA` ("2fa")、`STEP_DEVICE` ("device")；
   - 提供构造方法、静态工厂 `create()` 与跨模型安全适配工厂 `from(Flow)`。

---

## 3. 全链路状态机集成

### 3.1 会话管理器与流程推进联动
1. **`SessionManager.java`**：
   - `createSession` 与 `recoverySession` 内部：当解析到流程为 `FlowNames.VLIF_WEB_LOGIN` 时，默认使用 `SigninFlow.create()` 实例化并注入会话；
   - 初始化即具备 `waitingSteps = ["pwd", "authenticate"]`。
2. **`TransitionLockManager.java`**：
   - `advanceTransitionTicket` 在核销旧 TL 时，调用 `flow.completeStep(completedStep)`，确保步骤核销与 `waitingSteps` 队列削减的一致性。

### 3.2 登录业务层（`SigninServiceImpl.java`）
1. **账号校验阶段 (`identifier`)**：
   - 通过 `SigninFlow.from(session.getFlow())` 绑定强类型流程；
   - 若环境可疑或失败超限，调用 `flow.addWaitingStepFirst(SigninFlow.STEP_RECAPTCHA)` 将 `recaptcha` 插入待办队首（队列变为 `["recaptcha", "pwd", "authenticate"]`）；
   - 若安全检查正常，队列维持 `["pwd", "authenticate"]`。
2. **挑战处理阶段 (`challenge`)**：
   - `recaptcha` 挑战通过后，进入密码校验，`recaptcha` 自动从 `waitingSteps` 核销；
   - `pwd` 挑战通过后：
     - 若触发 2FA，将 `2fa` 动态插入队首（队列变为 `["2fa", "authenticate"]`）；
     - 若无需 2FA，核销 `authenticate`，完成会话终结并颁发令牌；
   - `2fa` 挑战通过后，核销 `authenticate` 并完成终结。

---

## 4. 验证与审计结论

1. **单元测试全量通过**：
   - `SigninFlowTest`：覆盖默认构造初始化、静态 `create()` 构建、`from()` 转换保留上下文、`waitingSteps` 动态出入队与核销流转（5/5 测试通过）；
   - `SigninServiceImplTest`：增加对 `session.getFlow()` 实例类型（`SigninFlow`）及其 `waitingSteps` 队列在各分支下推进状态的断言（全部 7 个测试通过）；
   - `volur-security` 与 `volur-auth` 联合构建测试 100% 成功（54 个测试全绿）；
   - 全平台 13 个模块 `mvn test-compile` 0 警告 0 错误编译通过。
2. **前端构建验证**：
   - `volur-lite-view-NEXT` 执行 `npm run build` 耗时 6.22s 构建成功，端到端协议无缝兼容。
3. **代码规范遵循**：
   - 所有改动的 Java 代码严格保持零注释（文档外置原则）。
