# 施工与修改记录：volur-security 独立会话流程模块抽取与 volur-auth 解耦架构

**日期**：2026-09-29  
**作者**：Antigravity Agent  
**模块**：`volur-security`、`volur-auth`、`volur-framework`、`volur-server`

---

## 1. 变更背景与架构演进

为实现统一的 VLIF（Volur Login Interface Framework）流程会话状态机管理，同时彻底消除模块间的**双向循环引用（Bidirectional / Circular Dependency）**，平台对安全与会话流模块进行了职责划分与重构：

1. **会话状态机抽象 (VLIF Flow Management)**：
   - 将流程会话初始化与控制中心从具体业务认证流中解耦，抽取至独立模块 `volur-security`；
   - 暴露 `/api/v1/flow/initSess` 端点，支持通过 `flowName`（大流程）与 `flowEntry`（具体功能入口）初始化集群唯一的分布式会话句柄 `dsh`，用于承载前端路由与后续认证、注册、密码找回等挑战阶段；
2. **避免双向引用原则 (Acyclic Dependency Graph)**：
   - 彻底将属于认证核心的组件（Spring Security 配置 `SecurityConfig`、白名单配置 `SecurityWhitelistProperties`、认证拦截器 `JwtAuthFilter`、风险过滤器 `RiskIpFilter`、维护模式过滤器 `MaintenanceModeFilter`、国密加解密 `Sm3*`、Token 会话管理 `TokenService`、用户详情 `UserDetails`、鉴权工具类 `SecurityUtils` 等）完整收敛归还给 `volur-auth`；
   - 将无状态的基础状态机流转核心（`TransitionLockManager`、`AuthFlowSession`、`SignInFlowSession`）沉降至平台最底层基础核心 `volur-framework`（`com.humolix.volur.framework.security.flow.*`）；
   - 使 `volur-auth` 与 `volur-security` 均直接依赖 `volur-framework`，二者之间**互不依赖**，实现零耦合单向 DAG 架构。

---

## 2. 核心架构与依赖拓扑

重构后平台模块依赖关系如下：

```
                ┌─────────────────────────────────┐
                │         volur-framework         │
                │  - TransitionLockManager        │
                │  - AuthFlowSession              │
                │  - RequestUtil / ContextUtil    │
                └───────┬─────────────────┬───────┘
                        │                 │
           ┌────────────▼───────┐   ┌─────▼──────────────┐
           │     volur-auth     │   │   volur-security   │
           │  - SecurityConfig  │   │  - FlowController  │
           │  - JwtAuthFilter   │   │  - FlowService     │
           │  - Sm3* / Filters  │   │  - FlowServiceImpl │
           │  - TokenService    │   │  - FlowInitVO      │
           └────────────┬───────┘   └─────┬──────────────┘
                        │                 │
                        └────────┬────────┘
                                 │
                        ┌────────▼────────┐
                        │  volur-server   │
                        │  (Spring Boot)  │
                        └─────────────────┘
```

---

## 3. 具体修改细节与文件清单

### 3.1 `volur-security` 模块职责收敛
- **Maven 工程配置** (`volur-security/pom.xml`)：
  - 依赖 `volur-framework`、`volur-logging`、`volur-system`、`volur-user`、`volur-verification`；
  - 移除对 `volur-auth` 的依赖，杜绝双向依赖。
- **控制器层** (`FlowController.java`)：
  - 路径：`@RequestMapping("/api/v1/flow")`；
  - 提供 `GET /api/v1/flow/initSess?flowName={flowName}&flowEntry={flowEntry}&fp={fp}&t={t}&dsh={dsh}`；
  - 返回标准视图 `ApiResponse<FlowInitVO>`。
- **服务层** (`FlowService.java` & `FlowServiceImpl.java`)：
  - 依赖 `volur-framework` 中的 `TransitionLockManager` 创建或更新 `AuthFlowSession` 并记录指纹与时间戳元数据；
  - 返回装配完成的 `FlowInitVO`（含 `dsh`、`cid`、`serverTime`）。
- **单元测试** (`FlowControllerTest.java`)：
  - 验证 `initSess` 接口参数注入与 VO 数据返回。

### 3.2 `volur-auth` 完整保留与恢复
- **安全与拦截链配置恢复**：
  - `com.humolix.volur.auth.config.SecurityConfig`：配置 Spring Security 过滤器链与鉴权提供者；
  - `com.humolix.volur.auth.config.SecurityWhitelistProperties`：维护系统放行白名单；
  - `com.humolix.volur.auth.security.exception.*`：`SecurityAccessDeniedHandler`、`SecurityAuthenticationEntryPoint`；
  - `com.humolix.volur.auth.security.filter.*`：`JwtAuthFilter`、`MaintenanceModeFilter`、`RiskIpFilter`；
  - `com.humolix.volur.auth.security.crypto.*`：`Sm3AuthProvider`、`Sm3PasswordEncoder`、`Sm3Util`；
  - `com.humolix.volur.auth.security.entity.UserDetails` & `UserDetailsService`；
  - `com.humolix.volur.auth.security.util.SecurityUtils` & `SecurityWhitelistMatcher`；
  - `com.humolix.volur.auth.service.TokenService` & `TokenServiceImpl`。
- **移除对 `volur-security` 的引用**：
  - `volur-auth/pom.xml` 移除 `volur-security` 依赖；
  - 内部类如 `AuthServiceImpl`、`SignInServiceImpl`、`SuperAdminInitializer`、`AuthVerificationCrossValidationTest` 等统一从 `com.humolix.volur.auth.*` 和 `com.humolix.volur.framework.*` 导入。

### 3.3 `volur-framework` 状态机流转基础底座
- `com.humolix.volur.framework.security.flow.AuthFlowSession`：通用分布式状态机会话模型；
- `com.humolix.volur.framework.security.flow.SignInFlowSession`：兼容 SignIn 状态模型；
- `com.humolix.volur.framework.security.flow.TransitionLockManager`：高熵 `dsh` 派发、HMAC-SHA256 签名 `TL` 票据签发、步骤序号 `cid` 严格校验与防跳步防重放逻辑；
- `com.humolix.volur.framework.security.TransitionLockManagerTest`：覆盖 `dsh` 格式、`TL` 签名验证、跳步与重放拦截用例。

### 3.4 登录控制器与前端流程初始化收敛 (`initSession` 彻底废除)
- **后端登录接口收敛**：
  - 彻底移除 `SignInController` 中冗余的历史初始化端点（`GET /api/v1/auth/signin/init` 与 `/session/init`）；
  - 同步清理 `SignInService` 及 `SignInServiceImpl` 中的 `initSession` 方法；
  - 彻底删除废弃的响应实体 `SignInInitVO.java`；
  - 登录全阶段仅保留标识符提交 (`/api/v1/auth/signin/identifier`) 与挑战裁决 (`/api/v1/auth/signin/challenge`)，会话与上下文生命周期统一交由 `volur-security` 的 `/api/v1/flow/initSess` 统一签发。
- **前端 API 与状态机对齐 (`src/volur-lite-view-NEXT`)**：
  - 从 `src/api/auth.ts` 中彻底移除 `initSignInSessionApi` 与 `SignInInitVO` 导入；
  - 前端全流程（登录、注册等）通过 `initFlowSessApi` 统一请求 `/api/v1/flow/initSess` 获取 `dsh`，并自动维护在 Pinia Setup Store `authFlow.ts` 中与路由参数深度联动。

---

## 4. 验证与审计结果

1. **后端完整构建与全套测试**：
   - 执行 `mvn clean test` 遍历平台全部 13 个子模块：
     - `volur-framework`：SUCCESS（所有通用工具、状态机与缓存测试通过）
     - `volur-auth`：SUCCESS（跨验证、DTO 校验、CORS、白名单、过滤器与 SM3 测试全部通过）
     - `volur-security`：SUCCESS（`FlowControllerTest` 通过）
     - `volur-competition`、`volur-container`、`volur-challenge` 等所有下游业务模块：SUCCESS
     - 总体测试运行无任何 Failure、Error 或异常阻断。
2. **前端工程构建**：
   - 在 `src/volur-lite-view-NEXT` 执行 `npm run build`：
     - `vue-tsc --noEmit` 校验通过，零类型错误；
     - Vite 生产打包顺利完成。
