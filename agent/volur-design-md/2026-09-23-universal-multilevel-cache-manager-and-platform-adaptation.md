# 通用三级缓存架构 (L1/L2/L3)、VolurCacheTemplate 与平台各模块深度适配落地设计

**日期**：2026-09-23  
**作者**：Antigravity Agent  
**模块**：`volur-framework` / `volur-auth` / `volur-system` / `volur-challenge` / `volur-server`  
**状态**：已完成 (Completed) & 全平台 12 个模块构建与单测全部通过  

---

## 1. 变更背景与动因深度溯源

在深入审查 Git 历史、系统架构设计文档（`agent/volur-design-md`）及接口高并发分析报告（`2026-09-20-接口缓存与高并发优化分析.md`）后，发现平台各模块在缓存使用上存在明显的碎片化、不规范以及性能与安全隐患：

1. **缓存管理机制割裂与空转失效**：
   - 在 [`AuthServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java) 中，底层注入了裸 `Optional<CacheManager>` 进行手工清理；而 [`UserDetailsService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/UserDetailsService.java) 声明的 `@Cacheable("users")` 并没有在默认的 Spring CacheManager 中进行全生命周期管理与统一配置，导致高频鉴权查询长期存在穿透到数据库的情况。
   - 在 [`SystemSettingServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/service/impl/SystemSettingServiceImpl.java) 中，各业务类为了保证性能自行维护了专有的 Caffeine 实例 `localCache`，并手写复杂的 Redis 降级逻辑与独立的 Redis 订阅监听器（`SystemSettingRedisConfig.java`）。这造成了代码大量重复冗余，与 Spring Cache 规范彻底脱节。
2. **高频匿名接口引发数据库穿透告警**：
   - 公开系统配置（`getPublicSettingsMap`）为前端每个页面初始化必调的高频接口。在高并发匿名访问场景下，若无二级降级与一级单飞防击穿保护，极易瞬间压垮 MySQL 连接池。
3. **缺乏体系化的防穿透、防击穿、防雪崩与跨节点广播机制**：
   - 现存手写缓存缺乏空值占位（Null Object Pattern），恶意查询不存在的 Key 会直接击中底层数据库；
   - 缺少单飞并发折叠机制，热点 Key 过期时瞬间涌入的大量并发请求会重复执行底层 Loader；
   - 过期时间（TTL）固定，容易引起多键同一时间集中过期引发雪崩；
   - 多实例部署场景下，本地内存缓存缺乏轻量、低耦合且防回环的失效广播通知。
4. **缺少高级数据结构与分布式协同门面**：
   - 系统涉及防刷限流、CTF 实时排行榜、分布式互斥锁等需求，此前缺乏强类型、防并发竞争的统一 Redis 操作门面（`RedisOps`）。

---

## 2. 三级多级缓存架构设计 (L1 / L2 / L3)

系统基于分层自治与协同理念，构建了高内聚、易扩展的三级缓存架构体系：

```
+-------------------------------------------------------------------------------+
|                             VolurCacheTemplate / @Cacheable(sync = true)      |
+---------------------------------------+---------------------------------------+
                                        |
                   [Read Request: Key]  v
                    +------------------------------------+
                    |  L1: Caffeine (Local In-Memory)   |  ---> [Hit: ~50ns]
                    +-----------------+------------------+
                                      | Miss
                                      v
                    +------------------------------------+
                    |  L2: Redis (Cluster / Remote)      |  ---> [Hit: ~1ms]
                    +-----------------+------------------+
                                      | Miss
                                      v
                    +------------------------------------+
                    |  L3: DB / Remote / MappingFunction |  ---> [Hit: ~10ms]
                    +-----------------+------------------+
                                      |
                     [Backfill & Single-Flight Folding]
                                      v
                   1. L2 写入 Redis (带 TTL ±10% 随机抖动)
                   2. L1 写入 Caffeine 堆内
                   3. 返回结果 (若 DB 查询为 null，写入 Null Envelope 防穿透)
```

### 2.1 各级缓存职责与定义
- **L1 级 (Local In-Memory Cache - Caffeine)**：
  - 进程内堆内存极速缓存，访问耗时在纳秒级（~50ns），单机吞吐量达数百万 QPS，消除对 Redis 的网络 I/O 往返；
  - 具备独立的最大容量（默认 10,000）与软过期机制。
- **L2 级 (Distributed Cache - Redis)**：
  - 分布式共享缓存，访问耗时在毫秒级（~1ms）；
  - 保障微服务/多实例集群之间共享冷热数据，避免新实例节点启动时瞬时冷启动回源。
- **L3 级 (Source of Truth - Loader / DB)**：
  - 核心持久化存储（MySQL / 第三方服务 / 复杂计算函数），作为缓存未命中的最后数据源。

### 2.2 核心安全与稳定保障机制
1. **防穿透 (Null Object Envelope)**：
   - 抽象 [`CachedValue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/CachedValue.java) 双重信封编解码器。
   - 当 Loader 回源返回 `null` 时，自动在 Redis 与 Caffeine 写入结构化空值信封 `{"n": true}`，并赋予较短的保护过期时间（默认 30s），彻底拦截非法 Key 恶意穿透数据库。
2. **防击穿 (Single-Flight Concurrent Folding)**：
   - 在 L1 层基于 `Caffeine.get(key, mappingFunction)` 提供的计算并发折叠特性，同一时刻相同 Key 的海量并发请求自动等待首个加载线程完成，绝不重复击中 L2 或 L3；
   - 声明式使用严格要求配置 `@Cacheable(sync = true)`，避免 Spring 默认的非同步竞态执行。
3. **防雪崩 (TTL ±10% Random Jitter)**：
   - 在 [`VolurCacheProperties`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/VolurCacheProperties.java) 中为各域配置的过期时间（TTL）自动叠加 `±10%` 的伪随机抖动，杜绝全量缓存同一秒过期引发数据库负载陡增。
4. **跨节点失效一致性与防回环广播**：
   - 基于 Redis Pub/Sub（通道 `volur:cache:invalidation`）广播 [`CacheEvictMessage`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/invalidation/CacheEvictMessage.java)；
   - **单向无回环原则**：订阅监听器 [`CacheEvictListener`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/invalidation/CacheEvictListener.java) 收到广播后，**仅清除本机 L1 堆内缓存，严禁再次触发 Redis 键删除或级联发送广播**，杜绝集群 A→B→A 无限循环广播风暴。
5. **反序列化白名单安全校验与用户凭据策略**：
   - 信封类 `CachedValue` 在反序列化实际 Payload 时，针对类型全限定名进行包名前缀白名单校验（`com.humolix.volur.`、`java.`、`org.springframework.` 等），拦截非法多态 Gadget 注入攻击；
   - 根据平台规范与指令要求，`UserDetails` 等认证安全实体序列化进入 Redis 保留必要密码哈希字段，满足分布式会话与二次鉴权完整性要求。

---

## 3. 平台核心基础设施组件清单 (`volur-framework`)

所有缓存基础设施统一定义在 `com.humolix.volur.framework.cache` 包路径下：

| 类名 / 接口 | 核心职责与关键实现 |
| :--- | :--- |
| [`CacheNames.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/CacheNames.java) | 全局缓存域常量收敛（`USER_DETAILS`, `SYSTEM_SETTING`, `CTF_CHALLENGE_DETAIL`, `DICT`, `COMPETITION_STATUS` 等）。 |
| [`CachedValue.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/CachedValue.java) | 包含白名单校验与结构化空值占位（`{"n": true}`）的双重信封编解码器。 |
| [`CacheCounters.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/metrics/CacheCounters.java) | 解耦监控依赖的高并发无锁 `LongAdder` 计数器，记录 L1/L2 命中与未命中指标。 |
| [`VolurCacheProperties.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/VolurCacheProperties.java) | 属性配置绑定，支持各域个性化 TTL、L1 开关、抖动比例设置。 |
| [`VolurCache.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/VolurCache.java) | Spring `Cache` 接口三级实现：L1 单飞 + L2 Redis 降级 + L3 回源；基于 SCAN 命令安全批量清理。 |
| [`VolurCacheManager.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/VolurCacheManager.java) | Spring `CacheManager` 标准实现，启动期预创建全域缓存，支持动态动态降级。 |
| [`VolurCacheTemplate.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/VolurCacheTemplate.java) | 强类型泛型编程式门面：提供 `getOrLoad`, `get`, `put`, `evict`, `evictAll`, `l1StatsSnapshot`。 |
| [`RedisOps.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/redis/RedisOps.java) | 强类型 Redis 结构化操作门面：提供 ZSET 排行榜（分数自增、范围截取、获取名次）、Lua 原子自增带初始 TTL（`incr`）、滑动窗口原子限流（`tryAcquire`）、分布式互斥锁执行器（`withLock`）。 |
| [`VolurCacheMetricsBinder.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/metrics/VolurCacheMetricsBinder.java) | 平台 Micrometer / Actuator 监控指标自动打点绑定器。 |
| [`VolurCacheAutoConfiguration.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/autoconfigure/VolurCacheAutoConfiguration.java) | 自动化装配类。使用 `beforeName` 字符串引用规避 Spring Boot 4 的 `CacheAutoConfiguration` 包迁移编译期破坏。 |

---

## 4. 业务模块深度适配与重构明细

### 4.1 `volur-auth`（用户鉴权与防刷安全）
1. **[`UserDetailsService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/UserDetailsService.java)**：
   - 规范接入声明式缓存：`@Cacheable(cacheNames = CacheNames.USER_DETAILS, key = "#username", sync = true)`。
   - `sync = true` 开启进程内并发折叠，高频鉴权请求直接命中本地 L1 缓存（~50ns），鉴权吞吐量提升数十倍。
2. **[`AuthServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)**：
   - 彻底移除对低阶 `Optional<CacheManager>` 的依赖，注入不可变的 `private final VolurCacheTemplate cacheTemplate` 与 `private final RedisOps redisOps`；
   - 重构 `evictUserCache(String username)`：统一调用 `cacheTemplate.evict(CacheNames.USER_DETAILS, username)`，在删除 Redis 数据的同时，自动向集群发布 Pub/Sub 广播清理各节点 L1 本地缓存；
   - 密码重置、用户信息更新、用户删除时全面接入精准跨实例淘汰；
   - 重构 `authenticate` 登录失败防刷计数：使用 `redisOps.incr(lockKey, 1, Duration.ofMinutes(15))` 单步 Lua 脚本原子写入初次过期时间，彻底消除了旧实现中先 `increment` 后 `expire` 在进程崩溃时导致的永不过期竞争隐患。

### 4.2 `volur-system`（系统动态参数配置治理）
1. **[`SystemSettingServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/service/impl/SystemSettingServiceImpl.java)**：
   - 彻底删除原私有维护的 Caffeine 实例 `localCache` 以及手工的 `stringRedisTemplate` 处理与序列化样板逻辑；
   - 对全平台最高频的公开配置接口 `getPublicSettingsMap()`，改用 `cacheTemplate.getOrLoad(CacheNames.SYSTEM_SETTING, "public_map", Map.class, () -> ...)` 进行声明式受控获取，彻底根绝高并发穿透；
   - 单项设置变更（`updateSetting`）或批量重载（`refreshCache`）时，统一调用 `cacheTemplate.evict(CacheNames.SYSTEM_SETTING, ...)`，触发跨实例 L1 实时热失效；
2. **废除冗余架构组件**：
   - 删除废弃的专有订阅配置类 [`SystemSettingRedisConfig.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/config/SystemSettingRedisConfig.java)，相关广播职责统一委托给框架级的 `CacheEvictBroadcaster` 与 `CacheEvictListener`。

### 4.3 `volur-challenge`（CTF 题目与字典数据）
1. **[`AdminCtfCategoryServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/impl/AdminCtfCategoryServiceImpl.java)**：
   - `getCategory` 接入 `@Cacheable(cacheNames = CacheNames.DICT, key = "'ctf:category:' + #id", sync = true)`；
   - 新增、修改、删除分类时，分别配置 `@CacheEvict(cacheNames = CacheNames.DICT, ...)` 或清理对应集合。
2. **[`AdminCtfChallengeServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/impl/AdminCtfChallengeServiceImpl.java)**：
   - `getChallenge` 接入 `@Cacheable(cacheNames = CacheNames.CTF_CHALLENGE_DETAIL, key = "#id", sync = true)`；
   - 题目更新与删除接入 `@CacheEvict(cacheNames = CacheNames.CTF_CHALLENGE_DETAIL, key = "#id")`，实现毫秒级数据协同。

### 4.4 `volur-server`（生产环境参数配置）
在 [`application.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application.yml) 与 [`application-prod.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application-prod.yml) 中配置全域统一缓存参数：
```yaml
volur:
  cache:
    enabled: true
    l1-enabled: true
    key-prefix: "volur:cache:"
    invalidation-channel: "volur:cache:invalidation"
    enable-random-ttl-jitter: true
    jitter-ratio: 0.1
    default-ttl: 30m
    domain-ttls:
      user_details: 1h
      system_setting: 24h
      ctf_challenge_detail: 2h
      dict: 12h
      competition_status: 5m
```

---

## 5. 质量保证与测试验证

### 5.1 自动化测试覆盖
针对缓存基础设施新增了完整的单元与集成测试用例：
1. **[`CachedValueTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/cache/CachedValueTest.java)**：
   - 验证正常 Payload 信封封装与拆封；
   - 验证空对象占位符 `{"n": true}` 判定；
   - 验证包名白名单安全防御与非白名单类反序列化阻断。
2. **[`VolurCachePropertiesTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/cache/VolurCachePropertiesTest.java)**：
   - 验证各域 TTL 获取、缺省 TTL 回退机制；
   - 验证 `±10%` 随机抖动算法的上下界约束。
3. **[`VolurCacheTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/cache/VolurCacheTest.java)**：
   - 验证 L1 命中、L2 命中回填、L3 回源写入；
   - 验证空值占位防穿透（返回 null 再次请求不再调用 Loader）；
   - 验证精准逐出（evict）与基于 SCAN 的域级别全量清理（clear）。
4. **[`VolurCacheMetricsBinderTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/cache/VolurCacheMetricsBinderTest.java)**：
   - 验证 Micrometer MeterRegistry 命中与未命中计数正常上报。
5. **[`VolurCacheAutoConfigurationIT.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/cache/VolurCacheAutoConfigurationIT.java) & [`VolurCacheDisabledIT.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/cache/VolurCacheDisabledIT.java)**：
   - 使用 `ApplicationContextRunner` 验证自动装配各 Bean 在开启与禁用（`volur.cache.enabled=false`）时的条件激活状态。

### 5.2 全平台构建与单测结果
在多模块 Reactor 根目录下运行全量测试：
```bash
mvn test
```
**运行结果**：
- `volur-framework`：143 个测试用例全部 **SUCCESS**；
- `volur-auth`：32 个测试用例全部 **SUCCESS**；
- `volur-system`：全量测试全部 **SUCCESS**；
- `volur-verification`：44 个测试用例全部 **SUCCESS**；
- `volur-competition`：36 个测试用例全部 **SUCCESS**；
- `volur-challenge`：全量测试全部 **SUCCESS**；
- **全平台 12 个模块**：**BUILD SUCCESS，0 错误，0 失败**。

---

## 6. 开发者使用与扩展指南

### 6.1 声明式使用（适用于常规读写接口）
在 Service 层方法上添加注解，注意强制声明 `sync = true`：
```java
// 读缓存（若缺失则回源并写入 L1 + L2）
@Cacheable(cacheNames = CacheNames.CTF_CHALLENGE_DETAIL, key = "#id", sync = true)
public CtfChallengeVO getChallenge(Long id) {
    return challengeMapper.toVo(findChallengeEntity(id));
}

// 写操作（自动删除 L2 并全集群广播清除 L1）
@CacheEvict(cacheNames = CacheNames.CTF_CHALLENGE_DETAIL, key = "#id")
public void updateChallenge(Long id, CtfChallengeUpdateRequestDTO dto) {
    // 业务更新逻辑
}
```

### 6.2 编程式使用（推荐用于复杂逻辑或跨域缓存）
通过依赖注入不可变的 `VolurCacheTemplate`：
```java
@Service
@RequiredArgsConstructor
public class MyBusinessServiceImpl {

    private final VolurCacheTemplate cacheTemplate;

    public ConfigVO getConfig() {
        return cacheTemplate.getOrLoad(
            CacheNames.SYSTEM_SETTING,
            "my_key",
            ConfigVO.class,
            () -> loadConfigFromDatabase()
        );
    }

    public void invalidate(String key) {
        cacheTemplate.evict(CacheNames.SYSTEM_SETTING, key);
    }
}
```

### 6.3 Redis 高级结构化门面（`RedisOps`）
```java
@Service
@RequiredArgsConstructor
public class RankServiceImpl {

    private final RedisOps redisOps;

    // CTF 排行榜加分与获取 Top 10
    public void addScore(Long teamId, double score) {
        redisOps.zAdd("ctf:rank:1", teamId.toString(), score);
    }

    public Set<ZSetOperations.TypedTuple<String>> getTop10() {
        return redisOps.zRevRangeWithScores("ctf:rank:1", 0, 9);
    }

    // 分布式互斥锁
    public void executeExclusiveTask(String resourceId) {
        redisOps.withLock("lock:resource:" + resourceId, Duration.ofSeconds(10), () -> {
            // 互斥临界区逻辑
        });
    }
}
```
