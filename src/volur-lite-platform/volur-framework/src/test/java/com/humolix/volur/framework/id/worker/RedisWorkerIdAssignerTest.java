package com.humolix.volur.framework.id.worker;

import com.humolix.volur.framework.id.config.SnowflakeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisWorkerIdAssignerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisConnectionFactory connectionFactory;

    private SnowflakeProperties properties;
    private RedisWorkerIdAssigner assigner;

    @BeforeEach
    void setUp() {
        properties = new SnowflakeProperties();
        properties.setWorkerTtlSeconds(30L);
        assigner = new RedisWorkerIdAssigner(redisTemplate, properties);
    }

    @Test
    @DisplayName("Assign WorkerID successfully via Redis Lua script")
    void testAssignWorkerIdSuccess() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(5L);

        long workerId = assigner.assignWorkerId("volur-server");

        assertEquals(5L, workerId);
        assertTrue(assigner.isRunning());

        // Second call should return cached allocatedWorkerId without invoking redis
        long cachedId = assigner.assignWorkerId("volur-server");
        assertEquals(5L, cachedId);
        verify(redisTemplate, times(1)).execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString());
    }

    @Test
    @DisplayName("Assign WorkerID fallback when Redis execution fails")
    void testAssignWorkerIdFallback() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis connection error"));

        long workerId = assigner.assignWorkerId("volur-server");

        assertTrue(workerId >= 0 && workerId <= 1023);
        assertTrue(assigner.isRunning());
    }

    @Test
    @DisplayName("Renew WorkerID normal renewal (res == 1)")
    void testRenewWorkerIdNormal() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(0L) // initial assignment
                .thenReturn(1L); // renew result

        assigner.assignWorkerId("volur-server");
        assertDoesNotThrow(() -> assigner.renewWorkerId());
    }

    @Test
    @DisplayName("Renew WorkerID self-healing recovery after expiration/sleep (res == 2)")
    void testRenewWorkerIdSelfHealingRecovery() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(0L) // initial assignment
                .thenReturn(2L); // recovery result

        assigner.assignWorkerId("volur-server");
        assertDoesNotThrow(() -> assigner.renewWorkerId());
    }

    @Test
    @DisplayName("Renew WorkerID ownership lost (res == 0)")
    void testRenewWorkerIdOwnershipLost() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(0L) // initial assignment
                .thenReturn(0L); // ownership lost

        assigner.assignWorkerId("volur-server");
        assertDoesNotThrow(() -> assigner.renewWorkerId());
    }

    @Test
    @DisplayName("Stop and release WorkerID lifecycle")
    void testStopAndReleaseWorkerId() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(3L);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                .thenReturn(1L);

        assigner.assignWorkerId("volur-server");
        assertTrue(assigner.isRunning());

        assigner.stop();
        assertFalse(assigner.isRunning());
        verify(redisTemplate, times(1)).execute(any(DefaultRedisScript.class), anyList(), anyString());
    }
}
