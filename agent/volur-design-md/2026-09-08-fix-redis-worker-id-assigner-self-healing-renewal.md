# Volur Lite 修复 RedisWorkerIdAssigner 休眠挂起后续约失效与死循环告警归档

- **执行日期**：2026-09-08
- **施工模块**：`volur-framework`
- **文档类型**：分布式 ID 分配器自愈与防休眠挂起续约优化

---

## 1. 问题背景与现象

在本地开发环境或宿主机器发生系统睡眠/休眠挂起（如笔记本合盖休眠 10~30 分钟）恢复后，后端控制台持续高频（每 10 秒）抛出以下警告日志：

```text
2026-09-08T10:38:34.984+08:00  WARN 24440 --- [volur-server] [iCP:housekeeper] com.zaxxer.hikari.pool.HikariPool        : VolurHikariCP - Thread starvation or clock leap detected (housekeeper delta=14m41s705ms).
2026-09-08T10:38:43.351+08:00  WARN 24440 --- [volur-server] [   scheduling-1] c.h.v.f.id.worker.RedisWorkerIdAssigner  : [RedisWorkerIdAssigner] Failed to renew WorkerID key [vredis:framework:snowflake_worker:volur-server:0], ownership lost
2026-09-08T10:38:53.353+08:00  WARN 24440 --- [volur-server] [   scheduling-1] c.h.v.f.id.worker.RedisWorkerIdAssigner  : [RedisWorkerIdAssigner] Failed to renew WorkerID key [vredis:framework:snowflake_worker:volur-server:0], ownership lost
```

### 根因分析：
1. **系统时钟跳跃与休眠挂起**：
   开发环境机器在合盖睡眠期间，操作系统挂起了 JVM 进程。恢复运行后，HikariCP 的 HouseKeeper 线程检测到实际时间差大幅跃迁（`housekeeper delta=14m41s`），输出 `Thread starvation or clock leap detected` 提示。
2. **WorkerID Key 自然过期被物理清除**：
   [`SnowflakeProperties`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/id/config/SnowflakeProperties.java) 中配置的 Worker ID TTL 默认仅为 30 秒（`workerTtlSeconds = 30L`）。当电脑休眠十几分钟后，Redis 中的 Key（如 `vredis:framework:snowflake_worker:volur-server:0`）已自然超时并被 Redis 物理删除。
3. **续期逻辑缺乏自愈机制导致死循环**：
   原有的 Lua 续约脚本仅判断 `if current == ARGV[1]` 则 `EXPIRE` 刷新 TTL；当 Key 已过期不存在时，`current` 为 `nil`，脚本返回 `0`。
   而在 Java 侧收到 `res == 0` 时，仅打印 `WARN` 日志，**没有尝试在无人占用的情况下重新占位（Re-acquire）**。
   导致唤醒后每隔 10 秒定时任务都会对一个不存在的 Key 执行续约并失败，陷入死循环刷屏，且当前实例在内存中继续以 `workerId = 0` 生成雪花 ID，在 Redis 中却处于无主状态，存在分布式并发下被其他新启动节点分配相同 ID 导致 ID 碰撞的隐患。

---

## 2. 修复方案

在 [`RedisWorkerIdAssigner.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/id/worker/RedisWorkerIdAssigner.java) 中实施多层防御与自愈重认领机制：

### 2.1 Lua 续期脚本三态原子判定升级
升级 `LUA_RENEW_SCRIPT`，增加对 Key 自然过期状态的原子恢复逻辑：
- **状态 1（正常续期）**：Key 存在且归属于当前实例 (`current == ARGV[1]`)，直接调用 `EXPIRE` 刷新 TTL，返回 `1`；
- **状态 2（无冲突自愈认领）**：Key 已自然过期但暂无其他节点占用 (`not current`)，原子执行 `SET key instanceId EX ttl` 重新占领 Worker ID，返回 `2`；
- **状态 3（真实冲突丢失所有权）**：Key 已被其他实例抢占 (`current ~= ARGV[1]`)，不覆盖，返回 `0`。

```lua
local current = redis.call('GET', KEYS[1])
if current == ARGV[1] then
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
    return 1
elseif not current then
    redis.call('SET', KEYS[1], ARGV[1], 'EX', tonumber(ARGV[2]))
    return 2
else
    return 0
end
```

### 2.2 Java 端响应与日志分级优化
1. **状态 2 自愈**：记录 `INFO` 级日志，告知开发者已在暂停/休眠后无冲突恢复 Worker ID，后续续约自动回归状态 1；
2. **状态 3 冲突**：记录 `ERROR` 级高危告警，明确提示可能存在的分布式雪花 ID 碰撞风险；
3. **空指针安全防护**：对 initial allocation 与 renewal 中的执行返回值均增加 `!= null` 防护，杜绝网络断开或管道回退时的装箱空指针异常。

### 2.3 增加单元测试覆盖
在 [`RedisWorkerIdAssignerTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/id/worker/RedisWorkerIdAssignerTest.java) 中添加 Mockito 单元测试，分别覆盖：
- Lua 初始分配成功与缓存读取；
- Redis 异常时的 Hash 兜底逻辑；
- 状态 1 正常续期；
- 状态 2 过期/休眠后的自愈恢复；
- 状态 3 所有权冲突告警；
- `stop()` 关机生命周期释放。

---

## 3. 验证结果

执行 `volur-framework` 模块单元测试：
```bash
mvn test -pl volur-framework
```

运行结果：
```text
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.194 s -- in com.humolix.volur.framework.id.worker.RedisWorkerIdAssignerTest
[INFO] Results:
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
全模块 20 个测试用例全部通过，零失败零报错。
