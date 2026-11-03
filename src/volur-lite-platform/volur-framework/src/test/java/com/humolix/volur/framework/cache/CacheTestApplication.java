package com.humolix.volur.framework.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 集成测试用的最小 Spring Boot 应用。
 *
 * <p>存在的意义是让 {@code @EnableAutoConfiguration} 真正去读
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}，
 * 从而走完整的自动配置排序流程——这是验证
 * {@code @AutoConfiguration(beforeName = "org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration")}
 * 是否安全、是否生效的唯一途径。
 */
@SpringBootApplication
public class CacheTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheTestApplication.class, args);
    }

    @Bean
    public CountingUserService countingUserService() {
        return new CountingUserService();
    }

    @Bean
    public io.micrometer.core.instrument.MeterRegistry meterRegistry(
            org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.binder.MeterBinder> binders) {
        io.micrometer.core.instrument.simple.SimpleMeterRegistry registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        binders.orderedStream().forEach(b -> b.bindTo(registry));
        return registry;
    }

    /**
     * 模拟真实的业务服务：{@code @Cacheable} 是否真的生效，
     * 取决于 {@code @EnableCaching} 与 {@code CacheManager} 是否同时就位。
     */
    public static class CountingUserService {

        private final AtomicInteger invocations = new AtomicInteger();
        private final AtomicInteger syncInvocations = new AtomicInteger();

        /**
         * 默认形式（{@code sync = false}）。
         *
         * <p>Spring 的实际流程是 {@code cache.get(key)} → 未命中则执行本方法 →
         * {@code cache.put(key, 结果)}。它<b>不会</b>调用
         * {@code Cache#get(Object, Callable)}，因此没有单飞保护。
         */
        @Cacheable(cacheNames = CacheNames.USER_DETAILS, key = "#id")
        public String findById(long id) {
            invocations.incrementAndGet();
            return "user-" + id;
        }

        /**
         * 同步形式（{@code sync = true}）。
         *
         * <p>此时 Spring 调用 {@code Cache#get(key, Callable)}，回源被放进
         * Caffeine 的映射函数里，<b>同一 key 的并发请求只有一个会真正执行本方法</b>。
         * 这是本框架防击穿能力的唯一入口，也是 {@code volur.cache.loader.*} 指标的数据来源。
         */
        @Cacheable(cacheNames = CacheNames.CTF_CHALLENGE_DETAIL, key = "#id", sync = true)
        public String findChallengeSync(long id) {
            syncInvocations.incrementAndGet();
            return "challenge-" + id;
        }

        public int invocations() {
            return invocations.get();
        }

        public int syncInvocations() {
            return syncInvocations.get();
        }
    }
}
