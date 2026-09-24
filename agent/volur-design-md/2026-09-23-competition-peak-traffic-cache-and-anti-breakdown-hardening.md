# 竞赛开赛瞬间高并发防击穿、动静分离三级缓存与容器防重分布式锁落地审计

**日期**：2026-09-23  
**作者**：Antigravity Agent  
**模块**：`volur-competition` / `volur-framework`  
**状态**：已完成 (Completed) & 全平台 12 个模块全量单测全部通过  

---

## 1. 变更背景与高并发痛点深度溯源

在网络安全竞赛（CTF / 攻防对抗）场景下，存在极其典型的**“开赛瞬间秒杀级流量风暴”**：
在倒计时归零的同一瞬间（T 0s ~ T + 60s），成百上千名参赛选手、战队成员和观赛大屏会同时涌入系统，产生以下四波密集冲击：

1. **赛题列表拉取风暴（开赛第 0 秒）**：
   - 过去 [`CtfCompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/ctf/impl/CtfCompetitionServiceImpl.java) 中的 `listChallenges` 接口，每次调用均执行 `challengeRepository.findByCompetitionIdAndIsVisibleTrueOrderBySortOrderAsc`（多表联查赛题）、`memberRepository.findMemberWithTeam`（查战队）与 `solvedRepository.findByCompetitionIdAndTeamId`（查解题历史）。
   - 1000 名选手并发刷新瞬时产生 3000~4000 次 SQL 查询，直接打满 HikariCP 数据库连接池（默认 10~20 连接），引发连接超时或数据库 502/504 宕机。
2. **热点首题详情击穿（开赛后第 5 秒 ~ 30 秒）**：
   - 选手获悉赛题列表后，全员并发点击签到题或热门题（如 Web / Misc 题）。
   - 过去 `getChallengeDetail` 每次重复查库，大文本题面、附件 URL、环境配置重复重载，缺少单飞并发折叠保护。
3. **靶机容器并发多开与宿主机过载（开赛后第 30 秒 ~ 3 分钟）**：
   - 同一战队的 3~4 名队员由于沟通延时，在同一瞬间同时点击“启动靶机”按钮；
   - 缺乏分布式互斥锁保护，多个线程并发向 Docker Daemon 发起容器创建请求，导致容器端口冲突、配额错乱，甚至瞬间大量并发拉起 Docker 容器导致物理宿主机 Docker 守护进程卡死。
4. **排行榜全量规约大排序风暴（开赛后持续）**：
   - 大屏幕、选手、教练以 3~5 秒一次的频率狂刷计分板接口（`getCtfScoreboard` / `getCompositeScoreboard`）；
   - 过去每次请求均查出全量队伍和全量解题历史记录，并在 JVM 内存里做大 Map 归约和全量 `rankList.sort` 排序，导致 CPU 100% 飙高。
5. **比赛详情轮询读写锁竞争（开赛前 5 分钟）**：
   - 过去的 `getCompetitionById` 和 `getCompetitionBySlug` 在读路径中包含了 `competition.refreshStatus() -> competitionRepository.save(competition)` 写库逻辑；
   - 在高并发读请求下，海量并发线程同时尝试 `UPDATE` 竞赛状态行，产生行级排他锁争用甚至死锁。

---

## 2. 详细改造方案与落地点

基于已落地的平台级通用三级缓存门面 [`VolurCacheTemplate`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/VolurCacheTemplate.java) 与高并发结构化门面 [`RedisOps`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/cache/redis/RedisOps.java)，进行了系统性的重构：

### 2.1 赛题列表“动静分离”三级缓存架构
- **核心思想**：将“对所有选手都相同的静态题目底座”与“针对当前队伍特定的动态解题标记”彻底解耦。
- **改动文件**：[`CtfCompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/ctf/impl/CtfCompetitionServiceImpl.java)
- **实现手段**：
  1. 静态题目底座：通过 `cacheTemplate.getOrLoad(CacheNames.COMPETITION_STATUS, "ctf:challenges:" + competitionId, ...)` 缓存。开赛瞬间 1000 人并发只由 1 个线程单飞查库，其余 999 个线程从本地 L1 Caffeine 堆内存（~50ns）极速返回；
  2. 动态状态派生：为 [`CtfCompetitionChallengeVO`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/ctf/CtfCompetitionChallengeVO.java) 添加 `@Builder(toBuilder = true)`，通过 `vo.toBuilder().isSolvedByMyTeam(solvedChallengeIds.contains(vo.getId())).build()` 产生不可变副本返回，彻底防止线程安全与缓存对象污染。

### 2.2 赛题详情单飞折叠与不可变派生
- **改动文件**：[`CtfCompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/ctf/impl/CtfCompetitionServiceImpl.java)
- **实现手段**：
  1. 使用 `cacheTemplate.getOrLoad(CacheNames.CTF_CHALLENGE_DETAIL, "comp:" + competitionId + ":chal:" + competitionChallengeId, ...)` 纳管题目详情；
  2. 为 [`CtfCompetitionChallengeDetailVO`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/ctf/CtfCompetitionChallengeDetailVO.java) 添加 `@Builder(toBuilder = true)`，同样通过不可变副本附加上当前队伍的解题判定，彻底杜绝签到题高并发击穿。

### 2.3 靶机容器启动同队同题分布式互斥锁
- **改动文件**：[`CtfCompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/ctf/impl/CtfCompetitionServiceImpl.java) 的 `startInstance`
- **实现手段**：
  - 构造锁键：`volur:lock:container:start:{competitionId}:{challengeId}:{teamId}`；
  - 基于 `redisOps.withLock(lockKey, Duration.ofSeconds(3), Duration.ofSeconds(30), ...)` 执行容器启动；
  - 当同队其他队员在 3 秒等待时间内未拿到锁（已被队友拉起），自动捕获并抛出 `BusinessException(HttpStatus.CONFLICT, "靶机容器正在启动或排队中，请勿重复操作")`，杜绝重复创建容器与端口冲突。

### 2.4 天梯榜与 CTF 榜单物化缓存防击穿
- **改动文件**：[`CompetitionScoreboardServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionScoreboardServiceImpl.java)
- **实现手段**：
  - 将 `getCtfScoreboard` 与 `getCompositeScoreboard` 委托给 `cacheTemplate.getOrLoad`；
  - 开赛高频狂刷期间，所有请求直接读取上一次计算完成的物化榜单；
  - 当缓存到期瞬间，通过 L1 并发折叠保证仅有 1 个请求执行数据库聚合与全排序；
  - **事件驱动主动失效**：在 `submitFlag` 正确解出题目后，主动调用 `cacheTemplate.evict` 精准淘汰榜单缓存，实现“有人解题立即热刷新、无人解题 100% 走缓存”的最高性能表现。

### 2.5 比赛详情与公共设置轮询消除写锁竞争
- **改动文件**：[`CompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionServiceImpl.java) 与 [`AdminCompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminCompetitionServiceImpl.java)
- **实现手段**：
  - `getCompetitionById`、`getCompetitionBySlug`、`getCompetitionAbout` 与 `listAnnouncements` 接入 `cacheTemplate.getOrLoad`；
  - 将 `refreshStatus()` 产生的写操作用 try-catch 隔离，防止并发读请求打架抛 500；
  - 比赛状态统一由 `AdminCompetitionServiceImpl` 的后台定时调度任务 `@Scheduled(fixedRate = 60000)` 安全推进并主动广播失效缓存。

### 2.6 管理端赛题与公告变动的主动广播驱逐
- **改动文件**：
  - [`AdminCtfMountServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/ctf/impl/AdminCtfMountServiceImpl.java)：挂载、更新或卸载赛题时，主动调用 `cacheTemplate.evict` 淘汰题目列表和题目详情；
  - [`AdminCompetitionAnnouncementController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/admin/AdminCompetitionAnnouncementController.java)：发布、更新、置顶或删除公告时，主动淘汰 `announcements:{competitionId}`。

---

## 3. 验证与测试结论

1. **新增单元测试用例**：
   - [`CtfCompetitionCacheTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/service/CtfCompetitionCacheTest.java)：
     - `testListChallengesDynamicSeparation`：验证动静分离，验证派生对象带有解题标记且未污染 L1 共享对象；
     - `testGetChallengeDetailCache`：验证赛题详情缓存与不可变副本；
     - `testStartInstanceDistributedLock`：验证靶机容器启动分布式互斥锁；
     - `testStartInstanceLockConflict`：验证并发冲突时返回 409 业务状态；
     - `testSubmitFlagEvictsCaches`：验证解题正确时题目列表、详情与天梯榜缓存被精确逐出。
   - [`CompetitionScoreboardCacheTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/service/CompetitionScoreboardCacheTest.java)：
     - `testGetCtfScoreboardMaterializedCache`：验证 CTF 榜单物化缓存命中，不走数据库；
     - `testGetCompositeScoreboardMaterializedCache`：验证天梯总榜物化缓存。
2. **测试结果**：
   - `volur-competition` 模块：**43 个测试全部 SUCCESS（0 失败，0 错误）**；
   - 全平台 Reactor 12 个模块：**全量 `mvn test` 100% BUILD SUCCESS（耗时 ~12s）**。
