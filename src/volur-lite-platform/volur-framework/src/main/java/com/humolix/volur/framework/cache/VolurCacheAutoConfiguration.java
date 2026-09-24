package com.humolix.volur.framework.cache;

import com.humolix.volur.framework.cache.invalidation.CacheEvictBroadcaster;
import com.humolix.volur.framework.cache.invalidation.CacheEvictListener;
import com.humolix.volur.framework.cache.redis.RedisOps;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 全局缓存自动装配。
 *
 * <p><b>沿用项目既有的自动装配约定</b>：与 {@code SnowflakeAutoConfiguration} 一样通过
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * 注册，因此 <b>无需改动启动类或任何业务代码</b>。
 *
 * <p><b>关键点 1：必须在 Spring Boot 的缓存自动配置之前装配。</b>
 * 一旦 {@code spring-boot-cache} 在类路径上且存在 {@code RedisConnectionFactory}，
 * Boot 会通过其 {@code RedisCacheConfiguration} 自动配置一个 {@code RedisCacheManager}。
 * 那个默认实现只做单级 Redis 缓存、使用 JDK 序列化、且没有跨实例 L1 失效能力。
 * 通过 {@code before} 保证本配置的 {@link VolurCacheManager} 先注册，
 * 使 Boot 的默认实现因 {@code @ConditionalOnMissingBean} 而退让。
 *
 * <p><b>关键点 1 的 Boot 4 迁移陷阱（务必注意）</b>：
 * Spring Boot 3.x 中该类位于 {@code org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration}；
 * <b>Boot 4.0 起单体 {@code spring-boot-autoconfigure} 被拆分</b>，缓存相关类整体迁到了
 * 新模块 {@code spring-boot-cache}，包名变为
 * {@code org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration}。
 * 因此这里<b>不能</b>写 {@code before = CacheAutoConfiguration.class}——那会编译失败
 * （找不到符号），而且会强迫 {@code volur-framework} 为了一个排序声明而新增
 * {@code spring-boot-cache} 依赖。
 *
 * <p>改用 {@link AutoConfiguration#beforeName()} 字符串形式可同时解决两个问题：
 * <ol>
 *   <li><b>零编译期依赖</b>——引用的类不在类路径上也能编译；</li>
 *   <li><b>自动适配两种环境</b>——若下游没有 {@code spring-boot-cache}（本项目现状），
 *       该名字在自动配置列表中不存在，排序器直接忽略；若下游引入了它，排序立即生效。
 *       无论哪种情况都无需改动本模块。</li>
 * </ol>
 *
 * <p><b>关键点 2：只创建一个 {@link RedisMessageListenerContainer}。</b>
 * 项目现有 {@code SystemSettingRedisConfig} 已自建了一个容器，若每个缓存域都照此办理，
 * N 个缓存域就是 N 套订阅连接。统一到本容器后应逐步把其他 topic 也挂载到这里。
 *
 * <p><b>降级开关</b>：设置 {@code volur.cache.enabled=false} 时本配置整体退让，
 * 由于 {@code @EnableCaching} 一并失效，所有 {@code @Cacheable} 会退化为"直接执行原方法"。
 * 这是刻意的：缓存组件出问题时，运维需要一个"一键关闭"的开关，
 * 而不是让应用因为找不到 CacheManager 而启动失败。
 *
 * <p><b>⚠️ 反向陷阱</b>：{@code @EnableCaching} 与 {@code CacheManager} 必须同时存在。
 * 只加 {@code @EnableCaching} 而不提供 {@code CacheManager} 会让<b>应用启动失败</b>：
 * <pre>
 *   NoSuchBeanDefinitionException: No qualifying bean of type
 *   'org.springframework.cache.CacheManager' available: no CacheResolver specified
 * </pre>
 * 本项目当前<b>没有</b>引入 {@code spring-boot-cache}，所以 Boot 不会自动提供
 * {@code RedisCacheManager}——本类才是唯一的 CacheManager 来源。
 */
@AutoConfiguration(beforeName = "org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration")
@ConditionalOnProperty(prefix = "volur.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(VolurCacheProperties.class)
@EnableCaching
public class VolurCacheAutoConfiguration {

    /** 结构化操作门面：ZSET / 限流 / 分布式锁。与缓存管理器职责分离。 */
    @Bean
    @ConditionalOnMissingBean
    public RedisOps volurRedisOps(StringRedisTemplate redisTemplate) {
        return new RedisOps(redisTemplate);
    }

    /** 失效广播发送方。 */
    @Bean
    @ConditionalOnMissingBean
    public CacheEvictBroadcaster volurCacheEvictBroadcaster(StringRedisTemplate redisTemplate,
                                                            VolurCacheProperties properties) {
        return new CacheEvictBroadcaster(redisTemplate, properties);
    }

    /**
     * 全局缓存管理器。
     *
     * <p>{@code @ConditionalOnMissingBean(CacheManager.class)} 让业务方在特殊场景下
     * 仍可声明自己的 CacheManager 覆盖本实现。
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public VolurCacheManager volurCacheManager(StringRedisTemplate redisTemplate,
                                               VolurCacheProperties properties,
                                               RedisOps redisOps,
                                               CacheEvictBroadcaster broadcaster) {
        return new VolurCacheManager(properties, redisTemplate, redisOps, broadcaster);
    }

    /**
     * 编程式缓存门面。
     *
     * <p>{@code @ConditionalOnBean} 是<b>防御性</b>的：若将来有人引入
     * {@code spring-boot-cache} 且排序失效导致本类的 CacheManager 退让，
     * 缺少它会让本 bean 因找不到 {@link VolurCacheManager} 而<b>启动失败</b>。
     * 加上守卫后，最坏情况退化为"使用 Boot 默认缓存"，而不是应用起不来。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(VolurCacheManager.class)
    public VolurCacheTemplate volurCacheTemplate(VolurCacheManager cacheManager) {
        return new VolurCacheTemplate(cacheManager);
    }

    /**
     * 失效广播订阅方（只清本机 L1）。
     *
     * <p>与下面的容器一起受 {@code volur.cache.invalidation.enabled} 控制。
     * 关闭广播时不应再创建订阅者与订阅容器——否则会白占一条 Redis 订阅连接，
     * 而且单机部署/本地开发时这个容器还会反复尝试连接不可用的 Redis 并刷日志。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(VolurCacheManager.class)
    @ConditionalOnProperty(prefix = "volur.cache.invalidation", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public CacheEvictListener volurCacheEvictListener(VolurCacheManager cacheManager) {
        return new CacheEvictListener(cacheManager);
    }

    /**
     * 全局唯一的缓存失效订阅容器。
     *
     * <p>后续接入新缓存域时，应把新 topic 也 {@code addMessageListener} 到本容器，
     * <b>不要新建容器</b>。
     */
    @Bean(name = "volurCacheRedisContainer")
    @ConditionalOnMissingBean(name = "volurCacheRedisContainer")
    @ConditionalOnProperty(prefix = "volur.cache.invalidation", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer volurCacheRedisContainer(RedisConnectionFactory connectionFactory,
                                                                  CacheEvictListener listener,
                                                                  VolurCacheProperties properties) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener,
                new ChannelTopic(properties.getInvalidation().getTopic()));
        return container;
    }

    /**
     * 指标绑定：仅在 Micrometer 存在时装配。
     *
     * <p><b>为什么用嵌套 {@code @Configuration} 而不是直接在 {@code @Bean} 方法上标注
     * {@code @ConditionalOnClass}？</b> 因为 {@code @Bean} 方法的返回类型是
     * {@link VolurCacheMetricsBinder}，而它 {@code implements MeterBinder}——
     * 若 Micrometer 缺失，解析该方法的返回类型就会失败。把条件放在嵌套配置类上，
     * Spring 会在<b>读取其成员之前</b>就跳过整个类，这才是安全的写法。
     *
     * <p>这也保证了 {@code volur-framework} 对 Micrometer 只是
     * {@code <optional>true</optional>} 依赖：下游有 Actuator 就自动有指标，
     * 没有也照样启动。
     *
     * <p><b>⚠️ 必须重复外层 {@code @ConditionalOnProperty}。</b>
     * 原因是静态嵌套的 {@code @Configuration} 类<b>同时是组件扫描的候选</b>
     * （{@code @Configuration} 元注解了 {@code @Component}，且静态嵌套类满足
     * {@code isIndependent()}）。若只在外层标注条件，关闭缓存时外层被跳过，
     * 但这个嵌套类仍会被扫描注册，于是启动时报
     * "No qualifying bean of type VolurCacheManager"——把"一键降级开关"
     * 变成了"启动失败"。这是很隐蔽的一类坑。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "volur.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class MetricsConfiguration {

        @Bean
        @ConditionalOnMissingBean
        VolurCacheMetricsBinder volurCacheMetricsBinder(CacheManager cacheManager) {
            return new VolurCacheMetricsBinder(cacheManager);
        }
    }
}
