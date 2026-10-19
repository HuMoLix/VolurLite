package com.humolix.volur.framework.id.generator;

import com.humolix.volur.framework.id.constant.SnowflakeConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest {

    @Test
    @DisplayName("Test basic ID generation monotonicity")
    void testBasicIdGeneration() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1L);
        long lastId = 0;
        for (int i = 0; i < 1000; i++) {
            long id = generator.nextId();
            assertTrue(id > lastId, "Generated ID must be strictly greater than previous ID");
            lastId = id;
        }
    }

    @Test
    @DisplayName("Test concurrent ID generation with 0 collision")
    void testConcurrentIdGeneration() throws InterruptedException {
        int threadCount = 16;
        int idsPerThread = 10_000;
        int totalExpectedIds = threadCount * idsPerThread;

        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(2L);
        Set<Long> generatedIds = ConcurrentHashMap.newKeySet(totalExpectedIds);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        generatedIds.add(generator.nextId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "ID generation tasks timed out");
        assertEquals(totalExpectedIds, generatedIds.size(), "All generated IDs in high concurrency must be distinct");
    }

    @Test
    @DisplayName("Test invalid worker ID validation")
    void testInvalidWorkerId() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1L));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(SnowflakeConstants.MAX_WORKER_ID + 1));
    }

}
