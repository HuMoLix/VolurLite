# 施工记录：volur-captcha 全面升级为 volur-verification 安全验证与风控中台

## 1. 基本信息

- **重构模块**：`volur-captcha` -> `volur-verification`
- **关联模块**：`volur-lite-platform` (父POM), `volur-framework`, `volur-system`, `volur-user`, `volur-auth`, `volur-server`
- **新增模块路径**：[`src/volur-lite-platform/volur-verification`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification)
- **清理模块路径**：`src/volur-lite-platform/volur-captcha`（已安全移除）
- **执行时间**：2026-08-24
- **规范依据**：[`volur-design-skills/Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/volur-design-skills/Rules.md) 及 [`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md)

---

## 2. 变更背景与架构全景

为了解决原有系统图形验证码形态单一、易受 OCR 破解、缺乏场景隔离、邮件缺乏多维度防轰炸与单点 SMTP 发信无容灾切换等瓶颈，本次重构将 `volur-captcha` 升格为 `volur-verification`，一站式实现了从阶段一到阶段三的企业级安全验证中台架构。

### 2.1 依赖关系拓扑 (Acyclic Dependency DAG)

```
volur-framework (通用异常与响应包装)
      ▲
volur-system (系统配置动态加载)
      ▲
volur-user (用户邮箱查重)
      ▲
volur-verification (多模态人机验证、自适应风控引擎、多通道邮件容灾服务)
      ▲
volur-auth (认证、会话与密码重置)
      ▲
volur-server (单体聚合启动主服务)
```

- 严格保持单向依赖，杜绝双向与循环引用。
- 全部类遵循 `private final` + `@RequiredArgsConstructor` 不可变构造注入，禁止字段注入与手动构造器编写。

---

## 3. 核心设计与实现亮点

### 3.1 策略模式与多模态人机验证体系 (`com.humolix.volur.verification.captcha`)
1. **统一策略接口 [`CaptchaProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/CaptchaProvider.java)**：
   - `LINE` ([`LineCaptchaProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/LineCaptchaProvider.java))：传统线段干扰字符；
   - `MATH` ([`MathCaptchaProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/MathCaptchaProvider.java))：算术加减乘口算运算式；
   - `SHEAR` ([`ShearCaptchaProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/ShearCaptchaProvider.java))：高扭曲抗 OCR 字符验证码；
   - `SLIDE_PUZZLE` ([`SlidePuzzleCaptchaProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/SlidePuzzleCaptchaProvider.java))：基于 [`SlidePuzzleEngine`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/engine/SlidePuzzleEngine.java) 内存纯算法生成的背景图 + 滑块凸凹缺口切片，验证 $\pm 6\text{px}$ 容差位移；
   - `CLOUD_TURNSTILE` ([`CloudTurnstileCaptchaProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/CloudTurnstileCaptchaProvider.java))：Cloudflare Turnstile 适配器。
2. **策略管理器 [`CaptchaProviderManager`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/manager/CaptchaProviderManager.java)**：统一集中管理与路由分发。

### 3.2 自适应动态风控与多维限频体系 (`com.humolix.volur.verification.risk`)
1. **严格遵循系统开关优先准则**：
   - 默认状态下验证码开启与否完全取决于系统配置（`ENABLE_CAPTCHA`）；
   - 风控引擎 ([`RiskEvaluationEngine`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/engine/RiskEvaluationEngine.java)) 仅在验证码开启时，根据客户端 IP 的请求频次、10 分钟内失败惩罚历史评估风险等级（`LOW`, `MEDIUM`, `HIGH`），自适应提升验证码呈现难度（字符 -> 扭曲 -> 滑块拼图）。
2. **精准滑动窗口限频器 [`RedisSlidingWindowRateLimiter`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/limiter/RedisSlidingWindowRateLimiter.java)**：
   - 基于 Redis Sorted Set (`ZSet`) 实现精准滑动时间窗口，彻底解决固定窗口临界突发跳变漏洞。

### 3.3 API 演进与两阶段 Ticket 机制
1. **控制器演进 [`CaptchaController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/controller/v1/CaptchaController.java)**：
   - `GET /api/v1/captcha/challenge`：支持场景参数 `scene`（`LOGIN`, `REGISTER`, `CHANGE_PASSWORD`, `RESET_PASSWORD`）与类型参数 `type`；
   - `GET /api/v1/captcha`：平滑兼容旧版客户端调用；
   - `POST /api/v1/captcha/verify-ticket`：行为验证码或前置核验通过后，签发一次性短期票据 `captchaTicket`（有效期 5 分钟），主业务接口仅需校验该 Ticket。
2. **场景隔离与防跨接口重放**：
   - Redis Key 升级为 `vredis:verification:captcha:image:{scene}:{captchaId}` 及 `vredis:verification:captcha:ticket:{scene}:{ticket}`，防止注册验证码被盗用于登录爆破。

### 3.4 全方位多层次防轰炸与多通道容灾发信体系 (`com.humolix.volur.verification.mail`)
1. **四重防邮件轰炸体系 ([`EmailServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java))**：
   - **前置人机核验**：当 `ENABLE_CAPTCHA` 开启时，发信前强校验图形验证码或验证票据；
   - **IP 级滑动窗口限流**：单 IP 每小时最多发送 10 封，24 小时最多 30 封；
   - **邮箱单日限额 (Daily Cap)**：单邮箱 24 小时最多接收 5 封；
   - **单次 60 秒冷却**：单邮箱 60s 内仅能触发 1 次；
   - **合规性过滤**：黑名单拦截常见临时一次性邮箱（`tempmail.com`, `mailinator.com` 等），白名单过滤允许注册的邮箱后缀。
2. **多发信渠道与自动故障转移 ([`FailoverMailDispatcher`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/dispatcher/FailoverMailDispatcher.java))**：
   - [`SmtpMailDeliveryProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/provider/SmtpMailDeliveryProvider.java)：动态 SMTP 连接池；
   - [`AliyunDirectMailDeliveryProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/provider/AliyunDirectMailDeliveryProvider.java)：阿里云云端发信；
   - [`LoggingFallbackMailDeliveryProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/provider/LoggingFallbackMailDeliveryProvider.java)：本地测试日志兜底降级通道。
3. **受控并发与指数退避重试 ([`MailTaskExecutor`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/executor/MailTaskExecutor.java))**：
   - `Semaphore(50)` 控制最大异步并发；
   - 遭遇网络抖动时按指数退避策略（1s, 2s, 4s）自动重试最多 3 次。

---

## 4. 验证结果

- 执行全量 Reactor 编译构建：
  ```bash
  mvn clean compile -DskipTests
  ```
  **10 个子模块 100% 编译成功 (BUILD SUCCESS)**。
- 执行自动化单元测试：
  ```bash
  mvn test
  ```
  **`SlidePuzzleEngineTest` 测试 100% 通过**，背景与滑块图像切片生成无异常。

---

## 5. 影响范围

- **前端调用**：
  - 可继续无缝调用 `/api/v1/captcha` 或升级调用 `/api/v1/captcha/challenge`；
  - 交互式滑块可通过 `/api/v1/captcha/verify-ticket` 换取 `captchaTicket` 后提交注册/修改密码；
- **认证模块**：
  - `volur-auth` 平滑迁移至 `volur-verification` 依赖，业务注册、登录与改密链路无缝协同。
