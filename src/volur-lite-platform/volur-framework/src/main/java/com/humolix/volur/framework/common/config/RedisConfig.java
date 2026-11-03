package com.humolix.volur.framework.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "com.humolix.volur.**.repository.redis")
public class RedisConfig {
}
