# 废弃 AuthController 控制器体系下线与 Nginx 认证限流清理方案

## 1. 变更背景与目标

随着 Volur Lite 新一代前端工程（`volur-lite-view-NEXT`）与后端认证体系全面迁移至基于 **Google Accounts 风格的状态机流转模型（Dsh/TL 机制）**：
- 登录流程由 [`SigninController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SigninController.java) (`/api/v1/auth/signin/*`) 与 Challenge 体系接管；
- 注册流程由 [`SignupController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SignupController.java) (`/api/v1/auth/signup/*`) 驱动；
- 凭证与账号找回由 [`RecoveryController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/RecoveryController.java) (`/api/v1/auth/recovery/*`) 接管；
- 会话生命周期与登出/Token 刷新由 [`TokenController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/TokenController.java) (`/api/v1/auth/logout`, `/api/v1/auth/refresh`) 独立承接。

历史控制器 [`AuthController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/AuthController.java) 早前已被标注为 `@Deprecated(since = "1.0.0", forRemoval = true)`。经审查，前端工程 `volur-lite-view-NEXT` 的全部源码对该控制器及其实体没有任何引用。为遵循 `GEMINI.md` 的“重构彻底性铁律”与“单一真实源（SSOT）原则”，需将后端废弃的旧控制器、旧服务实现、旧 DTO/VO 实体、配套专属异常以及 Nginx 反向代理层中的遗留限流规则彻底下线并清理。

---

## 2. 关键设计与改动清单

### 2.1 后端废弃组件物理移除 (Backend Retirement)

1. **控制器层清理**：
   - 彻底删除 `src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/AuthController.java`。
2. **服务层清理**：
   - 彻底删除 `src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/AuthService.java`；
   - 彻底删除 `src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java`。
3. **领域实体 (DTO/VO) 清理**：
   - 移除旧登录/注册/重置/注销领域模型：
     - `RegisterRequestDTO.java`
     - `AuthenticateRequestDTO.java`
     - `AuthenticateResponseVO.java`
     - `ForgotPasswordRequestDTO.java`
     - `ForgotPasswordResetRequestDTO.java`
     - `ResetPasswordRequestDTO.java`
     - `VerifyChangePasswordCodeRequestDTO.java`
     - `ChangePasswordRequestDTO.java`
     - `DeleteAccountRequestDTO.java`
     - `RefreshTokenRequestDTO.java`
4. **异常类清理**：
   - 移除仅被旧 `AuthServiceImpl` 引用的历史异常类：
     - `AuthException.java`
     - `UserOrEmailExistException.java`
     - `InvalidCaptchaException.java`
     - `TokenAlreadyBlacklistedException.java`
     - `TokenInvalidException.java`
5. **单元测试清理**：
   - 移除针对旧 `AuthServiceImpl.register` 与旧 DTO 校验的测试类：
     - `AuthVerificationCrossValidationTest.java`
     - `RegisterRequestDTOValidationTest.java`

### 2.2 安全配置与白名单收敛 (Security & Filter Whitelist Alignment)

1. **[`SecurityWhitelistProperties.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityWhitelistProperties.java)**：
   - 从 `WHITE_LIST_PATHS` 中彻底移除废弃的未授权访问端点：
     - `/api/v1/auth/login`
     - `/api/v1/auth/register`
     - `/api/v1/auth/forgot-password`
     - `/api/v1/auth/password-resets`
     - `/api/v1/auth/reset-password-confirm`
   - 保留现代流转端点 `/api/v1/auth/signin/**`、`/api/v1/auth/signup/**`、`/api/v1/auth/recovery/**`、`/api/v1/session/**`、`/api/v1/auth/logout`、`/api/v1/auth/refresh`。
2. **[`MaintenanceModeFilter.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/MaintenanceModeFilter.java)**：
   - 从维护模式豁免路径列表 `EXEMPT_PATHS` 中移除已下线的 `/api/v1/auth/login`（保留 `/api/v1/auth/signin/**`）。
3. **安全拦截器单测路径同步更新**：
   - 同步修正 [`SecurityWhitelistMatcherTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/security/SecurityWhitelistMatcherTest.java)、[`MaintenanceModeFilterTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/security/filter/MaintenanceModeFilterTest.java)、[`CsrfProtectionFilterTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/security/filter/CsrfProtectionFilterTest.java) 及 [`RiskIpFilterTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/security/filter/RiskIpFilterTest.java) 中的测试路径，全面对齐现行真实路由 `/api/v1/auth/signin/identifier` 与 `/api/v1/auth/signin/authenticate`。

### 2.3 Nginx 反向代理限流规则升级 (Nginx Rate Limiting Upgrade)

在 [`nginx.conf`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/nginx/conf/nginx.conf#L200-L225) 与 [`nginx.conf.example`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/nginx/conf/nginx.conf.example#L167-L192) 中：
- 彻底移除废弃的 `location ^~ /api/v1/auth/login` 与 `location ^~ /api/v1/auth/register` 块；
- 替换为对新一代认证状态机入口的前置突发与并发限流配置：
  - `location ^~ /api/v1/auth/signin`
  - `location ^~ /api/v1/auth/signup`
  - `location ^~ /api/v1/auth/recovery`
- 继续严格复用 `limit_req zone=volur_auth_req_limit burst=10 nodelay;` 与 `limit_conn volur_conn_limit 15;`，筑牢针对暴力破解与凭据撞库攻击的网关级防护。

---

## 3. 验证与测试结果

1. **后端构建与测试**：
   - 执行 `mvn test -f src/volur-lite-platform/pom.xml -pl volur-auth -am`；
   - 结果：全量 52 个单元测试用例全部通过（Tests run: 52, Failures: 0, Errors: 0, Skipped: 0），Reactor 构建整体 `SUCCESS`。
2. **前后端依赖完整性**：
   - 确认整个平台及前端 `volur-lite-view-NEXT` 编译无任何遗留类型缺失或失效路径引用。
