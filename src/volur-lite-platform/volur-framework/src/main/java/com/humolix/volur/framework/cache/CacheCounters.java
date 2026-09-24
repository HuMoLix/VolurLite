package com.humolix.volur.framework.cache;

import java.util.concurrent.atomic.LongAdder;

/**
 * 单缓存实例的运行期计数。
 *
 * <p><b>为什么用 {@link LongAdder} 而不是 {@code AtomicLong}？</b>
 * 缓存读写是所有请求的必经路径，计数器是<b>极高写入频次、极低读取频次</b>的场景。
 * {@code LongAdder} 在竞争下把写入分散到多个 cell，吞吐显著优于 CAS 自旋的
 * {@code AtomicLong}；其代价是读取时需汇总所有 cell，而我们只在指标抓取时读一次。
 *
 * <p><b>为什么本类不依赖 Micrometer？</b>
 * 这是刻意的分层：{@code volur-framework} 不应该为了埋点而把 Micrometer 变成硬依赖。
 * 计数用 JDK 原生类型完成，由可选的 {@link VolurCacheMetricsBinder} 在
 * {@code micrometer-core} 存在时才把计数翻译成指标。若下游没引 Actuator，
 * 计数照常累加（开销可忽略），只是没人来读——而不是让整个模块编译不过。
 *
 * <p>本类线程安全。{@link #snapshot()} 返回的是一次性快照，各字段之间不保证原子一致性，
 * 对监控用途而言这完全可以接受。
 */
public final class CacheCounters {

    private final LongAdder l2Hits = new LongAdder();
    private final LongAdder l2Misses = new LongAdder();
    private final LongAdder l2Errors = new LongAdder();
    private final LongAdder l2Writes = new LongAdder();
    private final LongAdder nullPlaceholders = new LongAdder();
    private final LongAdder loaderCalls = new LongAdder();
    private final LongAdder loaderNanos = new LongAdder();
    private final LongAdder puts = new LongAdder();
    private final LongAdder explicitEvictions = new LongAdder();
    private final LongAdder broadcastFailures = new LongAdder();
    private final LongAdder decodeFailures = new LongAdder();

    void l2Hit() {
        l2Hits.increment();
    }

    void l2Miss() {
        l2Misses.increment();
    }

    void l2Error() {
        l2Errors.increment();
    }

    void l2Write() {
        l2Writes.increment();
    }

    void nullPlaceholder() {
        nullPlaceholders.increment();
    }

    void loaderCall() {
        loaderCalls.increment();
    }

    void loaderNanos(long nanos) {
        loaderNanos.add(nanos);
    }

    void put() {
        puts.increment();
    }

    void explicitEviction() {
        explicitEvictions.increment();
    }

    void broadcastFailure() {
        broadcastFailures.increment();
    }

    void decodeFailure() {
        decodeFailures.increment();
    }

    public Snapshot snapshot() {
        return new Snapshot(
                l2Hits.sum(),
                l2Misses.sum(),
                l2Errors.sum(),
                l2Writes.sum(),
                nullPlaceholders.sum(),
                loaderCalls.sum(),
                loaderNanos.sum(),
                puts.sum(),
                explicitEvictions.sum(),
                broadcastFailures.sum(),
                decodeFailures.sum()
        );
    }

    /**
     * 计数快照。
     *
     * @param l2Hits           L2 命中次数
     * @param l2Misses         L2 未命中次数（注意：计的是"查询次数"而非"唯一 key 数"，
     *                         启用分布式锁时一次加载会对 L2 查询两次）
     * @param l2Errors         L2 读写异常次数（已降级，不代表业务失败）
     * @param l2Writes         L2 写入成功次数
     * @param nullPlaceholders 写入的空值占位次数
     * @param loaderCalls      <b>编程式/sync 路径</b>的回源次数，见 {@link #puts} 的说明
     * @param loaderNanos      <b>编程式/sync 路径</b>的回源累计耗时（纳秒）
     * @param puts             写入次数。<b>声明式缓存（{@code @Cacheable} 默认
     *                         {@code sync=false}）的"真实回源次数"看这个值</b>：
     *                         它的流程是 {@code get} 未命中 → 执行原方法 → {@code put}，
     *                         因此 {@code puts} 才对应"打了一次源"。
     *                         {@code loaderCalls} 只在 {@code @Cacheable(sync = true)}
     *                         或编程式 {@code getOrLoad} 时才有值。
     * @param explicitEvictions 显式 evict() 调用次数
     * @param broadcastFailures 失效广播发送失败次数
     * @param decodeFailures   值反序列化失败次数（通常意味着类型不兼容或脏数据）
     */
    public record Snapshot(
            long l2Hits,
            long l2Misses,
            long l2Errors,
            long l2Writes,
            long nullPlaceholders,
            long loaderCalls,
            long loaderNanos,
            long puts,
            long explicitEvictions,
            long broadcastFailures,
            long decodeFailures
    ) {
    }
}
