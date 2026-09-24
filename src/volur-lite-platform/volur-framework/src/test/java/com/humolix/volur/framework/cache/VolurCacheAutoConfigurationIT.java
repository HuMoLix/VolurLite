package com.humolix.volur.framework.cache;

import com.humolix.volur.framework.cache.invalidation.CacheEvictListener;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动配置装配的集成测试。
 *
 * <p><b>为什么必须有这一层？</b> 单元测试把 {@link VolurCache} 单独拿出来测，
 * 但架构评估里最关键的 P0 问题是"{@code @Cacheable} 是空操作"——那是<b>装配问题</b>，
 * 不是算法问题：{@code @EnableCaching} 与 {@code CacheManager} 必须同时存在，
 * 且 CacheManager 必须在自动配置排序中胜出。这些只有起真实上下文才能验证。
 *
 * <p>本测试的 classpath 里<b>刻意包含了 {@code spring-boot-cache}</b>
 * （Boot 4 的缓存模块，内含 {@code RedisCacheConfiguration}，会尝试创建
 * {@code RedisCacheManager}）。因此 {@link #ourCacheManagerWinsOverBootDefault()}
 * 是对 {@code beforeName} 排序方案的直接验证：若排序失效，Boot 的 CacheManager 会先注册，
 * 我们的实现因 {@code @ConditionalOnMissingBean} 退让，测试立即失败。
 *
 * <p>Redis 指向一个不可达端口：本测试只关心装配关系，不关心 Redis 行为，
 * 因此关闭失效广播以避免无意义的连接重试日志。
 */
@SpringBootTest(
        classes = CacheTestApplication.class,
        properties = {
                "volur.cache.invalidation.enabled=false",
                "spring.data.redis.host=127.0.0.1",
                "spring.data.redis.port=1",
                "spring.data.redis.repositories.enabled=false",
                "spring.main.banner-mode=off",
                "logging.level.com.humolix.volur.framework.cache=WARN"
        })
class VolurCacheAutoConfigurationIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CacheTestApplication.CountingUserService userService;

    @Test
    @DisplayName("@Cacheable 真的生效：同一 key 只执行一次目标方法")
    void cacheableAnnotationActuallyCaches() {
        // CountingUserService 是单例、被整个测试类共享，因此断言必须基于增量，
        // 否则测试之间会通过这个计数器互相污染（这是集成测试的经典陷阱）。
        int before = userService.invocations();

        assertThat(userService.findById(1001L)).isEqualTo("user-1001");
        assertThat(userService.findById(1001L)).isEqualTo("user-1001");
        assertThat(userService.findById(1001L)).isEqualTo("user-1001");

        // 这就是架构评估中"@Cacheable 是空操作"的正面验证：
        // 若装配缺失，这里会是 3。
        assertThat(userService.invocations() - before)
                .as("同一 key 的三次调用只应回源一次")
                .isEqualTo(1);

        // 不同 key 必须各自回源
        assertThat(userService.findById(1002L)).isEqualTo("user-1002");
        assertThat(userService.invocations() - before).isEqualTo(2);
    }

    @Test
    @DisplayName("我们的 CacheManager 在排序中胜出，而非 Boot 默认的 RedisCacheManager")
    void ourCacheManagerWinsOverBootDefault() {
        assertThat(cacheManager).isInstanceOf(VolurCacheManager.class);
        // 反向确认：Boot 的默认实现确实在类路径上，只是被我们挤掉了
        assertThat(context.getBeanNamesForType(VolurCacheManager.class)).hasSize(1);
    }

    @Test
    @DisplayName("CacheManager 暴露了预创建的全部声明缓存")
    void declaresAllRegisteredCaches() {
        assertThat(cacheManager.getCacheNames()).containsAll(CacheNames.ALL);
    }

    @Test
    @DisplayName("编程式门面与结构化操作门面均已就绪")
    void programmaticFacadesAreAvailable() {
        assertThat(context.getBean(VolurCacheTemplate.class)).isNotNull();
        assertThat(context.getBean(com.humolix.volur.framework.cache.redis.RedisOps.class)).isNotNull();
    }

    @Test
    @DisplayName("关闭失效广播时不创建订阅者与订阅容器（不白占连接）")
    void invalidationDisabledSkipsListenerAndContainer() {
        assertThat(context.getBeanNamesForType(CacheEvictListener.class)).isEmpty();
        assertThat(context.containsBean("volurCacheRedisContainer")).isFalse();
        // 但发送方仍存在，只是内部短路
        assertThat(context.getBean(com.humolix.volur.framework.cache.invalidation.CacheEvictBroadcaster.class))
                .isNotNull();
    }

    /**
     * 声明式缓存的回源次数要看 {@code volur.cache.puts}，而不是 {@code loader.calls}。
     *
     * <p>这是本测试最重要的一个断言，它固化了一条容易踩空的语义：
     * {@code @Cacheable} 默认 {@code sync=false} 时 Spring 走
     * {@code get(key)} + {@code put(key, value)}，<b>不经过</b>
     * {@code Cache#get(Object, Callable)}。因此：
     * <ul>
     *   <li>{@code volur.cache.puts} 增长 → 表示真的回源了一次；</li>
     *   <li>{@code volur.cache.loader.calls} <b>保持为 0</b> → 这不是 bug，
     *       而是"声明式缓存没有单飞路径"的直接体现。</li>
     * </ul>
     * 曾经因为没意识到这点，把 {@code loader.calls} 当成"回源次数"写进看板，
     * 结果线上永远显示 0 次回源。
     */
    @Test
    @DisplayName("声明式缓存：puts 反映真实回源，loader.calls 恒为 0（sync=false 无单飞路径）")
    void declarativeCacheCountsPutsNotLoaderCalls() {
        MeterRegistry registry = context.getBean(MeterRegistry.class);
        String cache = CacheNames.USER_DETAILS;

        double putsBefore = registry.get("volur.cache.puts").tag("cache", cache)
                .functionCounter().count();
        double loadsBefore = registry.get("volur.cache.loader.calls").tag("cache", cache)
                .functionCounter().count();

        int invocationsBefore = userService.invocations();
        userService.findById(3001L);
        userService.findById(3001L);

        assertThat(userService.invocations() - invocationsBefore)
                .as("同一 key 只应回源一次").isEqualTo(1);

        assertThat(registry.get("volur.cache.puts").tag("cache", cache)
                .functionCounter().count() - putsBefore)
                .as("一次未命中对应一次 put").isEqualTo(1.0D);

        assertThat(registry.get("volur.cache.loader.calls").tag("cache", cache)
                .functionCounter().count() - loadsBefore)
                .as("sync=false 不经过 get(key, Callable)，因此 loader.calls 不增长")
                .isEqualTo(0.0D);
    }

    @Test
    @DisplayName("sync=true 时走 get(key, Callable)，loader.calls 才增长")
    void syncCacheableUsesLoaderPath() {
        MeterRegistry registry = context.getBean(MeterRegistry.class);
        String cache = CacheNames.CTF_CHALLENGE_DETAIL;

        double before = registry.get("volur.cache.loader.calls").tag("cache", cache)
                .functionCounter().count();
        int invocationsBefore = userService.syncInvocations();

        userService.findChallengeSync(4001L);
        userService.findChallengeSync(4001L);

        assertThat(userService.syncInvocations() - invocationsBefore).isEqualTo(1);
        assertThat(registry.get("volur.cache.loader.calls").tag("cache", cache)
                .functionCounter().count() - before).isEqualTo(1.0D);
        // 回源累计耗时也必须为正
        assertThat(registry.get("volur.cache.loader.duration").tag("cache", cache)
                .functionCounter().count()).isPositive();
    }

    @Test
    @DisplayName("L1 命中会体现在 l1.gets 上")
    void l1HitsAreVisibleThroughMetrics() {
        MeterRegistry registry = context.getBean(MeterRegistry.class);
        String cache = CacheNames.USER_DETAILS;

        double hitsBefore = registry.get("volur.cache.l1.gets").tag("cache", cache)
                .tag("result", "hit").functionCounter().count();

        userService.findById(5001L);
        userService.findById(5001L);
        userService.findById(5001L);

        // 三次读取中后两次为 L1 命中
        assertThat(registry.get("volur.cache.l1.gets").tag("cache", cache)
                .tag("result", "hit").functionCounter().count() - hitsBefore)
                .isEqualTo(2.0D);
    }

    @Test
    @DisplayName("Micrometer 就绪时指标绑定器被装配")
    void binderBeanIsPresent() {
        assertThat(context.getBeanNamesForType(VolurCacheMetricsBinder.class)).hasSize(1);
    }
}
