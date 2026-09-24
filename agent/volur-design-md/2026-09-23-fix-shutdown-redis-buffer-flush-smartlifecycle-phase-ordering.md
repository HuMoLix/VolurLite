# 修复服务优雅停机时 Redis 日志缓冲区收尾抛出 LettuceConnectionFactory STOPPED 异常

## 1. 变更背景与问题溯源

在平台服务触发优雅停机（`SpringApplicationShutdownHook`）时，后台日志记录与竞赛操作日志持久化服务抛出如下严重异常：

```
2026-09-23T05:47:15.246+08:00  INFO 77960 --- [volur-server] [ionShutdownHook] c.h.v.l.s.i.SyslogPersistenceServiceImpl : [Syslog] 服务优雅停机，正在执行 Redis 系统日志缓冲区收尾落库...
2026-09-23T05:47:15.246+08:00 ERROR 77960 --- [volur-server] [ionShutdownHook] c.h.v.l.s.i.SyslogPersistenceServiceImpl : [Syslog] 消费 Redis 日志缓冲区批量入库异常: LettuceConnectionFactory has been STOPPED. Use start() to initialize it
2026-09-23T05:47:15.246+08:00  INFO 77960 --- [volur-server] [ionShutdownHook] s.i.CompetitionLogPersistenceServiceImpl : [CompetitionLog] 服务优雅停机，正在执行 Redis 日志缓冲区收尾落库...
2026-09-23T05:47:15.247+08:00 ERROR 77960 --- [volur-server] [ionShutdownHook] s.i.CompetitionLogPersistenceServiceImpl : [CompetitionLog] 消费 Redis 日志缓冲区批量入库异常: LettuceConnectionFactory has been STOPPED. Use start() to initialize it
```

### 根本原因分析
1. **Spring 容器停机阶段执行顺序**：
   在 Spring 停机阶段（`AbstractApplicationContext.doClose()`）：
   - **阶段 1**：发布 `ContextClosedEvent` 事件；
   - **阶段 2**：调用 `LifecycleProcessor.onClose()`，按照 `SmartLifecycle.getPhase()` 的降序关闭所有生命周期 Bean；
   - **阶段 3**：调用 `destroySingletons()` 销毁单例 Bean，此时才执行 `@PreDestroy` 注解或 `DisposableBean.destroy()` 回调。
2. **LettuceConnectionFactory 的生命周期定位**：
   - Spring Data Redis 的 `LettuceConnectionFactory` 实现了 `SmartLifecycle` 接口，默认 `phase = 0`；
   - 因此在 **阶段 2** 中，`LettuceConnectionFactory.stop()` 已被优先执行，其内部连接池被重置并释放，状态原子标记变为 `STOPPED`。
3. **`@PreDestroy` 的执行滞后冲突**：
   - 原实现中，`SyslogPersistenceServiceImpl` 与 `CompetitionLogPersistenceServiceImpl` 仅依靠 `@PreDestroy` 进行收尾；
   - 该注解在 **阶段 3** 才被触发。此时 `drainBuffer()` 试图调用 `stringRedisTemplate.opsForList().leftPop()`，连接工厂断言 `assertStarted()` 失败，导致收尾落库无法完成。

---

## 2. 架构重构与改造说明

为确保在 Redis 连接工厂被关闭前完成缓冲区数据的完全排空与落库，将两个日志持久化服务的停机生命周期提升至 **阶段 2（`SmartLifecycle`）** 并分配高优先级 `Phase`。

### 2.1 改造详情

1. **实现 `org.springframework.context.SmartLifecycle` 接口**：
   - 实现了 `start()`、`stop()`、`isRunning()` 和 `getPhase()`；
   - 内部维护 `private volatile boolean running` 状态标记；
   - 在 `stop()` 中受控执行缓冲区排空与落库 `drainBuffer()`，落库完成后将 `running` 置为 `false`，支持幂等防护。
2. **生命周期阶段分配（Phase Ordering）**：
   - 重写 `getPhase()` 返回 `Integer.MAX_VALUE - 1000`（与平台雪花节点释放器 `RedisWorkerIdAssigner` 保持对齐）；
   - 在 Spring 停机第二阶段中，`SmartLifecycle` 按 Phase 降序执行 `stop()`。`Integer.MAX_VALUE - 1000` 显著高于 `LettuceConnectionFactory` 的 `phase = 0`，确保收尾落库时 Redis 连接工厂和底层连接处于正常活跃（STARTED）状态。
3. **清理旧注销机制**：
   - 移除原类上的 `@PreDestroy` 注解方法，彻底消除重复执行风险。

---

## 3. 改动代码清单

- [`SyslogPersistenceServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-logging/src/main/java/com/humolix/volur/logging/service/impl/SyslogPersistenceServiceImpl.java)：
  - 继承 `SmartLifecycle` 接口；
  - 移除 `@PreDestroy onShutdown`；
  - 接入 `start()`, `stop()`, `isRunning()`, `getPhase() = Integer.MAX_VALUE - 1000`。
- [`CompetitionLogPersistenceServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionLogPersistenceServiceImpl.java)：
  - 继承 `SmartLifecycle` 接口；
  - 移除 `@PreDestroy onShutdown`；
  - 接入 `start()`, `stop()`, `isRunning()`, `getPhase() = Integer.MAX_VALUE - 1000`。

---

## 4. 影响与回归验证

1. **编译与自动化测试**：
   - 执行 `mvn compile -pl volur-logging,volur-competition -am` 编译通过（11 个模块 Reactor 构建 SUCCESS）；
   - 执行 `mvn test -pl volur-logging,volur-competition`，43 个单元测试全部通过（0 失败，0 错误）。
2. **服务停机收尾能力保障**：
   - 彻底解决服务平滑重启/关闭时丢弃 Redis 缓冲区残留日志的问题；
   - 日志批量入库完成后，Redis 连接池按正常时序优雅注销。
