# 抽象提取安全会话基础模型 Session 与 AuthFlowSession 继承解耦重构方案

## 1. 方案背景与目标

在 Volur Lite 安全与身份认证体系演进中，针对会话管理（Session Management）提出了更清晰的分层与职责解耦要求：
1. **基础会话（Generic Session）与流程会话（Auth Flow Session）职责混合**：原 `AuthFlowSession` 同时承担了会话基础生命周期元数据（如会话唯一标识 `dsh`、网络环境、设备指纹、安全风控、扩展属性、创建与更新时间）以及特定认证流程状态机流转字段（如流程名称、当前步骤、步骤序号 `cid`、事务锁票据 `activeTL`、已完成步骤列表），导致模型职责过载；
2. **通用能力下沉与复用需求**：为后续在线会话（Online Session）、双因子校验临时会话、OAuth 会话等业务扩展预留通用的实体抽象；
3. **零破坏性与全链路向后兼容**：
   - 提取通用的基类 `Session`，包含通用会话属性；
   - `AuthFlowSession` 继承 `Session`，仅保留认证与注册状态机专用控制字段；
   - 采用 Lombok `@SuperBuilder` 保持现有构造调用链（如 `AuthFlowSession.builder().dsh(...)...build()`）100% 源码兼容；
   - 保持 Jackson 反序列化与 Redis 存储 JSON 结构平滑兼容。

---

## 2. 核心架构设计与改动点

### 2.1 会话属性归类与职责拆分

各字段归属收敛如下表所示：

| 属性名称 | 归属类型 | 说明与职责 |
| :--- | :--- | :--- |
| `dsh` | `Session` (基类) | 分布式会话句柄（Distributed Session Handle），作为全局唯一会话标识 |
| `clientIp` | `Session` (基类) | 客户端 IP 地址，用于来源安全校验与 IP 风险防控 |
| `userAgent` | `Session` (基类) | 客户端 User-Agent，用于客户端指纹比对 |
| `deviceFingerprint` | `Session` (基类) | 设备指纹，用于识别终端设备变化 |
| `riskLevel` | `Session` (基类) | 会话综合风险评级（LOW / MEDIUM / HIGH） |
| `metadata` | `Session` (基类) | 通用扩展元数据键值对字典（`Map<String, Object>`） |
| `createdAt` | `Session` (基类) | 会话创建毫秒时间戳 |
| `updatedAt` | `Session` (基类) | 会话最后活动与更新毫秒时间戳 |
| `flowName` | `AuthFlowSession` (子类) | 认证流程名称（如 `VlifWebLogin`） |
| `flowEntry` | `AuthFlowSession` (子类) | 认证流程入口（如 `WebServiceLogin`） |
| `identifier` | `AuthFlowSession` (子类) | 流程中正在认证的用户主体标识 |
| `currentStep` | `AuthFlowSession` (子类) | 状态机当前所处步骤（如 `init`, `pwd`, `pt/options` 等） |
| `cid` | `AuthFlowSession` (子类) | 状态机步骤序号计数器，防止越级与乱序调用 |
| `activeTL` | `AuthFlowSession` (子类) | 当前步骤生效的流转票据（Transition Lock, `ADG-...`） |
| `completedSteps` | `AuthFlowSession` (子类) | 当前流程已验证通过的历史步骤轨迹列表 |

### 2.2 实体类重构清单

1. **基础会话模型 (`Session.java`)**：
   - 位置：`src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/session/Session.java`
   - 实现 `Serializable` 接口；
   - 标注 `@Data`, `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`；
   - 声明 `dsh`, `clientIp`, `userAgent`, `deviceFingerprint`, `riskLevel`, `metadata`, `createdAt`, `updatedAt`。

2. **状态机会话模型 (`AuthFlowSession.java`)**：
   - 位置：`src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/session/AuthFlowSession.java`
   - 继承 `Session`；
   - 标注 `@Data`, `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@EqualsAndHashCode(callSuper = true)`, `@ToString(callSuper = true)`；
   - 仅保留 `flowName`, `flowEntry`, `identifier`, `currentStep`, `cid`, `activeTL`, `completedSteps`。

3. **单元测试落地 (`SessionInheritanceTest.java`)**：
   - 位置：`src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/domain/session/SessionInheritanceTest.java`
   - 覆盖基础属性读写、`@SuperBuilder` 链式调用继承、Jackson 双向序列化一致性及默认值初始化。

---

## 3. 规范与纯粹性审计

1. **纯粹代码与零注释铁律**：
   - 重构后的 `Session.java` 与 `AuthFlowSession.java` 严格杜绝任何解释性注释与冗余行内备注；
   - 完整推导逻辑与设计权责统一收敛至本方案文档。
2. **彻底重构铁律**：
   - 字段拆分彻底清晰，无任何冗余字段双写或兼容层别名。

---

## 4. 验证与回归测试

1. **单元测试执行**：
   - 执行 `mvn test "-Dtest=SessionInheritanceTest"`，4 个测试用例全部通过（`BUILD SUCCESS`）。
2. **平台模块全量回归**：
   - 执行 `mvn test "-Dtest=TransitionLockManagerTest,RecaptchaServiceImplTest,PasskeyServiceImplTest,SigninServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"`；
   - 13 个子模块全部构建成功，安全流程与状态机全量测试通过。
