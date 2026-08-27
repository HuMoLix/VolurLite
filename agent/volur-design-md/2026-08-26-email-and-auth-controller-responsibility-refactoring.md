# Volur Lite 邮件发信与认证控制器职责解耦及重构归档

- **执行日期**：2026-08-26
- **涉及模块**：`volur-verification`, `volur-auth`, `volur-lite-view`
- **文档类型**：架构重构、职责边界清晰化、安全加固与前后端 1:1 对齐

---

## 1. 重构背景与核心问题

在先前的实现中，[`AuthController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/AuthController.java) 与 [`EmailController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/controller/v1/EmailController.java) 存在多处职责重叠、接口定义冗余与越权安全隐患：

1. **注册发信接口冗余**：
   - `AuthController` 暴露了 `/api/v1/auth/email/verification`；
   - `EmailController` 暴露了 `/api/v1/email/captcha/register`；
   - 前端实际使用的是 `EmailController` 的路径，导致 `AuthController` 存在冗余死代码。
2. **越权安全风险与模块职责越界**：
   - `EmailController` 曾暴露 `/api/v1/email/captcha/change-password` 与 `/api/v1/email/captcha/verify`。由于 `volur-verification` 为无状态通用支撑模块，无法感知当前登录态（`SecurityUtils`），导致任何人都可以调用该接口为任意邮箱触发验证码与获取 Token；
   - 相比之下，`volur-auth` 中实现了严格的用户属主校验（`SecurityUtils.getCurrentUser()`）。
3. **DTO 归属不清晰与前端类型不一致**：
   - 修改密码核验 DTO `VerifyChangePasswordCodeRequestDTO` 错放在了 `volur-verification` 中；
   - 前端 `ChangePasswordRequestDTO` 存在字段与后端不一致的遗留问题。

---

## 2. 架构设计与职责划分

遵循 **“通用基础设施能力下沉至 verification，强业务与鉴权状态收敛至 auth”** 原则：

| 模块 | 核心定位 | 负责的路由与功能 |
| :--- | :--- | :--- |
| **`volur-verification`** | 通用验证码与底层发信基础设施（无状态） | - `POST /api/v1/email/captcha/register`：发送注册邮箱验证码（含 IP/邮箱风控限流、人机 Ticket 校验）<br>- `POST /api/v1/email/test`：SMTP 连通性测试 |
| **`volur-auth`** | 身份认证、密码生命周期与会话管理（强鉴权） | - `POST /api/v1/auth/register`：用户注册主体提交<br>- `POST /api/v1/auth/forgot-password`：找回密码申请<br>- `POST /api/v1/auth/password/send-code`：已登录用户发送改密验证码<br>- `POST /api/v1/auth/password/verify-code`：已登录用户核验改密验证码<br>- `POST /api/v1/auth/password/change`：凭借令牌修改密码 |

---

## 3. 具体代码改动清单

### 3.1 后端改动 (`volur-lite-platform`)
1. **`volur-verification` 模块**：
   - [`EmailController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/controller/v1/EmailController.java)：移除了 `captcha/change-password` 与 `captcha/verify` 接口，仅保留统一注册发信与 SMTP 探活。
   - [`EmailService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/EmailService.java) & [`EmailServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java)：移除了不属于基础设施层的 `verifyChangePasswordCode` 方法及相关 Redis 键定义。
   - 删除了 `volur-verification` 下错放的 `VerifyChangePasswordCodeRequestDTO.java`。
2. **`volur-auth` 模块**：
   - 新增 [`VerifyChangePasswordCodeRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/VerifyChangePasswordCodeRequestDTO.java) 于 `com.humolix.volur.auth.domain.dto` 包下。
   - [`AuthController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/AuthController.java)：移除了冗余的 `POST /email/verification`；将修改密码发信标准化为 `POST /password/send-code`。
   - [`AuthService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/AuthService.java) & [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)：移除冗余方法 `sendVerificationEmail`，改密核验使用 auth 专属 DTO。
   - [`SecurityConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java)：从白名单中移除了废弃的 `/api/v1/auth/email/**`，确保修改密码相关路径严格要求身份认证。

### 3.2 前端改动 (`volur-lite-view`)
1. **API 层**：
   - [`src/api/verification/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/api/verification/index.ts)：强类型化 `sendRegisterEmailCodeApi(data: EmailCaptchaRequestDTO)`，移除改密相关接口。
   - [`src/api/auth/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/api/auth/index.ts)：新增 `sendChangePasswordEmailCodeApi` 与 `verifyChangePasswordCodeApi`，与后端 Controller 路径 100% 严格 1:1 对齐。
2. **类型层**：
   - [`src/types/verification/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/types/verification/index.ts)：移除 `VerifyChangePasswordCodeRequestDTO`。
   - [`src/types/auth/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/types/auth/index.ts)：补充 `VerifyChangePasswordCodeRequestDTO`，修正 `ChangePasswordRequestDTO` 字段结构。

---

## 4. 验证与测试结果

1. **后端验证**：
   - 执行 `mvn clean test-compile` 及 `mvn test` 全部通过（5/5 单元测试通过，11 个子模块构建成功）。
2. **前端验证**：
   - 执行 `vue-tsc --noEmit && vite build` 生产构建成功，0 TypeScript 类型错误。
