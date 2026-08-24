# 施工记录：volur-auth 与 volur-verification 深度交叉验证与两阶段 Ticket 机制完整打通

## 1. 基本信息

- **关联模块**：`volur-auth`, `volur-verification`
- **主要修改文件**：
  - [`AuthenticateRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/AuthenticateRequestDTO.java)
  - [`RegisterRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/RegisterRequestDTO.java)
  - [`ForgotPasswordRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/ForgotPasswordRequestDTO.java)
  - [`AuthService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/AuthService.java)
  - [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)
  - [`AuthController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/AuthController.java)
- **执行时间**：2026-08-24
- **规范依据**：[`volur-design-skills/Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/volur-design-skills/Rules.md) 及 [`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md)

---

## 2. 交叉验证发现的隐患与对齐清单

在对 `volur-auth` 与新升级的 `volur-verification` 进行端到端交叉链路审查时，识别并修复了以下核心衔接断点：

### 2.1 登录与注册支持两阶段 Ticket 与行为滑块
- **原隐患**：`AuthenticateRequestDTO` 和 `RegisterRequestDTO` 仅包含 `captchaId` 与 `captchaCode`。若前端采用行为滑块（Slide Puzzle）通过 `/api/v1/captcha/verify-ticket` 完成核验后，无法将获取的 `captchaTicket` 提交给登录或注册接口；
- **重构落实**：
  - 在 `AuthenticateRequestDTO`、`RegisterRequestDTO`、`ForgotPasswordRequestDTO` 中新增 `private String captchaTicket;`；
  - 在 `AuthServiceImpl` 中抽象统一的 `checkCaptchaIfEnabled(captchaId, captchaCode, captchaTicket, scene)`；
  - 支持 **Ticket 优先验证** 与 **传统图形码按场景验证** 双模态兼容，同时绑定 `CaptchaScene.LOGIN`、`CaptchaScene.REGISTER` 与 `CaptchaScene.RESET_PASSWORD` 场景隔离。

### 2.2 找回密码 Token Redis Key 前缀不一致修复
- **原隐患**：`volur-verification` 中的 `EmailServiceImpl.sendResetPasswordEmail` 存入的 Redis Key 为 `vredis:verification:password_reset:{token}`；而 `volur-auth` 中的 `AuthServiceImpl.resetPasswordByToken` 读取的是 `vredis:auth:password_reset:{token}`。导致用户点击邮件重置链接时无法读取 Token；
- **重构落实**：将 `AuthServiceImpl` 中的常量统一对齐为 `VREDIS_AUTH_PASSWORD_RESET_KEY_PREFIX = "vredis:verification:password_reset:"`，彻底消除链路断点。

### 2.3 邮件发送统一委托与 Thin Service 原则
- **原隐患**：`AuthServiceImpl` 中的 `sendVerificationEmail` 和 `sendChangePasswordEmail` 手动进行了验证码检查，随后调用重载的底层 `emailService.sendVerificationEmail(email, username)`，绕过了 `volur-verification` 中封装的 DTO 统一风控、黑白名单、临时邮箱过滤与单日配额；
- **重构落实**：直接委托给 `emailService.sendVerificationEmail(request)` 与 `emailService.sendChangePasswordEmail(request)`，实现单一真实源（Single Source of Truth）。

---

## 3. 验证结果

- **编译构建**：
  ```bash
  mvn clean compile -DskipTests
  ```
  10 个子模块 100% 编译成功 (BUILD SUCCESS)。
- **测试回归**：
  ```bash
  mvn test
  ```
  全量单元测试 100% 通过。
