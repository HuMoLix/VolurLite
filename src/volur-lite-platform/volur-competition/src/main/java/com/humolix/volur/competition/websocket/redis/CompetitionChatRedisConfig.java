package com.humolix.volur.competition.websocket.redis;

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
public class CompetitionChatRedisConfig {

    private final CompetitionChatRedisSubscriber chatRedisSubscriber;

    @Bean
    public RedisMessageListenerContainer competitionChatRedisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(chatRedisSubscriber, new ChannelTopic(CompetitionChatRedisPublisher.VREDIS_COMPETITION_CHAT_CHANNEL));
        log.info("[WebSocket-Chat] Redis 广播订阅容器初始化完成, topic={}", CompetitionChatRedisPublisher.VREDIS_COMPETITION_CHAT_CHANNEL);
        return container;
    }

}
