package com.humolix.volur.framework.id.worker;

import com.humolix.volur.framework.id.config.SnowflakeProperties;
import com.humolix.volur.framework.id.constant.SnowflakeConstants;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisWorkerIdAssigner implements WorkerIdAssigner {

    private final StringRedisTemplate redisTemplate;
    private final SnowflakeProperties properties;

    private Long allocatedWorkerId = null;
    private String currentServiceKey;
    private String instanceIdentifier;

    private static final String LUA_ASSIGN_SCRIPT =
            """
                    local prefix = KEYS[1]
                    local instanceId = ARGV[1]
                    local ttl = tonumber(ARGV[2])
                    for id = 0, 1023 do
                        local key = prefix .. id
                        if redis.call('EXISTS', key) == 0 then
                            redis.call('SET', key, instanceId, 'EX', ttl)
                            return id
                        end
                    end
                    return -1""";

    private static final String LUA_RENEW_SCRIPT =
            """
                    local current = redis.call('GET', KEYS[1])
                    if current == ARGV[1] then
                        redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
                        return 1
                    else
                        return 0
                    end""";

    private static final String LUA_RELEASE_SCRIPT =
            """
                    local current = redis.call('GET', KEYS[1])
                    if current == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    else
                        return 0
                    end""";

    @Override
    public synchronized long assignWorkerId(String serviceName) {
        if (allocatedWorkerId != null) {
            return allocatedWorkerId;
        }

        this.instanceIdentifier = getHostAddress() + ":" + UUID.randomUUID().toString().substring(0, 8);
        String prefix = SnowflakeConstants.VREDIS_FRAMEWORK_SNOWFLAKE_WORKER_KEY_PREFIX + serviceName + ":";

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_ASSIGN_SCRIPT, Long.class);
            Long res = redisTemplate.execute(script, Collections.singletonList(prefix),
                    instanceIdentifier, String.valueOf(properties.getWorkerTtlSeconds()));

            if (res != null && res >= 0) {
                this.allocatedWorkerId = res;
                this.currentServiceKey = prefix + allocatedWorkerId;
                log.info("[RedisWorkerIdAssigner] Successfully assigned WorkerID [{}] for service [{}] via Lua", allocatedWorkerId, serviceName);
                return allocatedWorkerId;
            }
        } catch (Exception e) {
            log.error("[RedisWorkerIdAssigner] Redis Lua script execution failed for service [{}]", serviceName, e);
        }

        long fallbackId = Math.abs(instanceIdentifier.hashCode()) % (SnowflakeConstants.MAX_WORKER_ID + 1);
        log.warn("[RedisWorkerIdAssigner] Fallback to Hash WorkerID [{}] for service [{}]", fallbackId, serviceName);
        this.allocatedWorkerId = fallbackId;
        return fallbackId;
    }

    @Scheduled(fixedRate = 10000)
    public void renewWorkerId() {
        if (allocatedWorkerId != null && currentServiceKey != null && instanceIdentifier != null) {
            try {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_RENEW_SCRIPT, Long.class);
                Long res = redisTemplate.execute(script, Collections.singletonList(currentServiceKey),
                        instanceIdentifier, String.valueOf(properties.getWorkerTtlSeconds()));
                if (res == null || res == 0) {
                    log.warn("[RedisWorkerIdAssigner] Failed to renew WorkerID key [{}], ownership lost", currentServiceKey);
                }
            } catch (Exception e) {
                log.warn("[RedisWorkerIdAssigner] Error renewing WorkerID key [{}]", currentServiceKey, e);
            }
        }
    }

    @PreDestroy
    public void releaseWorkerId() {
        if (allocatedWorkerId != null && currentServiceKey != null && instanceIdentifier != null) {
            try {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_RELEASE_SCRIPT, Long.class);
                redisTemplate.execute(script, Collections.singletonList(currentServiceKey), instanceIdentifier);
                log.info("[RedisWorkerIdAssigner] Released WorkerID key [{}]", currentServiceKey);
            } catch (Exception e) {
                log.warn("[RedisWorkerIdAssigner] Failed to release WorkerID key [{}]", currentServiceKey, e);
            }
        }
    }

    private String getHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

}
