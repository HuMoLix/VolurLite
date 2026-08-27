# 架构设计与实施方案：volur-captcha 升级为 volur-verification 安全验证与风控中台

## 1. 升级背景与核心目标

在现有 Volur Lite CTF 架构中，`volur-captcha` 模块承担了基础图形验证码（Hutool LineCaptcha）与 SMTP 邮件验证码发送职能。随着系统向生产级、高对抗、高可用演进，原有的单一验证码与直连 SMTP 发信架构暴露出若干瓶颈：
1. **人机对抗手段单一**：仅有固定干扰线字符验证码，易受现代深度学习 OCR 秒破，缺乏算术、滑块拼图（Slide Puzzle）及云端 Turnstile 等多模态人机验证；
2. **缺乏场景隔离**：验证码 Key 全局无差别存储，存在跨接口（如用注册场景验证码套用到登录爆破）重放风险；
3. **风控与限流维度单一**：邮件发信仅有单邮箱 60s 冷却，黑客轮询不同邮箱即可发动针对服务器 SMTP 配额的邮件轰炸；
4. **发信通道单一无容灾**：仅支持单点 SMTP，缺少多云服务商（如阿里云 DirectMail）主备自动容灾切换与可靠退避重试机制。

**核心目标**：将 `volur-captcha` 模块全面升格为 **`volur-verification`（统一安全验证与风控中台）**，一站式落实阶段一至阶段三的全部架构设计，构建集“**策略化多模态人机验证、自适应难度风控、全方位防轰炸风控体系、多通道高可用容灾发信**”于一体的企业级安全验证子系统。

---

## 2. 核心架构设计与风控规则边界

### 2.1 验证码触发与风控难度裁决边界（重要规范约束）
- **验证码启用决定权**：维持原系统设计，**默认状态下是否开启图形验证码校验完全取决于系统配置**（例如 `SystemSettingService.getBoolean("ENABLE_CAPTCHA", false/true)`），绝不将“是否启用验证码”交由风控引擎黑盒接管。
- **风控引擎核心职责**：**动态评估请求风险并裁决验证码的难度与呈现模态**。
  - **低风险（Normal）**：返回系统默认配置的基础验证码（如字符线段或口算算术）；
  - **中高风险（Elevated/High Risk，如短时间内高频失败、频繁切换账号、恶意 UA、异常请求速率）**：自适应提升验证码难度，强制升级为**高干扰字符图、复杂算术表达式或行为式滑块拼图（Slide Puzzle）**，增加自动化脚本攻击成本。

### 2.2 模块依赖与有向无环图 (Acyclic Dependency DAG)

```
volur-framework (通用异常、ApiResponse、RequestHolder)
      ▲
volur-system (SystemSettingService 系统配置与参数动态加载)
      ▲
volur-user (UserRepository 邮箱存在性核验)
      ▲
volur-verification (多模态验证码、自适应风控引擎、多通道邮件容灾服务)
      ▲
volur-auth (认证、授权、注册、登录与密码找回)
      ▲
volur-server (单体聚合启动主服务)
```

- `volur-verification` 仅依赖 `volur-framework`、`volur-system`、`volur-user`，绝不反向依赖 `volur-auth`；
- 所有 Controller、Service、Provider 严格遵守 `private final` + `@RequiredArgsConstructor` 构造注入与 Thin Controller 原则。

---

## 3. 详细子系统设计

### 3.1 策略模式与多模态人机验证生成器 (Captcha Strategy Pattern)

#### 3.1.1 核心类图与接口设计
- **`CaptchaType` 枚举**：`LINE`（字符线段）、`MATH`（算术运算）、`SHEAR`（扭曲字符）、`SLIDE_PUZZLE`（行为滑块拼图）、`CLOUD_TURNSTILE`（Cloudflare Turnstile 适配器）。
- **`CaptchaScene` 枚举**：`LOGIN`（登录）、`REGISTER`（注册）、`CHANGE_PASSWORD`（修改密码）、`RESET_PASSWORD`（找回密码）、`SENSITIVE_OP`（敏感操作）、`DEFAULT`（通用）。
- **`CaptchaProvider` 接口**：
  ```java
  public interface CaptchaProvider {
      CaptchaType getType();
      CaptchaResponseVO generate(CaptchaScene scene, RiskLevel riskLevel);
      boolean verify(String captchaId, String userInput, CaptchaScene scene);
  }
  ```
- **行为式滑块拼图生成机制 (`SlidePuzzleCaptchaProvider`)**：
  - 内存生成带有缺口的背景图与抠出的滑块图，返回 Base64 背景与滑块切片；
  - 记录缺口 X 坐标并存入 Redis（加密混淆，过期 2 分钟）；
  - 前端上报滑块最终位移 $X$ 坐标与滑动耗时/轨迹特征点，后端容差阈值校验（如 $\pm 4\text{px}$ 内判定合法）。
- **两阶段票据机制 (`verifyAndIssueTicket`)**：
  - 针对滑块验证或前置图形校验，验证通过后签发一次性票据 `captchaTicket`，主业务接口（如注册、登录）只需校验 Ticket，实现业务与人机交互彻底解耦。

### 3.2 自适应动态风控与多维限频 (Risk Engine & Sliding Window)

#### 3.2.1 风控引擎 (`RiskEvaluationEngine`)
- **`RiskLevel` 评级**：`LOW`（低风险）、`MEDIUM`（中风险）、`HIGH`（高风险）、`BLOCKED`（封禁）。
- **评级依据**：
  1. 客户端 IP 过去 10 分钟内的验证失败频次；
  2. 客户端 IP 请求速率（基于 Redis ZSet 滑动窗口）；
  3. 请求特征（User-Agent 缺失/爬虫特征）。
- **难度自适应升级映射**：
  - `LOW` -> 根据系统配置默认呈现 `MATH` 或 `LINE`；
  - `MEDIUM` -> 提升为 `SHEAR`（强扭曲抗 OCR 字符）；
  - `HIGH` -> 强制提升为 `SLIDE_PUZZLE`（行为滑块拼图）。

#### 3.2.2 Redis 滑动窗口限频器 (`RedisSlidingWindowRateLimiter`)
- 基于 Redis Sorted Set (`ZSet`) 实现精准滑动窗口，杜绝固定窗口的临界跳变漏洞：
  - `ZADD key timestamp timestamp`
  - `ZREMRANGEBYSCORE key 0 (now - windowMs)`
  - `ZCARD key` 判断是否超出阈值。

### 3.3 全方位多层次防轰炸与安全风控体系 (Email Multi-tier Anti-Spam)

防邮件轰炸体系采取四重阶梯式纵深防御：
```
       [ 客户端请求发送邮件 ]
                 │
                 ▼
 ┌───────────────────────────────┐
 │ 1. 前置人机验证 (Captcha Check) │ -> 当系统 ENABLE_CAPTCHA 开启时，强校验图形码或 Ticket
 └───────────────┬───────────────┘
                 │ (通过)
                 ▼
 ┌───────────────────────────────┐
 │ 2. IP 级滑动窗口频次拦截        │ -> 单 IP 每小时 ≤ 10 封, 每天 ≤ 30 封
 └───────────────┬───────────────┘
                 │ (通过)
                 ▼
 ┌───────────────────────────────┐
 │ 3. 单邮箱冷却与单日限额(Cap)   │ -> 60s 严格冷却, 单邮箱 24 小时 ≤ 5 封
 └───────────────┬───────────────┘
                 │ (通过)
                 ▼
 ┌───────────────────────────────┐
 │ 4. 邮箱后缀白名单/合规性预检   │ -> 过滤 Disposable 临时邮箱及不符合后缀
 └───────────────┬───────────────┘
                 │ (通过)
                 ▼
      [ 执行多通道异步投递 ]
```

### 3.4 多通道发信与高可用容灾 (Multi-Channel Mail Delivery & Failover)

#### 3.4.1 核心发信策略与容灾降级架构
- **`MailDeliveryProvider` 接口**：
  ```java
  public interface MailDeliveryProvider {
      String getProviderName();
      boolean isAvailable();
      void send(MailMessageBO message) throws MailDeliveryException;
  }
  ```
- **发信渠道实现**：
  1. **`SmtpMailDeliveryProvider`**：支持动态 SMTP Host、Port（465 SSL / 587 STARTTLS）、用户名、密码与超时控制；
  2. **`AliyunDirectMailDeliveryProvider`**：支持阿里云 DirectMail / 云 API 签名投递（支持从配置中心动态加载 AccessKey/SecretKey）；
  3. **`LoggingFallbackMailDeliveryProvider`**：未配置任何邮件服务时的优雅降级通道，打印日志并模拟成功，确保开发与测试环境不中断流程。
- **故障转移器 (`FailoverMailDispatcher`)**：
  - 维护主通道（Primary）与备用通道（Backup）列表；
  - 主通道发送发生不可恢复异常或超时，自动切换备用通道；
  - 记录发信日志与状态回执（`MailDeliveryAuditLog`）。

#### 3.4.2 异步受控任务执行器 (`MailTaskExecutor`)
- 使用线程池配合 `Semaphore(50)` 限制并发；
- 引入**指数退避重试（Exponential Backoff Retry）**：当遭遇网络抖动时，按 1s、3s、8s 自动进行最多 3 次重试。

---

## 4. API 演进与接口设计

### 4.1 图形/行为验证码接口 (`/api/v1/captcha`)
| 方法 | 路径 | 说明 | 参数 / Body | 响应 |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/captcha/challenge` | 获取人机验证挑战（支持场景与自适应风控） | `scene` (可选), `oldCaptchaId` (可选) | `CaptchaResponseVO` (包含 `captchaId`, `type`, `imageBase64`, `sliderBase64`, `y`) |
| `GET` | `/api/v1/captcha` | 兼容旧版获取图形验证码接口 | `oldCaptchaId` (可选) | `CaptchaResponseVO` |
| `POST` | `/api/v1/captcha/verify-ticket` | 行为式滑块核验并签发二次票据 Ticket | `CaptchaVerifyDTO` (`captchaId`, `userInput`, `scene`, `costTime`) | `CaptchaTicketVO` (`ticket`, `scene`, `expireSeconds`) |

### 4.2 邮件验证码接口 (`/api/v1/email`)
| 方法 | 路径 | 说明 | 参数 / Body | 响应 |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/email/captcha/register` | 发送注册邮箱验证码（全方位防轰炸） | `EmailCaptchaRequestDTO` | `ApiResponse<Void>` |
| `POST` | `/api/v1/email/captcha/change-password` | 发送修改密码邮箱验证码 | `EmailCaptchaRequestDTO` | `ApiResponse<Void>` |
| `POST` | `/api/v1/email/captcha/verify` | 校验修改密码验证码并签发凭证 | `VerifyChangePasswordCodeRequestDTO` | `ApiResponse<String>` (Token) |
| `POST` | `/api/v1/email/test` | 测试当前激活发信通道的连通性 | 无 | `ApiResponse<Void>` |

---

## 5. 项目目录结构与重构清单

```
src/volur-lite-platform/volur-verification/
├── pom.xml
└── src/main/java/com/humolix/volur/verification/
    ├── captcha/
    │   ├── engine/
    │   │   └── SlidePuzzleEngine.java            // 滑块缺口计算与图像切片算法
    │   ├── provider/
    │   │   ├── CaptchaProvider.java              // 策略接口
    │   │   ├── LineCaptchaProvider.java          // 字符线段
    │   │   ├── MathCaptchaProvider.java          // 算术运算
    │   │   ├── ShearCaptchaProvider.java         // 扭曲字符
    │   │   ├── SlidePuzzleCaptchaProvider.java   // 行为滑块
    │   │   └── CloudTurnstileCaptchaProvider.java// 云端人机验证适配器
    │   └── manager/
    │       └── CaptchaProviderManager.java       // 策略管理器
    ├── risk/
    │   ├── enums/
    │   │   ├── RiskLevel.java                    // 风控评级 (LOW, MEDIUM, HIGH, BLOCKED)
    │   │   └── CaptchaScene.java                 // 业务场景枚举
    │   ├── engine/
    │   │   └── RiskEvaluationEngine.java         // 自适应难度评级风控引擎
    │   └── limiter/
    │       └── RedisSlidingWindowRateLimiter.java// Redis ZSet 精准滑动窗口限频器
    ├── mail/
    │   ├── bo/
    │   │   └── MailMessageBO.java                // 统一邮件业务对象
    │   ├── provider/
    │   │   ├── MailDeliveryProvider.java         // 多通道策略接口
    │   │   ├── SmtpMailDeliveryProvider.java     // 动态 SMTP 渠道
    │   │   ├── AliyunDirectMailDeliveryProvider.java // 阿里云 DirectMail 渠道
    │   │   └── LoggingFallbackMailDeliveryProvider.java // 本地日志降级渠道
    │   ├── dispatcher/
    │   │   └── FailoverMailDispatcher.java       // 主备故障转移分发器
    │   └── executor/
    │       └── MailTaskExecutor.java             // 并发受控 + 退避重试异步执行器
    ├── domain/
    │   ├── dto/
    │   │   ├── CaptchaVerifyDTO.java             // 滑块核验 DTO
    │   │   ├── EmailCaptchaRequestDTO.java       // 发送邮件 DTO
    │   │   └── VerifyChangePasswordCodeRequestDTO.java
    │   └── vo/
    │       ├── CaptchaResponseVO.java            // 验证码出参 VO (多模态支持)
    │       └── CaptchaTicketVO.java              // 二次验证票据 VO
    ├── service/
    │   ├── CaptchaService.java                   // 验证码顶层服务
    │   ├── EmailService.java                     // 邮件顶层服务
    │   └── impl/
    │       ├── CaptchaServiceImpl.java
    │       └── EmailServiceImpl.java
    └── controller/v1/
        ├── CaptchaController.java                // 演进后验证码控制器
        └── EmailController.java                  // 邮件控制器
```

---

## 6. 验证与回归计划

1. **编译构建自检**：
   - 执行 `mvn clean compile -DskipTests`，确保 10 个子模块 100% 编译通过；
2. **多模态验证码测试**：
   - 验证 `LINE`、`MATH`、`SHEAR`、`SLIDE_PUZZLE` 各类验证码的生成与核验一致性；
   - 验证 `oldCaptchaId` 自动清理机制与一次性核销；
   - 验证两阶段 `captchaTicket` 签发与消费有效性；
3. **自适应风控与防轰炸限流测试**：
   - 连续高频请求验证码，验证风控引擎自适应将难度从基础提升为高阶滑块；
   - 模拟单 IP 高频请求邮件发送，验证 IP 级滑动窗口拦截；
   - 模拟单邮箱高频请求，验证 60s 冷却与单日 5 封上限拦截；
4. **多通道邮件容灾测试**：
   - 测试正常 SMTP 发信；
   - 模拟 SMTP 连接不可用时，验证系统平滑容灾切换或日志降级，不抛出非受控崩溃异常；
5. **`volur-auth` 完整链路联动测试**：
   - 验证注册流程（前置图形码 -> 邮件验证码 -> 注册提交）；
   - 验证找回密码与重置密码凭证链条无缝衔接。
