# Volur Lite 修复 RedisWorkerIdAssigner 关机钩子 Lettuce 已停止异常归档

- **执行日期**：2026-08-26
- **施工模块**：`volur-framework`
- **文档类型**：生命周期管理与优雅停机异常修复

---

## 1. 问题背景

在应用关闭/停机过程中（`SpringApplicationShutdownHook` 执行时），控制台偶现以下告警日志：
```text
WARN --- [ionShutdownHook] c.h.v.f.id.worker.RedisWorkerIdAssigner : [RedisWorkerIdAssigner] Failed to release WorkerID key [...]
java.lang.IllegalStateException: LettuceConnectionFactory has been STOPPED. Use start() to initialize it
```

### 根因分析：
1. Spring Boot 停机时，各个单例 Bean 的销毁顺序由 Spring 容器管理；
2. 若 `LettuceConnectionFactory` 先于 `RedisWorkerIdAssigner` 销毁并进入 `STOPPED` 状态，后续 `@PreDestroy` 执行 Redis Lua 脚本释放 WorkerID Key 时便会抛出 `IllegalStateException: LettuceConnectionFactory has been STOPPED`；
3. 实际上，WorkerID Key 在 Redis 中均配置了有效 TTL（默认 30s~60s），停机时即使未显式主动 DEL，Key 也会在超期后自动过期回收，无需打印高危 WARN 堆栈。

---

## 2. 修复方案

在 [`RedisWorkerIdAssigner.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/id/worker/RedisWorkerIdAssigner.java) 中采取多层防御与生命周期协同控制：

1. **接入 `SmartLifecycle` 优雅停机协议**：
   - 实现 `SmartLifecycle` 接口，设置 `getPhase() = Integer.MAX_VALUE - 1000`；
   - 在 Spring 容器销毁单例（包括关闭 Redis 连接工厂）之前，优先触发 `SmartLifecycle.stop()` 提前完成 Redis WorkerID 的主动释放；
2. **显式声明 Bean 依赖顺序**：
   - 标注 `@DependsOn("stringRedisTemplate")`，指示 Spring 确保 RedisTemplate 存活周期长于 WorkerID 分配器；
3. **连接工厂状态前置核验**：
   - 执行释放脚本前，检查 `redisTemplate.getConnectionFactory()` 是否属于 `Lifecycle` 且 `!lifecycle.isRunning()`，若已停止则安全跳过，避免无效调用；
4. **关机异常安全降级与幂等保护**：
   - 重置本地状态 `allocatedWorkerId = null` 杜绝重复释放；
   - 捕获 `IllegalStateException`，转为 DEBUG 级别记录并提示由 Redis TTL 自动兜底回收，消除令人困惑的 WARN 报警。

---

## 3. 验证结果

执行全平台 Maven 编译：
```bash
mvn clean compile -DskipTests
```
全平台 12 个子模块全部构建成功（`BUILD SUCCESS`）。
