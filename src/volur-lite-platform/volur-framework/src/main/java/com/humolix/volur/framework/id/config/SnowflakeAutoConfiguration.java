package com.humolix.volur.framework.id.config;

import com.humolix.volur.framework.id.generator.SnowflakeIdGenerator;
import com.humolix.volur.framework.id.worker.RedisWorkerIdAssigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(SnowflakeProperties.class)
@Import(RedisWorkerIdAssigner.class)
public class SnowflakeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SnowflakeIdGenerator snowflakeIdGenerator(RedisWorkerIdAssigner workerIdAssigner,
                                                       SnowflakeProperties properties,
                                                       @Value("${spring.application.name:volur-service}") String serviceName) {
        long workerId;
        if (properties.isEnableRedisWorker()) {
            workerId = workerIdAssigner.assignWorkerId(serviceName);
        } else {
            workerId = properties.getWorkerId();
        }
        return new SnowflakeIdGenerator(workerId);
    }

}
