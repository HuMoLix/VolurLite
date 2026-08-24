package com.humolix.volur.system.config;

import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.system.service.impl.SystemSettingServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SystemSettingRedisConfig {

    private final SystemSettingService systemSettingService;

    @Bean
    public RedisMessageListenerContainer systemSettingRedisContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(@NonNull Message message, byte[] pattern) {
                try {
                    String key = new String(message.getBody(), StandardCharsets.UTF_8);
                    log.debug("Received Redis evict broadcast for setting key: {}", key);
                    systemSettingService.evictLocalCache(key);
                } catch (Exception ex) {
                    log.error("Error processing system setting evict message", ex);
                }
            }
        }, new ChannelTopic(SystemSettingServiceImpl.VREDIS_SYSTEM_SETTING_TOPIC));
        return container;
    }

}
