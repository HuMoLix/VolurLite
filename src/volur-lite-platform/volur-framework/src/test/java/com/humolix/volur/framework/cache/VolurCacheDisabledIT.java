package com.humolix.volur.framework.cache;

import com.humolix.volur.framework.cache.invalidation.CacheEvictBroadcaster;
import com.humolix.volur.framework.cache.redis.RedisOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code volur.cache.enabled=false} 的紧急降级行为。
 *
 * <p>这个开关是给运维在缓存组件出故障时用的。它的契约必须非常明确：
 * <ol>
 *   <li><b>应用仍能启动</b>——绝不能因为缓存被关闭而启动失败；</li>
 *   <li><b>{@code @Cacheable} 退化为直接执行原方法</b>——不是"抛异常"，也不是"返回 null"；</li>
 *   <li>缓存相关的 bean 全部退让，不留下半死不活的组件。</li>
 * </ol>
 *
 * <p>注意第 2 点的实现原理：关闭本配置会连带让 {@code @EnableCaching} 失效，
 * 于是 Spring 根本不创建缓存代理，{@code @Cacheable} 注解被完全忽略。
 * 这比"保留代理但让 CacheManager 变成 no-op"更彻底，也更快。
 */
@SpringBootTest(
        classes = CacheTestApplication.class,
        properties = {
                "volur.cache.enabled=false",
                "spring.data.redis.host=127.0.0.1",
                "spring.data.redis.port=1",
                "spring.data.redis.repositories.enabled=false",
                "spring.main.banner-mode=off"
        })
class VolurCacheDisabledIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private CacheTestApplication.CountingUserService userService;

    @Test
    @DisplayName("关闭开关后应用照常启动，缓存相关 bean 全部退让")
    void contextStartsAndCacheBeansBackOff() {
        assertThat(context.getBeanNamesForType(VolurCacheManager.class)).isEmpty();
        assertThat(context.getBeanNamesForType(VolurCacheTemplate.class)).isEmpty();
        assertThat(context.getBeanNamesForType(VolurCacheMetricsBinder.class)).isEmpty();
        assertThat(context.getBeanNamesForType(RedisOps.class)).isEmpty();
        assertThat(context.getBeanNamesForType(CacheEvictBroadcaster.class)).isEmpty();
    }

    @Test
    @DisplayName("@Cacheable 退化为直接执行原方法（每次请求都回源，而不是报错）")
    void cacheableDegradesToDirectInvocation() {
        assertThat(userService.findById(1L)).isEqualTo("user-1");
        assertThat(userService.findById(1L)).isEqualTo("user-1");

        assertThat(userService.invocations()).isEqualTo(2);
    }
}
