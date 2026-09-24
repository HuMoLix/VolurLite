# 修复 RiskEvaluationEngine 多构造器导致 Spring Bean 实例化失败缺陷施工记录

## 1. 问题背景与异常日志审计

### 1.1 异常现场
在运行 `VolurLiteApplication.main` 启动后端服务时，Spring 容器初始化阶段抛出如下异常，服务启动崩溃退出（退出代码 1）：

```text
org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'captchaController' defined in file [.../com/humolix/volur/verification/controller/v1/CaptchaController.class]: Unsatisfied dependency expressed through constructor parameter 0:
Error creating bean with name 'captchaServiceImpl' defined in file [.../com/humolix/volur/verification/service/impl/CaptchaServiceImpl.class]: Unsatisfied dependency expressed through constructor parameter 1:
Error creating bean with name 'riskEvaluationEngine' defined in file [.../com/humolix/volur/verification/risk/engine/RiskEvaluationEngine.class]: Failed to instantiate [com.humolix.volur.verification.risk.engine.RiskEvaluationEngine]: No default constructor found
...
Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [com.humolix.volur.verification.risk.engine.RiskEvaluationEngine]: No default constructor found
	at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:102)
Caused by: java.lang.NoSuchMethodException: com.humolix.volur.verification.risk.engine.RiskEvaluationEngine.<init>()
```

### 1.2 溯源分析与责任变更定位
通过审计 `agent/volur-design-md` 以及 Git 变更集：
1. **致错审计文档**：
   - [`agent/volur-design-md/2026-09-22-risk-ip-governance-and-admin-risk-controller.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-md/2026-09-22-risk-ip-governance-and-admin-risk-controller.md)（《平台智能风控体系升维、基于 IP 封禁治理机制与 AdminRiskController 施工审计文档》）。
2. **致错代码改动**：
   - 在为风控引擎增加动态阈值读取（`SystemSettingService systemSettingService`）及失败超限自动封禁事件发布（`ApplicationEventPublisher eventPublisher`）时，类中新增了两个 `private final` 依赖字段。
   - 类上方声明了 Lombok `@RequiredArgsConstructor` 注解，因此 Lombok 会自动为该类生成 4 参数构造器：
     `public RiskEvaluationEngine(RedisSlidingWindowRateLimiter, StringRedisTemplate, SystemSettingService, ApplicationEventPublisher)`。
   - 然而，为了使历史既有测试用例（如 `CaptchaRiskFlowTest` 和 `SlidePuzzleEngineTest` 中直接使用 2 参数 `new RiskEvaluationEngine(rateLimiter, redisTemplate)`）无需修改即可通过编译，开发者在 [`RiskEvaluationEngine.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/engine/RiskEvaluationEngine.java) 中手动增加了一个重载构造器：
     ```java
     public RiskEvaluationEngine(RedisSlidingWindowRateLimiter rateLimiter, StringRedisTemplate redisTemplate) {
         this(rateLimiter, redisTemplate, null, null);
     }
     ```
3. **根本机制原因**：
   - **违反规范**：这直接违反了 [`agent/volur-design-skills/Rules.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-skills/Rules.md) 第 1.1 节规范：“**消除样板构造器**：禁止手动编写用于依赖注入的构造函数；若类中已使用 `@RequiredArgsConstructor`，必须移除所有冗余的手写构造器”。
   - **Spring 依赖注入推断机制冲突**：
     - 若一个 Spring Bean 只有一个构造函数，Spring 4.3+ 会自动推断并使用该唯一定义的构造函数进行依赖注入（无需 `@Autowired`）；
     - 但一旦一个 Bean 拥有多个构造函数且**均未显式标注 `@Autowired`** 时，Spring 无法判定应当使用哪一个构造函数，因此会降级回退去调用**默认无参构造函数**（`<init>()`）；
     - 由于 `RiskEvaluationEngine` 没有任何无参构造器，Spring 最终抛出 `NoSuchMethodException: ...RiskEvaluationEngine.<init>()`，导致依赖它的 `CaptchaServiceImpl` 和 `CaptchaController` 发生级联初始化失败。
   - **为什么先前单元测试未拦截**：`volur-verification` 中的所有单元测试均为基于 Mockito 的轻量级单测，未拉起完整 Spring 上下文（无 `@SpringBootTest`），因此本地执行 `mvn test -pl volur-verification` 能全部通过，只有在真实启动 Application 主程序时暴露。

---

## 2. 修复实施清单

### 2.1 移除冗余手写构造函数
在 [`RiskEvaluationEngine.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/engine/RiskEvaluationEngine.java) 中彻底移除手写的 2 参数构造器：
```java
// 移除该冗余手写构造器，仅保留 @RequiredArgsConstructor 生成的唯一全参构造器
// public RiskEvaluationEngine(RedisSlidingWindowRateLimiter rateLimiter, StringRedisTemplate redisTemplate) {
//     this(rateLimiter, redisTemplate, null, null);
// }
```
使 `RiskEvaluationEngine` 仅保留由 Lombok `@RequiredArgsConstructor` 自动生成的全参构造器，恢复 Spring 单一构造器自动依赖注入规范。

### 2.2 测试用例构造参数对齐
由于 `RiskEvaluationEngine` 内部在读取设置和发布事件时已做了完备的空安全防护（`systemSettingService == null` 时安全回退默认值，`eventPublisher == null` 时不发布事件），因此将既有测试用例中的调用统一对齐为 4 参数调用：
1. 在 [`CaptchaRiskFlowTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/CaptchaRiskFlowTest.java) 中：
   `riskEngine = new RiskEvaluationEngine(rateLimiter, redisTemplate, null, null);`
2. 在 [`SlidePuzzleEngineTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/SlidePuzzleEngineTest.java) 中：
   `RiskEvaluationEngine riskEngine = new RiskEvaluationEngine(null, null, null, null);`

---

## 3. 验证结果

1. 执行 `mvn test -pl volur-verification`：
   - 29 个单元测试全部通过（Failures: 0, Errors: 0, Skipped: 0）。
2. 执行 `mvn test -pl volur-auth -Dtest=AuthVerificationCrossValidationTest`：
   - 7 个跨模块验证测试全部通过。
3. 执行 `mvn test-compile`：
   - 全平台所有 12 个模块编译成功（BUILD SUCCESS）。
