package com.humolix.volur.framework.id.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "volur.snowflake")
public class SnowflakeProperties {

    private boolean enableRedisWorker = true;

    private Long workerId = 0L;

    private long workerTtlSeconds = 30L;

}
