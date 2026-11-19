# 平台声明式限流防爆破体系与敏感响应 CacheControl 安全加固方案

**文档版本**：v1.0  
**日期**：2026-11-19  
**施工范围**：`volur-framework`、`volur-auth`、`volur-verification`

---

## 1. 变更背景与系统现状分析

### 1.1 现状分析：系统中是否存在 `@RateLimit(key = "#request.email", ...)`
经过对整个平台的深度排查，系统**原本不存在**支持 SpEL 表达式（如 `#request.email`、`#request.dsh`、`#ip`）动态提取参数的声明式 `@RateLimit` 注解和切面：
- 原有的防刷逻辑高度分散且硬编码在各业务模块中（如 `EmailServiceImpl.checkMultiTierSendLimit` 中手动操作 Redis 与 `RedisSlidingWindowRateLimiter`；`TrainingRateLimitService` 中硬编码题目答错冷却与锁定）。
- 认证流（登录密码验证、找回密码验证码核验、注册验证码核验）缺乏统一的“防暴力破解/防撞库（Anti-Brute-Force）”拦截机制。
- 业务代码与限频逻辑深度耦合，无法做到在 Controller 层通过一行注解声明式防刷与防爆破。

### 1.2 敏感数据暴露与浏览器/代理缓存隐患
在身份认证与凭证找回链路中，以下接口返回了用户脱敏后的身份隐私数据（如脱敏邮箱、脱敏手机号、已登录账号列表、流程安全选项）：
- `GET /api/v1/auth/recovery/password/options`：找回密码可选通道（含 `maskedEmail`、`maskedPhone`）；
- `GET /api/v1/auth/recovery/username/options`：找回用户名通道；
- `GET /api/v1/auth/signin/accountchooser`：历史登录账号探测列表；
- `GET /api/v1/auth/signup/protect/options`：注册二步验证保护选项。

若未设置 HTTP 缓存协商头，浏览器历史记录回退、企业内网代理或共享网络网关可能会持久化缓存这些敏感数据，造成敏感隐私外泄风险。

---

## 2. 核心架构设计

### 2.1 平台级声明式限流与防爆破体系 (`volur-framework`)

限流与防爆破属于平台底层通用安全基座，统一收敛在 `com.humolix.volur.framework.ratelimit` 包下，提供给全平台任意模块开箱即用：

```
com.humolix.volur.framework.ratelimit
├── annotation
│   ├── RateLimit.java       # 声明式限流防爆破核心注解 (支持 @Repeatable)
│   └── RateLimits.java      # 注解容器
├── enums
│   └── RateLimitType.java   # 限流策略枚举 (SLIDING_WINDOW, FIXED_WINDOW, BRUTE_FORCE)
├── aspect
│   └── RateLimitAspect.java # AOP 切面 (SpEL 解析、多规则校验、防爆破状态机)
└── service
    └── RateLimitService.java# Redis 底层操作与原子计数服务 (Fail-Open 韧性设计)
```

#### 2.1.1 限流类型定义 (`RateLimitType`)
1. **`SLIDING_WINDOW`（滑动窗口防刷）**：
   - 适用于常规防刷，基于 Redis ZSet 实现高精度滑动窗口；
   - 无论请求成功与否均计入调用频次，消除固定窗口临界突刺。
2. **`FIXED_WINDOW`（固定窗口/单次冷却）**：
   - 适用于操作冷却（如 60 秒内只允许重新发送 1 次邮件验证码）。
3. **`BRUTE_FORCE`（防暴力破解/防撞库）**：
   - **前置拦截**：检查 `vredis:ratelimit:brute:lock:<module>:<key>` 锁标记，若处于锁定期则直接拒绝并计算剩余等待时间，不执行目标方法；
   - **执行过程**：放行执行目标认证逻辑；
   - **成功重置**：若认证通过（无异常抛出），立即删除失败计数器 `vredis:ratelimit:brute:fail:<module>:<key>`；
   - **异常计数与锁定**：若捕获到认证失败异常（凭据错误/验证码错误），递增失败计数器；当连续失败达到阈值（如连续 5 次错误）时，自动写入锁定标记并阻断后续请求。

#### 2.1.2 智能 SpEL 解析与安全内置宏
- 支持解析任意方法参数对象属性：例如 `#request.email`、`#request.dsh`、`#email`；
- 内置开箱即用的上下文宏：
  - `#ip`：自动通过 `RequestUtil.getRemoteIp()` 解析真实客户端 IP；
  - `#uri`：当前请求路径；
  - `#userId`：当前登录用户 ID（未登录为 0L）；
  - `#username`：当前登录用户名。
- 支持复合键表达式拼接：例如 `key = "#ip + ':' + #request.email"`。

---

### 2.2 响应实体扩展与 CacheControl 安全加固

#### 2.2.1 `ApiResponseEntity` 流式扩展
在 `ApiResponseEntity` 中新增流式构建方法：
```java
public ApiResponseEntity<T> cacheControl(CacheControl cacheControl) {
    if (cacheControl != null) {
        String headerValue = cacheControl.getHeaderValue();
        if (headerValue != null && !headerValue.isBlank()) {
            this.getHeaders().setCacheControl(headerValue);
        }
    }
    return this;
}
```

#### 2.2.2 敏感端点防缓存加固
在所有涉及身份信息与认证选项的 Controller 返回上挂载 `.cacheControl(CacheControl.noStore())`：
- `RecoveryController.getPasswordRecoveryOptions`
- `RecoveryController.getUsernameRecoveryOptions`
- `SignInController.getAccounts`
- `SignUpController.getOptions`

---

## 3. 落地实施细节

### 3.1 关键接口注解接入清单

| 模块 | 控制器 | 接口路径 | 防护类型 | 规则配置 | 业务目标 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `verification` | `EmailController` | `POST /captcha/register` | `SLIDING_WINDOW` | `#request.email` 60s 内 1 次<br>`#ip` 3600s 内 10 次 | 杜绝单邮箱/单IP滥发邮件刷验证码 |
| `auth` | `RecoveryController` | `POST /username/email` | `SLIDING_WINDOW` | `#request.email` 60s 内 1 次 | 防止高频撞库找回用户名发信 |
| `auth` | `RecoveryController` | `POST /password/emailotp/send` | `SLIDING_WINDOW` | `#dsh` 60s 内 1 次 | 会话维度邮件验证码发信防刷 |
| `auth` | `RecoveryController` | `POST /password/emailotp/verify` | `BRUTE_FORCE` | `#request.dsh` 300s 失败 5 次锁 10 分钟 | 找回密码 6 位验证码防穷举爆破 |
| `auth` | `SignInController` | `POST /authenticate` | `BRUTE_FORCE` | `#request.dsh` 900s 失败 5 次锁 15 分钟 | 用户登录密码防暴力破解 |
| `auth` | `SignUpController` | `POST /challenge/etotp` | `BRUTE_FORCE` | `#request.dsh` 300s 失败 5 次锁 10 分钟 | 注册邮箱验证码防爆破 |
| `auth` | `SignUpController` | `POST /email/resend` | `SLIDING_WINDOW` | `#request.dsh` 60s 内 1 次 | 注册重发验证码 60s 冷却 |

---

## 4. 规范与安全验证

1. **编译兼容性**：全平台 14 个 Maven 子模块编译通过（BUILD SUCCESS），JDK 25 兼容无语法冲突。
2. **零注释铁律**：所有修改的 Java 代码保持极致纯净，无任何冗余行内解释性注释，全部技术方案与设计原理完整收敛于本设计文档中。
3. **架构规范**：严格遵循不可变声明（`private final`）、Lombok `@RequiredArgsConstructor`、统一 Redis 键前缀 `vredis:ratelimit:*` 以及异常标准机制。
