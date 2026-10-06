# SessionManager 冗余与硬编码会话重载方法清理审计

## 1. 变更背景与动因
在之前的流程会话架构重构中，平台将 [`TransitionLockManager`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/flow/TransitionLockManager.java) 中的会话生命周期与 Redis 持久化能力抽取到了独立的 [`SessionManager`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/session/SessionManager.java) 组件中。

然而，在抽取过程中保留了两个带有默认硬编码流程名称的历史重载方法：
- `public Session createSession(RequestHeader requestHeader, RequestDeviceParams requestDeviceParams)`
- `public Session recoverySession(String dsh, RequestHeader requestHeader, RequestDeviceParams requestDeviceParams)`

经全局代码审计，这两个方法在全平台（包括业务层、控制器层与单元测试）中均**无任何调用处**，属于死代码（Dead Code）。

## 2. 缺陷与安全隐患分析
1. **违背多工作流架构（Multi-Flow Architecture）设计**：
   这两个方法内部写死了固定流程名 `"VlifWebLogin"` 和入口名 `"WebServiceLogin"`。在当前支持登录（`VlifWebLogin`）、注册（`VlifWebSignUp`）、重置密码等多种并行业务流程的架构下，默认固定为登录流程是严重的反模式。若外部模块误调用此方法，会导致非登录业务会话被错误绑定到登录状态机，造成跨流程状态污染。
2. **单一真实源（Single Source of Truth）与彻底重构铁律**：
   根据 `GEMINI.md` 的彻底重构规范，严禁保留废弃桥接层、兼容别名与无用的历史兜底代码。全平台所有会话创建（如 `/api/v1/session/initSess` 对应的 [`SessionServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/SessionServiceImpl.java)）与会话恢复（如 [`SigninServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SigninServiceImpl.java) 和 [`SignupServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SignupServiceImpl.java)）均显式声明了对应的 `flowName` 与 `flowEntry`，接口契约严格、清晰。

## 3. 具体改动清单
1. **[`SessionManager.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/session/SessionManager.java)**：
   - 彻底删除 `createSession(RequestHeader, RequestDeviceParams)` 重载方法；
   - 彻底删除 `recoverySession(String, RequestHeader, RequestDeviceParams)` 重载方法；
   - 仅保留入参完备且语义明确的：
     - `createSession(String flowName, String flowEntry, RequestHeader requestHeader, RequestDeviceParams requestDeviceParams)`
     - `recoverySession(String dsh, String flowName, String flowEntry, RequestHeader requestHeader, RequestDeviceParams requestDeviceParams)`
2. **[`SessionManagerTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/session/SessionManagerTest.java)**：
   - 将单元测试中的缺失 dsh 快速失败断言用例更新为显式传递 5 个参数的签名形式。

## 4. 影响与回归验证
- 全平台 13 个 Maven 模块（`volur-framework`、`volur-logging`、`volur-system`、`volur-user`、`volur-verification`、`volur-security`、`volur-notification`、`volur-auth`、`volur-challenge`、`volur-container`、`volur-competition`、`volur-server`）重新执行完整测试用例，全量 100% 通过（`BUILD SUCCESS`）。
