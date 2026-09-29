package com.humolix.volur.framework.cache.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

/**
 * Redis 结构化操作门面。
 *
 * <p><b>为什么它与 {@code CacheManager} 必须分开？</b>
 *
 * <p>Spring 的 {@code Cache} 接口只有 {@code get/put/evict/clear} 四个语义，而本项目的实际需求是：
 * <ul>
 *   <li><b>积分榜</b>——按分数排序、取 Top N、查某人名次。这是 <b>ZSET 排序结构</b>，不是 KV。</li>
 *   <li><b>限流</b>——原子自增 + 滑动窗口。这是 <b>Lua 脚本 + 计数器</b>。</li>
 *   <li><b>互斥</b>——靶机启动去重、4 处 {@code @Scheduled} 选主。这是 <b>分布式锁</b>。</li>
 * </ul>
 * 硬塞进 {@code CacheManager} 只会导致抽象泄漏。因此边界是：
 * <blockquote>
 * 问："这个值能不能整体读出来、整体写回去？"<br>
 * 能 → {@code CacheManager}（享受 L1 + 广播失效）<br>
 * 不能（要排序 / 要原子自增 / 要互斥）→ 本类
 * </blockquote>
 *
 * <p><b>本类刻意不做 L1 本地缓存</b>：排序、计数、锁的价值恰恰在于"所有实例看到同一份数据"，
 * 本地副本会直接破坏语义。
 *
 * <p>所有涉及"检查后修改"的操作（自增、限流、解锁）都使用 Lua 脚本保证原子性，
 * 避免 {@code GET → 判断 → SET} 的竞态。
 */
public class RedisOps {

    private static final Logger log = LoggerFactory.getLogger(RedisOps.class);

    // ------------------------------------------------------------------
    // Lua 脚本：所有"读-改-写"都必须原子化
    // ------------------------------------------------------------------

    /**
     * 自增并在首次设置 TTL。
     *
     * <p>为什么需要脚本：{@code INCR} 之后再单独 {@code EXPIRE} 存在窗口——
     * 若两次调用之间进程崩溃，key 会永久驻留造成内存泄漏。
     */
    private static final DefaultRedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>("""
            local v = redis.call('INCRBY', KEYS[1], ARGV[1])
            if v == tonumber(ARGV[1]) then
                redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            return v
            """, Long.class);

    /**
     * ZSET 滑动窗口限流。
     *
     * <p>ARGV: [1]=now(ms) [2]=window(ms) [3]=limit [4]=本次请求唯一标识
     * <p>返回 1 表示放行，0 表示被限流。
     *
     * <p>相比固定窗口计数，滑动窗口没有"窗口边界双倍流量"问题；
     * 相比令牌桶，实现更简单且天然精确。
     */
    private static final DefaultRedisScript<Long> SLIDING_WINDOW = new DefaultRedisScript<>("""
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now - window)
            local count = redis.call('ZCARD', KEYS[1])
            if count < limit then
                redis.call('ZADD', KEYS[1], now, ARGV[4])
                redis.call('PEXPIRE', KEYS[1], window)
                return 1
            end
            return 0
            """, Long.class);

    /**
     * 解锁：校验持有者后再删除。
     *
     * <p>为什么不能直接 {@code DEL}：若业务执行时间超过锁租约，锁已自动过期并被其他线程获取，
     * 此时直接 DEL 会误删他人的锁，导致互斥失效。
     */
    private static final DefaultRedisScript<Long> UNLOCK_CAS = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisOps(StringRedisTemplate redis) {
        this.redis = redis;
    }

    // ==================================================================
    // 一、排行榜 / 计分（ZSET）
    // ==================================================================

    /** 写入或覆盖成员分数。 */
    public void zAdd(String key, String member, double score) {
        redis.opsForZSet().add(key, member, score);
    }

    /** 分数累加（AWD 攻击得分、跨赛制合分等场景）。 */
    public double zIncrBy(String key, String member, double delta) {
        Double result = redis.opsForZSet().incrementScore(key, member, delta);
        return result == null ? 0d : result;
    }

    /**
     * 取降序 Top N 榜单。
     *
     * @param key  榜单 key
     * @param topN 取前多少名
     * @return 带名次（从 1 开始）的条目列表；无数据返回空列表
     */
    public List<ZSetEntry> zTop(String key, long topN) {
        if (topN <= 0) {
            return List.of();
        }
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(key, 0, topN - 1);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }
        List<ZSetEntry> result = new ArrayList<>(tuples.size());
        long rank = 1L;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Double score = tuple.getScore();
            result.add(new ZSetEntry(tuple.getValue(), score == null ? 0d : score, rank++));
        }
        return result;
    }

    /** 查询成员名次（从 1 开始，降序）。不存在返回 {@code null}。 */
    public Long zRank(String key, String member) {
        Long rank = redis.opsForZSet().reverseRank(key, member);
        return rank == null ? null : rank + 1;
    }

    /** 查询成员分数。不存在返回 {@code null}。 */
    public Double zScore(String key, String member) {
        return redis.opsForZSet().score(key, member);
    }

    /** 榜单成员总数。 */
    public long zCard(String key) {
        Long size = redis.opsForZSet().zCard(key);
        return size == null ? 0L : size;
    }

    /** 移除成员。 */
    public void zRemove(String key, String... members) {
        if (members == null || members.length == 0) {
            return;
        }
        redis.opsForZSet().remove(key, (Object[]) members);
    }

    /** 为榜单设置 TTL，避免历史赛事数据永久驻留。 */
    public void expire(String key, Duration ttl) {
        redis.expire(key, ttl);
    }

    /**
     * 榜单条目。
     *
     * @param member 成员标识（通常是 teamId 的字符串形式）
     * @param score  分数
     * @param rank   名次，从 1 开始
     */
    public record ZSetEntry(String member, double score, long rank) {
    }

    // ==================================================================
    // 二、原子计数与限流
    // ==================================================================

    /**
     * 原子自增，并在首次自增时设置 TTL。
     *
     * @param key   计数器 key
     * @param delta 增量
     * @param ttl   首次自增时设置的过期时间
     * @return 自增后的值
     */
    public long incr(String key, long delta, Duration ttl) {
        Long value = redis.execute(INCR_WITH_TTL, List.of(key), String.valueOf(delta),
                String.valueOf(ttl.toMillis()));
        return value == null ? 0L : value;
    }

    /**
     * 滑动窗口限流。
     *
     * <p>典型用法（Flag 提交防爆破）：
     * <pre>{@code
     * if (!redisOps.tryAcquire("volur:rate:flag:" + teamId, 30, Duration.ofMinutes(1))) {
     *     throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "提交过于频繁，请稍后再试");
     * }
     * }</pre>
     *
     * @param key    限流维度 key（建议按"业务 + 维度主体"组织，如 {@code volur:rate:flag:<teamId>}）
     * @param limit  窗口内允许的最大次数
     * @param window 窗口大小
     * @return true 放行；false 被限流
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        if (limit <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        String member = now + "-" + UUID.randomUUID();
        Long allowed = redis.execute(SLIDING_WINDOW, List.of(key),
                String.valueOf(now),
                String.valueOf(window.toMillis()),
                String.valueOf(limit),
                member);
        return allowed != null && allowed == 1L;
    }

    /** 查询滑动窗口当前计数（用于监控与前端提示），不产生副作用。 */
    public long currentWindowCount(String key, Duration window) {
        long now = System.currentTimeMillis();
        redis.opsForZSet().removeRangeByScore(key, 0, now - window.toMillis());
        Long size = redis.opsForZSet().zCard(key);
        return size == null ? 0L : size;
    }

    // ==================================================================
    // 三、分布式锁
    // ==================================================================

    /**
     * 尝试获取锁（单次，不重试）。
     *
     * @param key   锁 key
     * @param lease 租约时长，<b>应大于业务 P99 耗时</b>；超时后锁自动释放，避免死锁
     * @return 持有句柄；未获取到返回空
     */
    public Optional<LockHandle> tryLock(String key, Duration lease) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redis.opsForValue().setIfAbsent(key, token, lease);
        return Boolean.TRUE.equals(acquired) ? Optional.of(new LockHandle(key, token)) : Optional.empty();
    }

    /**
     * 在指定等待时间内轮询获取锁。
     *
     * @param key   锁 key
     * @param wait  最长等待时间
     * @param lease 租约时长
     */
    public Optional<LockHandle> tryLock(String key, Duration wait, Duration lease) {
        long deadline = System.nanoTime() + wait.toNanos();
        do {
            Optional<LockHandle> handle = tryLock(key, lease);
            if (handle.isPresent()) {
                return handle;
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        } while (System.nanoTime() < deadline);
        return Optional.empty();
    }

    /** 释放锁（校验持有者，避免误删他人锁）。 */
    public void unlock(LockHandle handle) {
        if (handle == null) {
            return;
        }
        try {
            redis.execute(UNLOCK_CAS, List.of(handle.key()), handle.token());
        } catch (Exception ex) {
            // 释放失败依赖租约自动过期，不向上抛
            log.warn("[volur-redis] 释放分布式锁失败，将由租约自动过期。key={}", handle.key(), ex);
        }
    }

    /**
     * 加锁执行业务逻辑，自动释放。
     *
     * <p>典型用法（靶机启动去重）：
     * <pre>{@code
     * return redisOps.withLock("volur:lock:instance:" + userId + ":" + challengeId,
     *         Duration.ofSeconds(3), Duration.ofSeconds(30),
     *         () -> doStartInstance(challengeId, userId));
     * }</pre>
     *
     * @throws IllegalStateException 等待超时未获取到锁
     */
    public <T> T withLock(String key, Duration wait, Duration lease, Supplier<T> action) {
        Optional<LockHandle> handle = tryLock(key, wait, lease);
        if (handle.isEmpty()) {
            throw new IllegalStateException("获取分布式锁超时: " + key);
        }
        try {
            return action.get();
        } finally {
            unlock(handle.get());
        }
    }

    /** 加锁执行无返回值的业务逻辑。 */
    public void withLock(String key, Duration wait, Duration lease, Runnable action) {
        withLock(key, wait, lease, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 锁持有句柄。
     *
     * @param key   锁 key
     * @param token 持有者标识，解锁时用于 CAS 校验
     */
    public record LockHandle(String key, String token) {
    }

    // ==================================================================
    // 四、通用工具
    // ==================================================================

    /**
     * 按模式批量删除。
     *
     * <p><b>必须用 SCAN 而非 KEYS</b>：{@code KEYS} 会阻塞 Redis 单线程，
     * 在百万级 key 下直接导致全站超时。SCAN 是游标式分批遍历，每次只阻塞极短时间。
     *
     * @param pattern 匹配模式，如 {@code vcache:user_details:*}
     * @return 删除的 key 数量
     */
    public long deleteByPattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
        List<String> batch = new ArrayList<>(500);
        long deleted = 0L;
        try (Cursor<String> cursor = redis.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= 500) {
                    deleted += deleteBatch(batch);
                }
            }
        } catch (Exception ex) {
            log.warn("[volur-redis] SCAN 删除失败，pattern={}", pattern, ex);
            return deleted;
        }
        deleted += deleteBatch(batch);
        return deleted;
    }

    private long deleteBatch(List<String> batch) {
        if (batch.isEmpty()) {
            return 0L;
        }
        Long removed = redis.delete(batch);
        batch.clear();
        return removed == null ? 0L : removed;
    }

    /** 删除单个或多个 key。 */
    public long delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return 0L;
        }
        Long removed = redis.delete(List.of(keys));
        return removed == null ? 0L : removed;
    }

    /** key 是否存在。 */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    /** 读取字符串值。 */
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    /** 写入字符串值。 */
    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    /** 批量读取，用于把 N 次网络往返压成 1 次。 */
    public List<String> multiGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return redis.opsForValue().multiGet(keys);
    }
}
