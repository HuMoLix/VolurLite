package com.humolix.volur.security.session.generator;

import com.humolix.volur.framework.id.generator.SnowflakeIdGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class DeviceSessionHashGeneratorTest {

    @Test
    void generate_shouldCorrectlyEncodeWorkerIdAndMatchFormat() {
        long workerId = 5L;
        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(workerId);
        DeviceSessionHashGenerator generator = new DeviceSessionHashGenerator(snowflakeIdGenerator);

        long beforeMs = System.currentTimeMillis();
        String dsh = generator.generate();
        long afterMs = System.currentTimeMillis();

        Assertions.assertNotNull(dsh);
        Assertions.assertTrue(generator.isValid(dsh));
        Assertions.assertTrue(dsh.startsWith("S-"));
        Assertions.assertTrue(dsh.contains(":"));

        long resolvedWorkerId = generator.extractWorkerId(dsh);
        Assertions.assertEquals(workerId, resolvedWorkerId);

        long timestampMillis = generator.extractTimestampMillis(dsh);
        Assertions.assertTrue(timestampMillis >= beforeMs && timestampMillis <= afterMs);
        Assertions.assertFalse(generator.isExpired(dsh, Duration.ofMinutes(15)));
    }

    @Test
    void generate_shouldHandleMaxWorkerId() {
        long maxWorkerId = 1023L;
        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(maxWorkerId);
        DeviceSessionHashGenerator generator = new DeviceSessionHashGenerator(snowflakeIdGenerator);

        String dsh = generator.generate();
        Assertions.assertTrue(generator.isValid(dsh));

        long resolvedWorkerId = generator.extractWorkerId(dsh);
        Assertions.assertEquals(maxWorkerId, resolvedWorkerId);
    }

    @Test
    void generate_concurrentGenerationShouldBeUnique() throws InterruptedException {
        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(2L);
        DeviceSessionHashGenerator generator = new DeviceSessionHashGenerator(snowflakeIdGenerator);

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
                        String dsh = generator.generate();
                        generatedSet.add(dsh);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Assertions.assertEquals(totalRequests, generatedSet.size());
        for (String dsh : generatedSet) {
            Assertions.assertTrue(generator.isValid(dsh));
        }
    }

    @Test
    void isValid_shouldCorrectlyIdentifyValidAndInvalidDsh() {
        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(1L);
        DeviceSessionHashGenerator generator = new DeviceSessionHashGenerator(snowflakeIdGenerator);

        Assertions.assertTrue(generator.isValid("S-100500042:1790902374919000"));
        Assertions.assertTrue(generator.isValid("S-202399999:1790902374919000"));
        Assertions.assertFalse(generator.isValid(null));
        Assertions.assertFalse(generator.isValid(""));
        Assertions.assertFalse(generator.isValid("X-100500042:1790902374919000"));
        Assertions.assertFalse(generator.isValid("S-10050:1790902374919000"));
        Assertions.assertFalse(generator.isValid("S-100500042"));
        Assertions.assertFalse(generator.isValid("S-100500042:"));
        Assertions.assertFalse(generator.isValid("S-100500042:abc"));
    }

    @Test
    void extractMethods_shouldThrowOnInvalidDsh() {
        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(1L);
        DeviceSessionHashGenerator generator = new DeviceSessionHashGenerator(snowflakeIdGenerator);

        Assertions.assertThrows(IllegalArgumentException.class, () -> generator.extractWorkerId("invalid-dsh"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generator.extractTimestampMillis("invalid-dsh"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generator.extractTimestampMicro("invalid-dsh"));
    }

    @Test
    void isExpired_shouldAccuratelyDetermineExpiration() {
        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(1L);
        DeviceSessionHashGenerator generator = new DeviceSessionHashGenerator(snowflakeIdGenerator);

        String currentDsh = generator.generate();
        Assertions.assertFalse(generator.isExpired(currentDsh, Duration.ofMinutes(15)));

        long oneHourAgoMicro = (System.currentTimeMillis() - 3600_000L) * 1000L;
        String pastDsh = "S-100100000:" + oneHourAgoMicro;
        Assertions.assertTrue(generator.isExpired(pastDsh, Duration.ofMinutes(15)));
        Assertions.assertTrue(generator.isExpired("invalid", Duration.ofMinutes(15)));
        Assertions.assertTrue(generator.isExpired(currentDsh, null));
    }

}
