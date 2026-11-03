package com.humolix.volur.notification.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RealtimeRedisConfig {

    public static final String VREDIS_REALTIME_PUBSUB_CHANNEL = "vredis:realtime:pubsub:channel";

    private final RealtimeRedisSubscriber realtimeRedisSubscriber;

    @Bean
    public RedisMessageListenerContainer realtimeRedisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(realtimeRedisSubscriber, new ChannelTopic(VREDIS_REALTIME_PUBSUB_CHANNEL));
        log.info("[Redis-PubSub] 实时事件监听容器初始化完成，监听频道: {}", VREDIS_REALTIME_PUBSUB_CHANNEL);
        return container;
    }

}
