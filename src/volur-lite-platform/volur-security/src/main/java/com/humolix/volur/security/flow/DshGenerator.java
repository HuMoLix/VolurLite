package com.humolix.volur.security.flow;

import com.humolix.volur.framework.id.generator.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class DshGenerator {

    private static final long BASE_CLUSTER_OFFSET = 100_000_000L;
    private static final long WORKER_ID_MULTIPLIER = 100_000L;
    private static final int MAX_LOCAL_SEQUENCE = 100_000;

    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final AtomicInteger sequence = new AtomicInteger(0);

    public String generate() {
        long workerId = snowflakeIdGenerator.getWorkerId();
        int currentSeq = (sequence.getAndIncrement() & Integer.MAX_VALUE) % MAX_LOCAL_SEQUENCE;
        long clusterPrefix = BASE_CLUSTER_OFFSET + (workerId * WORKER_ID_MULTIPLIER) + currentSeq;
        long timestampMicro = System.currentTimeMillis() * 1000L + (currentSeq % 1000);
        return "S-" + clusterPrefix + ":" + timestampMicro;
    }

}
