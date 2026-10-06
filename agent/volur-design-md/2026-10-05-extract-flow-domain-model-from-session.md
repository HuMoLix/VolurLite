# 抽象提取独立状态机流程模型 Flow 与 Session 架构解耦施工记录

- **施工日期**：2026-10-05
- **施工模块**：`volur-security` (`com.humolix.volur.security.domain.session`)
- **执行规范**：严格遵守 `GEMINI.md`、`Rules.md` 之零注释铁律与单一真实源原则。

---

## 1. 变更背景与架构目标

在 Google Accounts 认证体系的映射中，会话（Session）与业务流程（Flow）具备不同的领域职责与生命周期：
1. **基础会话（Session）**：承担客户端网络上下文、硬件设备指纹、综合安全风控、扩展元数据以及基于 `dsh` 的分布式缓存索引主键；
2. **流程状态机（Flow）**：专注于状态机的具体流转，承载流程名称（`flowName`）、流程入口（`flowEntry`）、主体标识（`identifier`）、当前所处步骤（`currentStep`）、步骤序号（`cid`）、流转锁票据（`activeTL`）及历史完成步骤轨迹（`completedSteps`）；
3. **架构诉求**：
   - 移除 `Session.java` 内部的未解耦内部类 `Flow`；
   - 将 `Flow` 提取为同包下独立的顶级领域模型实体 [`Flow.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/session/Flow.java)；
   - 在 [`Session.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/session/Session.java) 中显式引入 `private Flow flow;` 成员引用，建立清晰的聚合关系。

---

## 2. 核心改动点

### 2.1 新建独立的顶级实体 [`Flow.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/session/Flow.java)
- 位置：`src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/session/Flow.java`；
- 继承并实现 `Serializable` 接口；
- 标注 Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`；
- 包含字段：
  - `flowName`（默认 `"VlifWebLogin"`）
  - `flowEntry`（默认 `"WebServiceLogin"`）
  - `identifier`
  - `currentStep`（默认 `"init"`）
  - `cid`（默认 `0`）
  - `activeTL`（当前激活的状态跃迁锁）
  - `completedSteps`（已完成步骤列表）

### 2.2 重构 [`Session.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/session/Session.java)
- 物理清理移除内部临时类 `private class Flow`；
- 引入顶级类型成员变量 `private Flow flow;`；
- 消除类文件内的冗余注释与行内备注。

---

## 3. 构建与回归测试验证

1. **编译构建**：
   - 执行 `mvn test-compile -pl volur-security`，31 个源文件与 7 个测试类全部编译成功（`BUILD SUCCESS`）。
2. **测试回归**：
   - 执行 `mvn test -pl volur-security`，29 项单元测试全部通过（0 失败、0 错误、0 跳过）。
