# 施工与修改记录：DshGenerator 迁移至 DeviceSessionHashGenerator 与功能升级方案

**日期**：2026-10-05  
**作者**：Antigravity Agent  
**模块**：`volur-security`、`volur-auth`

---

## 1. 现状剖析与架构归属治理

### 1.1 语义与包命名规范问题
原 `DshGenerator` 位于 `com.humolix.volur.security.flow.DshGenerator`：
1. **缩写反模式**：使用 `Dsh` 缩写而非全称 `DeviceSessionHash`，违背平台类名自解释与清晰性原则；
2. **领域职责错配**：DSH（Device Session Hash）在概念上是安全会话（`Session`）的全局唯一凭证句柄和 Redis 主键（`vredis:security:flow:session:{dsh}`）。原实现将其归置在 `security.flow` 流程流转包下，造成了**会话实体管理职责（Session Domain）与业务认证流转职责（Flow Domain）的语义混淆**。

### 1.2 架构重构策略
- 将生成器迁移并重命名至：[`com.humolix.volur.security.session.generator.DeviceSessionHashGenerator`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/session/generator/DeviceSessionHashGenerator.java)；
- 彻底物理删除旧类 [`DshGenerator.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/flow/DshGenerator.java) 以及对应的单测类，杜绝保留任何向下兼容别名或废弃桥接层；
- 在 [`SessionManager.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/session/SessionManager.java) 中注入 `DeviceSessionHashGenerator` 并委托其派发新会话句柄。

---

## 2. 功能升级分析与落地实施

### 2.1 缺陷 1：并发同一毫秒内的微秒时间戳倒退问题（Clock Monotonicity）
- **原实现**：
  ```java
  long timestampMicro = System.currentTimeMillis() * 1000L + (currentSeq % 1000);
  ```
  在同一毫秒内，当 `currentSeq` 刚好跨过 1000 的倍数时（例如从 999 变为 1000），`(currentSeq % 1000)` 会突变回 0，导致生成的微秒时间戳在物理同一毫秒内发生 **999 微秒的回拨**。
- **升级方案**：
  引入 CAS 无锁原子单调微秒时钟发生器：
  ```java
  private long generateMonotonicTimestampMicro() {
      long nowMicro = System.currentTimeMillis() * 1000L;
      while (true) {
          long last = lastTimestampMicro.get();
          long next = (nowMicro > last) ? nowMicro : last + 1;
          if (lastTimestampMicro.compareAndSet(last, next)) {
              return next;
          }
      }
  }
  ```
  保证任意并发突发下生成的微秒时间戳严格单调不减，彻底杜绝时间戳倒退。

### 2.2 缺陷 2：缺少领域元数据反解与校验标准工具方法（Missing Domain Capabilities）
- **原实现**：
  仅对外提供孤立的 `generate()` 方法。调用方或测试代码若要校验格式、反解集群 `workerId` 或提取时间戳，必须在多处硬编码 `dsh.substring(2).split(":")` 和偏移量数学计算，极易产生代码异味。
- **升级方案**：
  在 `DeviceSessionHashGenerator` 中补齐完备的强类型自解释领域工具方法：
  - `public boolean isValid(String dsh)`：基于正则 `^S-(\\d{9}):(\\d{13,19})$` 严格校验输入 DSH 是否合规；
  - `public long extractWorkerId(String dsh)`：提取并反解生成该 DSH 的分布式节点 ID；
  - `public long extractTimestampMillis(String dsh)`：提取毫秒时间戳；
  - `public long extractTimestampMicro(String dsh)`：提取微秒时间戳；
  - `public boolean isExpired(String dsh, Duration ttl)`：便捷判定该 DSH 是否超时失效。

### 2.3 安全性与抗预测性权衡分析（Predictability & Front-End Contract）
- **现状分析**：
  DSH 当前形态为 `S-{clusterPrefix}:{timestampMicro}`：
  - `clusterPrefix` 是由 `BASE_CLUSTER_OFFSET + (workerId * WORKER_ID_MULTIPLIER) + currentSeq` 构成的 9 位数字；
  - 冒号后为微秒时间戳。
- **兼容性约束**：
  前端 `src/volur-lite-view-NEXT/src/utils/flow.ts` 中的 `parseDshTimestamp` 强依赖冒号后为纯数字微秒并以此做本地会话过期判断，`flow-crypto.ts` 强依赖 `dsh` 进行信封加密。
- **结论与后续演进空间**：
  保持外部公开格式为 `S-{clusterPrefix}:{timestampMicro}`，不破坏既有协议与数据模型；本次升级解除了时间戳对 `currentSeq` 的直接信息泄露，未来可进一步在保持 9 位数的前提下引入可逆伪随机置换（PRP）对 `currentSeq` 进行混淆以进一步增强抗推测性。

---

## 3. 测试与验证

1. 新增全量单元测试 [`DeviceSessionHashGeneratorTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/session/generator/DeviceSessionHashGeneratorTest.java)：
   - 标准格式校验与 `workerId` 反解准确性验证；
   - 极限节点编号（`workerId = 1023`）验证；
   - 16 线程高并发生成无碰撞测试；
   - `isValid` 正反用例完备覆盖；
   - `extractWorkerId`、`extractTimestampMillis` 异常防御测试；
   - `isExpired` 超期判定逻辑覆盖；
2. 更新 [`SessionManagerTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/session/SessionManagerTest.java)；
3. 执行 Maven 全量构建：`mvn test -pl volur-auth -am` 全链路通过（9 个模块全绿，构建 100% 成功）。
