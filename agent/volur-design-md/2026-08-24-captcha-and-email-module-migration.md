# 施工记录：图形验证码与邮件验证码功能独立拆分至 volur-captcha 模块

## 1. 基本信息

- **新增模块**：`volur-captcha`
- **关联模块**：`volur-lite-platform` (父POM), `volur-auth`, `volur-server`
- **新增文件**：
  - [`volur-captcha/pom.xml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/pom.xml)
  - [`CaptchaController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/controller/v1/CaptchaController.java)
  - [`EmailController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/controller/v1/EmailController.java)
  - [`CaptchaService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/service/CaptchaService.java)
  - [`CaptchaServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/service/impl/CaptchaServiceImpl.java)
  - [`EmailService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/service/EmailService.java)
  - [`EmailServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/service/impl/EmailServiceImpl.java)
  - [`MailTaskExecutor.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/executor/MailTaskExecutor.java)
  - [`EmailCaptchaRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/domain/dto/EmailCaptchaRequestDTO.java)
  - [`VerifyChangePasswordCodeRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/domain/dto/VerifyChangePasswordCodeRequestDTO.java)
  - [`TestSmtpRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/domain/dto/TestSmtpRequestDTO.java)
  - [`CaptchaResponseVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/domain/vo/CaptchaResponseVO.java)
  - 邮件 HTML 模板（`verification_email.html`、`change_password_email.html`、`reset_password_email.html`、`smtp_test_email.html`）
- **修改文件**：
  - [`pom.xml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/pom.xml)
  - [`volur-auth/pom.xml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/pom.xml)
  - [`volur-server/pom.xml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-server/pom.xml)
  - [`AuthService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/AuthService.java)
  - [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)
  - [`AuthController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/AuthController.java)
  - [`SecurityConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java)
- **清理文件**：
  - 清理了 `volur-auth` 中原有的重复 `CaptchaService`、`CaptchaServiceImpl`、`EmailService`、`EmailServiceImpl` 及相关 DTO。
- **执行时间**：2026-08-24
- **规范依据**：[`volur-design-skills/Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/volur-design-skills/Rules.md) 及 [`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md)

---

## 2. 变更背景与架构解耦设计

在 Volur Lite 单体模块化架构演进中，图形验证码（LineCaptcha / Rate Limit）与邮件服务（SMTP / Async Mail Sender / HTML Template）属于通用的安全验证基础设施组件。原项目中散落在 `volur-auth` 中，导致业务认证逻辑与通用验证基础设施强耦合。

本次变更将图形验证码和邮件验证码完整独立为 `volur-captcha` 模块，并从 Volur 原项目（`com.humolix.volurctf.module.captcha` 与 `com.humolix.volurctf.module.email`）中合并迁移完善的控制器、限频防刷、异步发送与多套 HTML 邮件模板。

### 2.1 依赖关系与防双重/循环引用校验 (Acyclic DAG)

为了彻底避免模块间双向依赖或循环依赖，模块依赖严格按照单向有向无环图（DAG）构建：

```
volur-framework (基础异常、RequestHolder、API 响应封装)
      ▲
volur-system (系统配置 SystemSettingService、SMTP 参数读取)
      ▲
volur-user (用户仓储 UserRepository 存在性校验)
      ▲
volur-captcha (图形验证码与邮件验证服务及独立 Controller)
      ▲
volur-auth (认证、会话、登录、注册与权限拦截)
      ▲
volur-server (单体聚合主服务)
```

- `volur-captcha` 仅依赖 `volur-framework`、`volur-system`、`volur-user`，**绝不反向依赖 `volur-auth`**；
- `volur-auth` 依赖 `volur-captcha` 并复用其 `CaptchaService` 与 `EmailService` 接口；
- 双向引用校验结论：**完全单向解耦，无任何循环依赖（0 Circular Dependency）**。

---

## 3. 核心功能与规范实现详情

### 3.1 图形验证码服务与接口 (`volur-captcha`)

1. **接口**：[`CaptchaController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/controller/v1/CaptchaController.java)
   - 路由：`GET /api/v1/captcha?oldCaptchaId=...`
   - 响应：返回 `ApiResponse<CaptchaResponseVO>`，包含 `captchaId` 与 Base64 格式图片。
2. **逻辑与安全加固**：
   - **IP 级限频防刷**：基于客户端真实 IP（`RequestHolder.getRemoteIp()`），每分钟限制最多 15 次请求，超出抛出 `TOO_MANY_REQUESTS`。
   - **旧验证码自动清理**：传入合法 `oldCaptchaId` 时即时从 Redis 清理旧 Key，避免 Redis 内存膨胀。
   - **一次性校验机制**：校验通过或读取后立即 `redisTemplate.delete(key)`，杜绝重放攻击。
   - **Redis 键规范**：`VREDIS_CAPTCHA_IMAGE_KEY_PREFIX = "vredis:captcha:image:"`，`VREDIS_CAPTCHA_RATE_LIMIT_KEY_PREFIX = "vredis:captcha:rate_limit:"`，过期时间统一采用 `Duration.ofMinutes(2)`。

### 3.2 邮件验证码服务与接口 (`volur-captcha`)

1. **接口**：[`EmailController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/controller/v1/EmailController.java)（严格遵循 Thin Controller 原则，仅注入 `EmailService`）
   - `POST /api/v1/email/captcha/register`：注册验证码发送（直接透传 `EmailCaptchaRequestDTO` 至 Service 层）。
   - `POST /api/v1/email/captcha/change-password`：修改密码验证码发送（直接透传 `EmailCaptchaRequestDTO` 至 Service 层）。
   - `POST /api/v1/email/captcha/verify`：校验修改密码验证码，返回校验令牌 Token。
   - `POST /api/v1/email/test`：测试 SMTP 连接连通性。
2. **逻辑与安全加固（收敛于 Service 层）**：
   - **Thin Controller 原则重构**：将系统配置判断（`ENABLE_CAPTCHA`）与图形验证码核验（`captchaService.verifyCaptcha`）完全下沉至 [`EmailServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-captcha/src/main/java/com/humolix/volur/captcha/service/impl/EmailServiceImpl.java) 内部，Controller 层彻底移除对 `CaptchaService` 和 `SystemSettingService` 的横向注入，防止内部调用时安全策略被绕过。
   - **60 秒发送冷却**：每邮箱 60s 内仅允许触发一次发送（`vredis:captcha:email_send_limit:`），有效防范邮件轰炸。
   - **动态 SMTP 客户端与配置读取**：从 `SystemSettingService` 动态加载 `SMTP_HOST`、`SMTP_PORT`、`SMTP_USERNAME`、`SMTP_PASSWORD`、`SMTP_FROM`，支持 465 SSL 与 587 STARTTLS。
   - **并发受控的异步发送与降级机制**：通过 `MailTaskExecutor` 使用 `Semaphore(50)` 限制并发邮件发送量，避免线程与连接耗尽；支持 HTML 模板构建异常时的纯文本安全降级。
    - **多套精美 HTML 模板预加载**：应用启动时 `@PostConstruct` 预加载 `verification_email`、`change_password_email`、`reset_password_email`、`smtp_test_email` 到并发缓存。
    - **方法返回值与布尔类型规范化**：
      - 执行类方法（如 `sendVerificationEmail`、`sendChangePasswordEmail`）返回值统一设计为 `void`（失败通过抛出 `BusinessException` 中断，成功正常返回），杜绝无意义的布尔返回值；
      - 校验类方法（如 `verifyCode`、`verifyCaptcha`）返回值统一采用基本数据类型 `boolean`，杜绝 `Boolean` 包装类自动拆箱导致的潜在空指针风险（NPE 防护）。

### 3.3 规范合规性自检对照

| 规范项目 | 规则要求 | 实现落实情况 |
| :--- | :--- | :--- |
| **依赖注入** | 严禁 `@Autowired`，强制 `private final` + `@RequiredArgsConstructor` | 所有 Controller、Service、Executor 严格遵守 |
| **Thin Controller 原则** | 控制器严禁承载业务逻辑与策略核验，必须收敛至 Service 层 | `EmailController` 仅注入 `EmailService`，配置与验证码核验下沉至 Service |
| **布尔与返回值设计** | 校验返回 `boolean`，执行操作返回 `void`，杜绝 `Boolean` 拆箱 NPE | `EmailService` 接口与实现方法全面重构落实 |
| **模型分层** | 收敛于 `domain.dto` 和 `domain.vo` | DTO 使用 `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`<br>VO 使用 `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` |
| **控制器版本化** | `com.humolix.volur.<module>.controller.v1` 及 `/api/v1/*` | 路由统一声明为 `/api/v1/captcha` 与 `/api/v1/email` |
| **Redis 命名与操作** | 前缀 `VREDIS_` + 模块 + 功能 + `KEY_PREFIX`，值为小写多级冒号 | 所有常量遵循规范，过期时间全部使用 `java.time.Duration`，0 弃用 API 调用 |
| **安全过滤放行** | 公开接口放行 | [`SecurityConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java) 正确放行 `/api/v1/captcha/**` 与 `/api/v1/email/captcha/**` |

---

## 4. 验证结果

执行 Maven 全量编译构建：
```bash
mvn clean compile -DskipTests
```
- **Reactor 结果**：全部 10 个子模块（`volur-lite-platform`, `volur-framework`, `volur-system`, `volur-user`, `volur-captcha`, `volur-auth`, `volur-challenge`, `volur-container`, `volur-scoreboard`, `volur-server`）**100% 编译成功（BUILD SUCCESS）**。

---

## 5. 影响范围

- 前端可直接调用 `/api/v1/captcha` 获取图形验证码，调用 `/api/v1/email/captcha/*` 进行邮箱验证；
- `volur-auth` 中的注册、登录、找回密码与重置密码功能通过注入 `volur-captcha` 的服务接口无缝协同；
- 极大提升了架构清晰度，彻底消除循环依赖，增强了系统的抗攻击与容灾能力。
