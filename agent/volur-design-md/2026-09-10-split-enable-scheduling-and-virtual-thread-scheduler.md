# 主类解耦拆分与 Java 25 虚拟线程定时调度器施工报告

## 1. 变更背景与需求动因
在 Volur Lite 架构演进中，主启动类 [`VolurLiteApplication.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-server/src/main/java/com/humolix/volur/VolurLiteApplication.java) 曾直接声明 `@EnableScheduling` 注解。

默认情况下，Spring 的定时调度机制存在严重的安全隐患与性能缺陷：
1. **默认单线程调度缺陷 (Single-Thread Bottleneck)**：
   - Spring 默认的 `TaskScheduler` 底层为单线程调度（`poolSize = 1`）；
   - 在平台运行中，诸如 `CtfInstanceServiceImpl.autoCleanExpiredInstances`（清理过期容器，涉及 Docker API 与网络 I/O）、`GlobalSseManager.sendHeartbeat`（SSE 心跳检测）以及 `RedisWorkerIdAssigner.heartbeat`（雪花算法工作节点心跳）等多个高频/核心定时任务并发运行；
   - 一旦任意一个定时任务因网络抖动、Docker 假死或慢 SQL 卡死，**后续所有其他定时任务都会被无限期排队阻塞**，极易引发 Snowflake Worker ID 续约超时断号、客户端 SSE 掉线等连锁系统级事故。
2. **启动类职责过载**：
   - 主启动类应当保持极简微内核规范，仅负责引导 Spring Boot 应用上下文，所有定时任务基础设施配置应当收敛下沉至 [`volur-framework`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework)。

为彻底消除定时任务卡死导致全局停滞的严重风险，本次针对 `@EnableScheduling` 展开专项拆分与架构升级。

---

## 2. 核心架构设计与改动明细

```
                         ┌──────────────────────────────────────────────┐
                         │ Spring @Scheduled 触发源 (CRON / FixedDelay)  │
                         └──────────────────────┬───────────────────────┘
                                                │
                                                ▼
┌───────────────────────────────────────────────────────────────────────────────────────────────┐
│ SchedulingConfig (实现 SchedulingConfigurer)                                                   │
│  - taskRegistrar.setTaskScheduler(volurTaskScheduler)                                         │
└───────────────────────────────────────┬───────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌───────────────────────────────────────────────────────────────────────────────────────────────┐
│ SimpleAsyncTaskScheduler (Spring 7 / Spring Boot 4)                                           │
│  - virtualThreads = true (Java 25 虚拟线程)                                                   │
│  - threadNamePrefix = "volur-sched-vt-"                                                       │
│  - targetTaskExecutor = volurThreadManager.getVirtualExecutor() (复用统一虚拟线程引擎)             │
│  - taskDecorator = AsyncContextTaskDecorator (自动补全与传播 traceId 上下文)                     │
│  - errorHandler = 捕获并规范记录异常堆栈，绝不中断调度器运行                                       │
└───────────────────────────────────────┬───────────────────────────────────────────────────────┘
                                        │
                   ┌────────────────────┴────────────────────┐
                   ▼                                         ▼
┌──────────────────────────────────────┐  ┌──────────────────────────────────────┐
│ Task A (如: 容器清理，阻塞 30s)        │  │ Task B (如: Redis WorkerID 心跳, 立即) │
│ - 运行在独立虚拟线程                     │  │ - 运行在独立虚拟线程                     │
│ - I/O 阻塞时自动挂起 (unmount) 物理载体 │  │ - 完全不受 Task A 阻塞影响，准时触发      │
└──────────────────────────────────────┘  └──────────────────────────────────────┘
```

### 2.1 变更模块与新建文件列表

| 模块 | 包路径 / 文件路径 | 类型 | 职责说明 |
| :--- | :--- | :--- | :--- |
| `volur-server` | [`VolurLiteApplication.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-server/src/main/java/com/humolix/volur/VolurLiteApplication.java) | 修改 | 移除 `@EnableScheduling` 注解与对应 import，实现启动类极简化 |
| `volur-framework` | [`SchedulingConfig.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/SchedulingConfig.java) | 新增 | 承载 `@EnableScheduling`，实现 `SchedulingConfigurer`，配置虚拟线程调度器 |
| `volur-framework` | [`AsyncProperties.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/thread/AsyncProperties.java) | 修改 | 扩展配置属性，增加 `scheduleThreadNamePrefix`（默认 `"volur-sched-vt-"`） |
| `volur-framework` | [`SchedulingConfigTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/common/config/SchedulingConfigTest.java) | 新增 | 单元测试：覆盖虚拟线程执行、长耗时非阻塞并发调度及 MDC traceId 传播验证 |

---

## 3. 关键技术实现解析

### 3.1 基于 Java 25 虚拟线程的 `SimpleAsyncTaskScheduler`
1. **彻底解决单线程串行卡死问题**：
   - 采用 Spring 7 原生的 `SimpleAsyncTaskScheduler`，开启 `setVirtualThreads(true)`；
   - 通过 `setTargetTaskExecutor(volurThreadManager.getVirtualExecutor())`，直接接入先前实现的统一高并发线程管理器；
   - 每次定时任务触发时均由独立的虚拟线程进行派发。当某一定时任务进行繁重 I/O（如向远程 Docker Daemon 请求超时）时，虚拟线程自动 unmount 释放物理核心，后续其他定时任务完全不受干扰，照常准时触发。
2. **全链路可追溯追踪与上下文传播**：
   - 挂载 `AsyncContextTaskDecorator`，保证定时任务在执行过程中自动生成或继承 `traceId`，日志体系保持高度统一；
3. **全局未捕获异常熔断防护**：
   - 注入专有 `ErrorHandler`，当定时任务执行中途抛出 `RuntimeException` 时，完整记录方法堆栈与错误明细，杜绝异常向上冒泡导致定时调度线程崩溃退出。

---

## 4. 验证与测试

1. **针对性单元测试**：
   - 运行 [`SchedulingConfigTest`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/common/config/SchedulingConfigTest.java)：
     - `testScheduledTaskRunsOnVirtualThread`：断言定时任务执行线程满足 `isVirtual() == true` 且前缀匹配；
     - `testLongRunningTaskDoesNotBlockSubsequentTasks`：模拟 Task 1 严重 I/O 阻塞 500ms，断言随后被调度的 Task 2 完全不被阻塞并优先执行完成（验证非阻塞并发调度）；
     - `testScheduledTaskMdcTraceIdGeneration`：验证定时任务线程自动具备 `traceId` 追踪上下文；
   - **3/3 全部通过，0 失败**。
2. **全工程多模块集成编译**：
   - 针对平台 12 个 Maven 模块执行 `mvn test-compile`，全量工程编译构建成功（Reactor Summary: `SUCCESS`）。
