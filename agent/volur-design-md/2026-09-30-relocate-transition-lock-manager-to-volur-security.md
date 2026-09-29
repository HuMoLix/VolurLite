# 施工与修改记录：TransitionLockManager 迁移至 volur-security 模块与 Dsh/TL 生成逻辑解耦拆分

**日期**：2026-09-30  
**作者**：Antigravity Agent  
**模块**：`volur-security`、`volur-auth`、`volur-framework`、`volur-lite-platform`

---

## 1. 变更背景与架构演进

在 VLIF（Volur Login Interface Framework）流转状态机的演进过程中，此前为打破双向依赖，将状态流转锁管理器临时沉降在底层的 `volur-framework` 中。然而从领域驱动设计（DDD）与安全职责划分角度：
1. **安全职责归属**：`TransitionLockManager` 与状态机会话安全校验、状态转移、防重放/防跳步控制直接相关，属于平台安全与访问控制的核心资产，应归属在 `volur-security` 模块中；
2. **单一职责原则（SRP）与代码高内聚**：原 `TransitionLockManager` 内部同时承揽了分布式会话句柄（`dsh`）的生成、HMAC-SHA256 签名的流转锁票据（`tl`）的生成与加密、以及 Redis 会话状态生命周期调度等多项职责。为提升代码可读性、可维护性与单一职责解耦，必须将 `dsh` 与 `tl` 的生成算法拆分为独立的生成器类；
3. **彻底清理历史兼容包袱**：依据平台规范（`GEMINI.md`），彻底删除空转且已废弃的兼容别名类（如 `SignInFlowSession`），禁止妥协保留任何兼容层；代码文件遵循极致纯净与零注释铁律，文档统一外置沉淀。

---

## 2. 依赖拓扑与模块设计

重构后的模块单向依赖拓扑清晰明确，杜绝任何循环依赖：

```
                    ┌───────────────────────────┐
                    │      volur-framework      │
                    │   (Base Util / Context)   │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │      volur-security       │
                    │  - DshGenerator           │
                    │  - TransitionLockGenerator│
                    │  - TransitionLockManager  │
                    │  - AuthFlowSession        │
                    │  - FlowService / Controll.│
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │        volur-auth         │
                    │  - SigninServiceImpl      │
                    │  - SecurityConfig         │
                    │  - JwtAuthFilter          │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │       volur-server        │
                    │       (Spring Boot)       │
                    └───────────────────────────┘
```

- **Maven Reactor 顺序**：在父工程 `pom.xml` 中将 `<module>volur-security</module>` 排在 `<module>volur-auth</module>` 之前；
- **依赖声明**：`volur-auth/pom.xml` 显式引入 `volur-security` 模块依赖。

---

## 3. 具体改动与实现细节

### 3.1 独立生成器解耦拆分（volur-security）

1. **`DshGenerator.java` (`com.humolix.volur.security.flow.DshGenerator`)**：
   - 职责：专注于分布式会话句柄（Distributed State Handle，简称 `dsh`）的生成；
   - 算法结构：`S-<clusterId>:<timestampMicro>`，结合微秒级时间戳、自增原子序列（12位循环）与机器集群标识，保障集群分布式唯一性与时序单调性；
   - 声明为 Spring `@Component`。

2. **`TransitionLockGenerator.java` (`com.humolix.volur.security.flow.TransitionLockGenerator`)**：
   - 职责：专注于生成不可伪造的高熵状态流转锁票据（Transition Lock，简称 `tl`）；
   - 算法结构：基于 HMAC-SHA256 算法，以系统安全密钥 `secretKey`、随机高熵盐（`SecureRandom` 16字节）以及会话关键元数据（`dsh`, `cid`, `flowName`, `flowEntry`, `targetStep`）计算防伪消息摘要，输出格式为 `ADG-<Base64UrlSafe>`；
   - 声明为 Spring `@Component`。

### 3.2 状态流转管理器与会话模型重构（volur-security）

1. **`AuthFlowSession.java` (`com.humolix.volur.security.domain.session.AuthFlowSession`)**：
   - 规范迁移至 `volur-security` 的领域模型目录 `domain/session` 下；
   - 承载 `dsh`、`cid`（单调递增步骤序列号）、`currentStep`、`flowName`、`flowEntry`、`deviceFingerprint` 等核心状态，支持动态扩展上下文参数（`Map<String, Object> context`）。

2. **`TransitionLockManager.java` (`com.humolix.volur.security.flow.TransitionLockManager`)**：
   - 迁移至 `com.humolix.volur.security.flow`；
   - 注入 `DshGenerator` 与 `TransitionLockGenerator`，遵循不可变依赖注入（`private final` + Lombok `@RequiredArgsConstructor`）；
   - 核心调度能力：
     - `createSession(...)`：委托 `DshGenerator` 派发新 `dsh`，初始化会话并持久化至 Redis；
     - `transition(...)`：状态转移推进，递增 `cid`，更新当前步骤与过期时间，委托 `TransitionLockGenerator` 签发下一阶段强校验 `tl`；
     - `verifyTransition(...)` / `verifyAndConsume(...)`：防重放、防跳步与 HMAC 强一致性校验；
   - 严格消除所有行内注释，保持源码极致纯粹。

### 3.3 全链路引用同步与清理旧包袱

1. **`volur-framework` 彻底清理**：
   - 物理删除 `TransitionLockManager.java`；
   - 物理删除 `AuthFlowSession.java`；
   - 物理删除历史遗留废弃别名子类 `SignInFlowSession.java`；
   - 物理删除原单元测试 `TransitionLockManagerTest.java`；
   - 删除无用空的 `flow` 目录。
2. **`volur-auth` 业务层适配**：
   - `SigninServiceImpl.java`：更新引用为 `com.humolix.volur.security.flow.TransitionLockManager` 与 `com.humolix.volur.security.domain.session.AuthFlowSession`；
   - 全链路适配无损。
3. **`volur-security` 业务层适配**：
   - `FlowServiceImpl.java`：更新引入本模块内的 `TransitionLockManager` 与 `AuthFlowSession`，消除代码内注释。

---

## 4. 自动化测试与质量验证

1. **测试用例迁移与重构**：
   - 新建 `src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/flow/TransitionLockManagerTest.java`；
   - 覆盖率验证：
     - `testCreateSession`：验证 `dsh` 结构与初始步骤会话建立；
     - `testTransition`：验证步骤推进、`cid` 自增与有效 `tl` 生成；
     - `testVerifyTransition_Success`：验证合法签名通过；
     - `testVerifyTransition_TamperedTL`：验证篡改或非法 `tl` 拦截；
     - `testVerifyTransition_WrongCid`：验证乱序、跳步 `cid` 拦截；
     - `testVerifyTransition_WrongTargetStep`：验证非法目标阶段拦截。
2. **Maven 自动化验证**：
   - 执行命令：`mvn test -pl volur-security -am`；
   - 验证结果：全量 60 个用例全部执行通过（`BUILD SUCCESS`，0 Failures，0 Errors）。
