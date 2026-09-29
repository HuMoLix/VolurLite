package com.humolix.volur.security.flow;

import com.humolix.volur.framework.id.generator.SnowflakeIdGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class DshGeneratorTest {

    @Test
    void generate_shouldCorrectlyEncodeWorkerIdAndMatchFormat() {
        long workerId = 5L;
        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(workerId);
        DshGenerator dshGenerator = new DshGenerator(snowflakeIdGenerator);

        String dsh = dshGenerator.generate();
        Assertions.assertNotNull(dsh);
        Assertions.assertTrue(dsh.startsWith("S-"));
        Assertions.assertTrue(dsh.contains(":"));

        String[] parts = dsh.substring(2).split(":");
        Assertions.assertEquals(2, parts.length);

        long clusterPrefix = Long.parseLong(parts[0]);
        Assertions.assertTrue(clusterPrefix >= 100_000_000L && clusterPrefix < 1_000_000_000L);

        long resolvedWorkerId = (clusterPrefix - 100_000_000L) / 100_000L;
        Assertions.assertEquals(workerId, resolvedWorkerId);
    }

    @Test
    void generate_shouldHandleMaxWorkerId() {
        long maxWorkerId = 1023L;
        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(maxWorkerId);
        DshGenerator dshGenerator = new DshGenerator(snowflakeIdGenerator);

        String dsh = dshGenerator.generate();
        String[] parts = dsh.substring(2).split(":");
        long clusterPrefix = Long.parseLong(parts[0]);

        Assertions.assertTrue(clusterPrefix >= 100_000_000L && clusterPrefix < 1_000_000_000L);
        long resolvedWorkerId = (clusterPrefix - 100_000_000L) / 100_000L;
        Assertions.assertEquals(maxWorkerId, resolvedWorkerId);
    }

    @Test
    void generate_concurrentGenerationShouldBeUnique() throws InterruptedException {
        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(2L);
        DshGenerator dshGenerator = new DshGenerator(snowflakeIdGenerator);

        int threadCount = 16;
        int perThreadCount = 500;
        int totalRequests = threadCount * perThreadCount;

        Set<String> generatedSet = ConcurrentHashMap.newKeySet(totalRequests);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < perThreadCount; j++) {
                        generatedSet.add(dshGenerator.generate());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Assertions.assertEquals(totalRequests, generatedSet.size());
    }

}
