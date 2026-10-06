# 会话初始化端点纯粹创建语义重构落地规范

- **施工日期**：2026-10-05
- **施工模块**：`volur-security`、`volur-lite-view-NEXT`
- **相关核心文件**：
  - [`InitSessionRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/dto/InitSessionRequestDTO.java)
  - [`SessionServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/SessionServiceImpl.java)
  - [`SessionControllerTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/SessionControllerTest.java)
  - [`SessionServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/SessionServiceImplTest.java)
  - [`InitSessionRequestDTOValidationTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/domain/dto/InitSessionRequestDTOValidationTest.java)
  - [`src/volur-lite-view-NEXT/src/types/auth/flow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/types/auth/flow.ts)
  - [`src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts)
  - [`src/volur-lite-view-NEXT/src/store/modules/flow/signupFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signupFlow.ts)
- **执行规范**：严格遵循 `GEMINI.md` 之单一职责（SRP）、零注释铁律与重构彻底性铁律。

---

## 1. 变更背景与原因

在先前的架构设计中，会话初始化接口 `initSess` 试图兼容 Google GLIF 的跨流程句柄复用漫游（Session Continuation），导致该端点同时具备“创建新会话”和“恢复/重新对齐旧会话”的重叠语义：
1. **语义违和与职责重叠**：“初始化（Init）”本应是从无到有的纯粹动作，允许传递旧 `dsh` 造成了调用者和审计者的心智负担；
2. **状态机污染风险**：在零信任安全体系中，登录流程与注册流程彼此物理隔离，不应跨流程盲目复用同一个状态机会话句柄；
3. **前端实际从未走恢复分支**：前端 Store（`signinFlow.ts`、`signupFlow.ts`）在检测到 URL 存在 `dsh` 时短路跳过 `initSessionApi`，传入的 `dsh` 实质上永远为 `undefined`。

为此，实施“推荐演进方向”，彻底将 `initSess` 纯粹化为“仅创建新会话”。

---

## 2. 核心改动清单

### 2.1 后端工程重构 (`volur-security`)
1. **`InitSessionRequestDTO.java` 字段净化**：
   - 彻底移除 `dsh` 字段，入参仅保留会话建联必需的 5 个属性：`flowName`、`flowEntry`、`fp`、`t`、`data`；
2. **`SessionServiceImpl.java` 纯粹创建语义**：
   - 移除解密入参中的 `dsh`，解密时统一以 `dsh = null` 参与对称派生；
   - 移除 `recoverySession` 判定分支，直接单一调用 `transitionLockManager.createSession(...)`；
   - 初始首发 `activeTL`（步骤为 `"init"`，`cid = 0`）无条件确定性签发并持久化；
   - 清理未使用的 `StringUtils` 引用。
3. **后端单测与契约同步更新**：
   - 更新 `SessionControllerTest`、`SessionServiceImplTest` 以及 `InitSessionRequestDTOValidationTest` 中的 DTO 构造与 mock 行为，单测全量通过。

### 2.2 前端工程重构 (`volur-lite-view-NEXT`)
1. **`types/auth/flow.ts` 契约对齐**：
   - 从 `SessionInitParams` 接口中删除 `dsh?: string` 字段；
2. **Store 流程接入清理 (`signinFlow.ts` & `signupFlow.ts`)**：
   - 移除 `targetDsh` 变量与向 `initSessionApi` 传递 `dsh` 的参数；
   - `encryptFlowCredential` 规范化为无 `dsh` 的首建调用。

---

## 3. 验证结果

- **后端单元测试**：
  - `volur-security`：37 项单元测试 100% 通过（`BUILD SUCCESS`）；
  - `volur-auth`：54 项单元测试 100% 通过（`BUILD SUCCESS`）。
- **前端强类型检查与生产编译**：
  - `volur-lite-view-NEXT` 执行 `vue-tsc --noEmit && vite build`，2859 个模块编译成功，0 错误，0 告警。
