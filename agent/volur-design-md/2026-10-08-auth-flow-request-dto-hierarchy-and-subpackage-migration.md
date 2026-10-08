# 认证与安全流程请求模型 (FlowRequestDTO) 基类抽取与 Auth DTO 子包结构治理

## 1. 背景与重构动因

在 Volur 平台的安全认证体系中，涉及登录流（`signin`）、注册流（`signup`）、找回账号流（`recovery`）以及安全凭据挑战流（`challenge`）等多个多阶段交互流程。
在早期的实现中，存在以下架构与工程缺陷：

1. **DTO 扁平堆叠与职责混乱**：
   - `com.humolix.volur.auth.domain.dto` 目录下混合平铺了数十个 DTO，涵盖通用接口入参、选择器入参、注册各阶段 DTO、找回凭据 DTO 等，缺乏领域边界与语义隔离；
2. **流程请求核心凭据重复声明**：
   - 诸如分布式会话句柄（`dsh`）、状态流转票据（`tl`）、状态机步骤序号（`cid`）在每一个流程 DTO 中均被重复编写字段、Validation 注解与 Getter/Setter；
   - 缺乏流程请求基底对象统一抽象，无法在网关、切面、防重放校验器以及抽象拦截层中进行统一类型多态收敛；
3. **架构分层依赖约束**：
   - `volur-auth` 依赖 `volur-security`，但 `volur-security` 不能反向依赖 `volur-auth`；
   - 因此，通用的流程请求基底类必须沉降于底层安全基础设施模块 `volur-security`（`com.humolix.volur.security.domain.dto`），以支持跨安全挑战与认证业务的全域继承复用。

---

## 2. 核心架构设计与重构方案

### 2.1 提取底层通用基类 `FlowRequestDTO`
在 `volur-security` 模块中创建基类 `FlowRequestDTO`：
- **位置**：`com.humolix.volur.security.domain.dto.FlowRequestDTO`
- **声明属性**：
  - `dsh` (`String`)：分布式会话句柄（Distributed Session Handle），强制 `@NotBlank` 与 `@Trimmed`；
  - `tl` (`String`)：状态流转锁/防重放票据（Transition Lock）；
  - `cid` (`Integer`)：流程序号/步骤索引（Correlation ID / Sequence Index）；
- **类级别注解**：
  - `@Data`
  - `@SuperBuilder`
  - `@NoArgsConstructor`
  - `@AllArgsConstructor`
  - `implements Serializable`（包含 `serialVersionUID = 1L`）

### 2.2 子包目录划分与迁移归属

将原平铺在 `com.humolix.volur.auth.domain.dto` 根路径下的流程相关 DTO 进行领域拆分，归入对应子包：

#### 1. 登录流领域 (`com.humolix.volur.auth.domain.dto.signin`)
- `SignInAuthenticateRequestDTO`：继承 `FlowRequestDTO`，扩展 `rememberMe`；
- `SignInIdentifierRequestDTO`：继承 `FlowRequestDTO`，扩展 `identifier`；
- `AccountChooserSelectRequestDTO`：继承 `FlowRequestDTO`，扩展 `identifier`；
- `AccountChooserProbeRequestDTO`：探测账号状态集合（`identifiers: List<String>`）；
- `AccountChooserRemoveRequestDTO`：移除单个账号标识（`identifier: String`）。

#### 2. 注册流领域 (`com.humolix.volur.auth.domain.dto.signup`)
- `SignUpNameRequestDTO`：继承 `FlowRequestDTO`，扩展 `username`；
- `SignUpEmailRequestDTO`：继承 `FlowRequestDTO`，扩展 `email`；
- `SignUpEmailResendRequestDTO`：继承 `FlowRequestDTO`；
- `SignUpEtotpChallengeRequestDTO`：继承 `FlowRequestDTO`，扩展 `code`；
- `SignUpPasswordRequestDTO`：继承 `FlowRequestDTO`，扩展 `password`, `confirmPassword`；
- `SignUpCompleteRequestDTO`：继承 `FlowRequestDTO`，扩展 `skipOptions`, `selectedOptions`。

#### 3. 找回流领域 (`com.humolix.volur.auth.domain.dto.recovery`)
- `UsernameRecoveryEmailRequestDTO`：继承 `FlowRequestDTO`，扩展 `email`；
- `UsernameRecoveryRecaptchaRequestDTO`：继承 `FlowRequestDTO`，扩展 `recaptchaTicket`；
- `UsernameRecoveryEmailOTPRequestDTO`：继承 `FlowRequestDTO`，扩展 `code`。

#### 4. 根目录保留非流程请求 (`com.humolix.volur.auth.domain.dto`)
保留常规单体操作接口 DTO（非 `dsh`/`tl`/`cid` 驱动的有限状态机）：
- `AuthenticateRequestDTO`、`RegisterRequestDTO`、`RefreshTokenRequestDTO`、`ChangePasswordRequestDTO`、`ResetPasswordRequestDTO`、`DeleteAccountRequestDTO` 等。

### 2.3 安全挑战请求模型统一继承 `FlowRequestDTO`
在 `volur-security` 中，挑战请求也统一调整为继承 `FlowRequestDTO`，消除重复字段：
- `ChallengeSelectRequestDTO` extends `FlowRequestDTO`
- `PasswordChallengeRequestDTO` extends `FlowRequestDTO`
- `EmailOTPChallengeRequestDTO` extends `FlowRequestDTO`
- `SMSOTPChallengeRequestDTO` extends `FlowRequestDTO`
- `ReCAPTCHAChallengeSubmitDTO` extends `FlowRequestDTO`
- `PasskeyAssertionVerifyRequestDTO` extends `FlowRequestDTO`

---

## 3. 改动影响面与全链路对齐

1. **控制器层 (`volur-auth` / `volur-security`)**：
   - 移除旧平铺 DTO 引用与通配符 `.*` 导入；
   - 显式引入对应 `signin`、`signup`、`recovery` 子包下的 DTO；
2. **服务接口与实现层**：
   - `SigninService` / `SigninServiceImpl`：入参类型完全对齐；
   - `SignupService` / `SignupServiceImpl`：入参类型完全对齐；
   - `RecoveryService` / `RecoveryServiceImpl`：入参类型完全对齐；
3. **测试用例层**：
   - 修复全部测试类中针对多参数构造函数的旧式调用，统一采用 Lombok `@SuperBuilder` 构建器语法，杜绝参数位置混淆隐患；
4. **编译与验证状态**：
   - Maven 平台 13 个子模块完整构建与测试执行通过（`BUILD SUCCESS`）；
   - 前端工程 `volur-lite-view-NEXT`（`vue-tsc` + `vite build`）编译通过，零错误。
