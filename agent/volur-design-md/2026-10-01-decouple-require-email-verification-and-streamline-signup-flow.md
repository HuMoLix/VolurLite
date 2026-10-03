# 施工与修改记录：移除注册响应 requireEmailVerification 冗余字段并精简流转状态机

- **实施日期**：2026-10-01
- **涉及模块**：
  - 后端工程：`volur-auth`（`SignUpFlowResponseVO.java`, `SignupServiceImpl.java`）
  - 前端工程：`volur-lite-view-NEXT`（`flow.ts` types, `signupFlow.ts`, `NameStepView.vue`, `PasswordStepView.vue`）
- **核心架构**：基于 `nextStep` 驱动的极简单向流转状态机、注册连续性设计与 `TransitionLock` 防跳步验证

---

## 1. 变更背景与架构决策

在原有的多步骤注册方案中，后端在 `SignupServiceImpl.java` 的 `name(...)` 响应中显式返回了 `.requireEmailVerification(requireEmailVerification)` 布尔字段，并在前端 `signupFlowStore` 中进行了全局状态维护。

经架构重构审计，认定该字段在协议层属于冗余传输与不必要的耦合：
1. **状态机前向推进的自解释性**：
   - 服务端根据系统动态配置 `system.auth.require_email_verification` 已在内部完成流转决策；
   - 服务端在签发 `TransitionLock` 票据时已在 `nextStep` 中锁定了目标状态（开启邮件返回 `email`，关闭邮件返回 `password`）；
   - 前端无需感知整体业务管道配置，只需严格遵循 `nextStep` 推动路由：`targetPath = '/v1/signup/steps/' + nextStep`，实现真正的薄前端与轻量状态机。
2. **注册交互的单向连续性（去除逆向回退）**：
   - 账号注册是具备强上下文与原子事务性的连贯流程，用户在完成用户名（及邮箱核验）进入设置密码阶段后，若要放弃或切换账号，应当重新开启或改用登录；
   - 移除 `PasswordStepView.vue` 中依赖 `requireEmailVerification` 判定回退目标的“返回”按钮，使注册操作更为紧凑连贯，彻底消除对回退拓扑感知字段的依赖。
3. **安全防跳步的完整性不受任何影响**：
   - 无论前端是否感知 `requireEmailVerification`，后端 `TransitionLockManager` 均在签发每一阶段 `TL` 票据时与预期步骤强绑定；
   - 用户无法伪造或跳过任何受保护步骤，安全性由后端状态锁（`TransitionLock`）闭环保障。

---

## 2. 详细改造与工程实现

### 2.1 后端工程改造 (`volur-auth`)
1. **领域响应模型 (`SignUpFlowResponseVO.java`)**：
   - 彻底删除 `private Boolean requireEmailVerification;` 属性定义。
2. **服务实现层 (`SignupServiceImpl.java`)**：
   - 保留内部读取系统配置用于决定 `nextStep`（`"email"` 或 `"password"`）及 `password(...)` 方法中的邮箱核验校验逻辑；
   - 在 `name(...)`、`email(...)` 及 `verifyEtotp(...)` 构建 `SignUpFlowResponseVO` 时，彻底移除 `.requireEmailVerification(...)` 构建项。

### 2.2 前端工程改造 (`volur-lite-view-NEXT`)
1. **类型定义 (`src/types/auth/flow.ts`)**：
   - `SignUpFlowResponseVO` 接口中移除 `requireEmailVerification?: boolean` 字段。
2. **状态管理 (`src/store/modules/flow/signupFlow.ts`)**：
   - 移除 `const requireEmailVerification = ref<boolean>(true)` 响应式状态；
   - `submitSignUpName` 方法返回签名精简为 `Promise<{ nextStep: string; status?: string }>`，去除对 `data.requireEmailVerification` 的读取与赋值；
   - Store 导出对象中剔除 `requireEmailVerification`。
3. **步骤视图优化与快速失败（Fail-Fast）**：
   - **`NameStepView.vue`**：严格检查 `res.nextStep` 是否存在，若缺失直接抛出异常 `throw new Error('服务端未返回下一步认证流程')`，绝不使用 `res.nextStep || 'email'` 假值，随后推向 `/v1/signup/steps/${res.nextStep}`；
   - **`PasswordStepView.vue`**：
     - 移除底部的“返回”按钮（`.btn-secondary-action`），保留右对齐紧凑单一主按钮“完成注册”；
     - 移除对应的 `goBack()` 函数与冗余的 `.btn-secondary-action` SCSS 样式。
4. **全平台流转假值清理**：
   - 全面清理 `signupFlow.ts`、`signinFlow.ts`、`deviceChallengeView.vue`、`recaptchaChallengeView.vue` 中所有类似 `nextStep || 'pwd'`、`nextStep || 'email'` 等假值兜底反模式，改为缺失即显式抛错中断。

---

## 3. 规范落地与 GEMINI.md / Rules.md 强化

在根规范 [`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md) 及后端详细规范 [`Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md) 中已新增专门的约束性条款：
1. **GEMINI.md § 1.1 条款 10（后端 Fail-Fast & Zero-Mock 原则）**：对于未配置的基础设施或业务依赖（如 SMTP 邮件服务、人机验证密钥、短信网关等），没配置就是没配置，严禁私自 Mock 假数据或假装发送成功，必须直接抛出强类型业务异常快速失败，确保测试与联调期能够第一时间暴露出缺失的配置与底层缺陷。
2. **GEMINI.md § 1.3 条款 5（前端 Fail-Fast Navigation Rule）**：前端路由推进必须严格由服务端响应的真实业务字段驱动，严禁在代码中对未返回的关键流程字段进行前端伪造或隐式 Mock 兜底；缺失时必须直接抛错中断，确保联调与测试期能够第一时间暴露后端接口或时序问题。
3. **GEMINI.md § 3 条款 9（全端通用 Fast-Fail 铁律）**：全端杜绝隐式 Mock 与伪造兜底，严禁伪造假数据或静默吞异常。
4. **Rules.md § 17（全链路快速失败与严禁隐式 Mock 铁律）**：后端开发基准中全面确立未配置即中断的快速失败与零 Mock 铁律。

---

## 4. 验证与审计结论

1. **后端 Maven 编译验证**：
   - 执行 `mvn test-compile -pl volur-auth -am -DskipTests`，`BUILD SUCCESS`，代码干净无错误。
2. **前端 Vite 编译验证**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），0 类型错误，产物顺利构建。
3. **零注释铁律遵守**：
   - 所有变更代码文件杜绝冗余行内备注或解释性注释，全部技术方案与推导过程收敛至本文档中。
