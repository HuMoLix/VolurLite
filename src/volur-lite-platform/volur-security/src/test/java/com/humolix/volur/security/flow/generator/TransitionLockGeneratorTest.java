package com.humolix.volur.security.flow.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class TransitionLockGeneratorTest {

    private final TransitionLockGenerator generator = new TransitionLockGenerator();

    @Test
    void generate_shouldProduceValidFormatAndEntropy() {
        String tl = generator.generate("S-100200300:1790000000000", 1, "pwd", "user@ctf.local");

        Assertions.assertNotNull(tl);
        Assertions.assertTrue(generator.isValid(tl));
        Assertions.assertTrue(tl.startsWith(TransitionLockGenerator.TL_PREFIX));
        Assertions.assertEquals(64, tl.length());
    }

    @Test
    void generate_consecutiveCallsShouldDifferDueToNonce() {
        String tl1 = generator.generate("S-100200300:1790000000000", 1, "pwd", "user@ctf.local");
        String tl2 = generator.generate("S-100200300:1790000000000", 1, "pwd", "user@ctf.local");

        Assertions.assertNotNull(tl1);
        Assertions.assertNotNull(tl2);
        Assertions.assertNotEquals(tl1, tl2);
        Assertions.assertTrue(generator.isValid(tl1));
        Assertions.assertTrue(generator.isValid(tl2));
    }

    @Test
    void generate_handlesNullParametersGracefully() {
        String tl = generator.generate(null, 0, null, null);

        Assertions.assertNotNull(tl);
        Assertions.assertTrue(generator.isValid(tl));
        Assertions.assertEquals(64, tl.length());
    }

    @Test
    void isValid_shouldCorrectlyDetectValidAndInvalidTokens() {
        String validTl = generator.generate("S-100200300:1790000000000", 0, "init", null);
        Assertions.assertTrue(generator.isValid(validTl));

        Assertions.assertFalse(generator.isValid(null));
        Assertions.assertFalse(generator.isValid(""));
        Assertions.assertFalse(generator.isValid("ADG-"));
        Assertions.assertFalse(generator.isValid("ADG-SHORT"));
        Assertions.assertFalse(generator.isValid("XYZ-" + validTl.substring(4)));
        Assertions.assertFalse(generator.isValid(validTl + "EXTRA"));
        Assertions.assertFalse(generator.isValid("ADG-" + "A".repeat(59)));
        Assertions.assertFalse(generator.isValid("ADG-" + "A".repeat(61)));
        Assertions.assertFalse(generator.isValid("ADG-" + "A".repeat(58) + "$#"));
    }

    @Test
    void generate_concurrencyShouldProduceAllUniqueTokens() throws InterruptedException {
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
                        String tl = generator.generate("S-100200300:1790000000000", 1, "pwd", "user@ctf.local");
                        generatedSet.add(tl);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Assertions.assertEquals(totalRequests, generatedSet.size());
        for (String tl : generatedSet) {
            Assertions.assertTrue(generator.isValid(tl));
        }
    }

}
