package com.humolix.volur.security.session.generator;

import com.humolix.volur.framework.id.generator.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class DeviceSessionHashGenerator {

    private static final String DSH_PREFIX = "S-";
    private static final char DELIMITER = ':';
    private static final long BASE_CLUSTER_OFFSET = 100_000_000L;
    private static final long WORKER_ID_MULTIPLIER = 100_000L;
    private static final int MAX_LOCAL_SEQUENCE = 100_000;
    private static final Pattern DSH_PATTERN = Pattern.compile("^S-(\\d{9}):(\\d{13,19})$");

    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final AtomicInteger sequence = new AtomicInteger(0);
    private final AtomicLong lastTimestampMicro = new AtomicLong(0);

    public String generate() {
        long workerId = snowflakeIdGenerator.getWorkerId();
        int currentSeq = (sequence.getAndIncrement() & Integer.MAX_VALUE) % MAX_LOCAL_SEQUENCE;
        long clusterPrefix = BASE_CLUSTER_OFFSET + (workerId * WORKER_ID_MULTIPLIER) + currentSeq;
        long timestampMicro = generateMonotonicTimestampMicro();
        return DSH_PREFIX + clusterPrefix + DELIMITER + timestampMicro;
    }

    public boolean isValid(String dsh) {
        if (dsh == null || dsh.isEmpty()) {
            return false;
        }
        return DSH_PATTERN.matcher(dsh).matches();
    }

    public long extractWorkerId(String dsh) {
        if (!isValid(dsh)) {
            throw new IllegalArgumentException("Invalid device session hash format: " + dsh);
        }
        int colonIndex = dsh.indexOf(DELIMITER);
        long clusterPrefix = Long.parseLong(dsh.substring(DSH_PREFIX.length(), colonIndex));
        return (clusterPrefix - BASE_CLUSTER_OFFSET) / WORKER_ID_MULTIPLIER;
    }

    public long extractTimestampMillis(String dsh) {
        return extractTimestampMicro(dsh) / 1000L;
    }

    public long extractTimestampMicro(String dsh) {
        if (!isValid(dsh)) {
            throw new IllegalArgumentException("Invalid device session hash format: " + dsh);
        }
        int colonIndex = dsh.indexOf(DELIMITER);
        return Long.parseLong(dsh.substring(colonIndex + 1));
    }

    public boolean isExpired(String dsh, Duration ttl) {
        if (!isValid(dsh) || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return true;
        }
        long createdAtMillis = extractTimestampMillis(dsh);
        return (System.currentTimeMillis() - createdAtMillis) > ttl.toMillis();
    }

    private long generateMonotonicTimestampMicro() {
        long nowMicro = System.currentTimeMillis() * 1000L;
        while (true) {
            long last = lastTimestampMicro.get();
            long next = (nowMicro > last) ? nowMicro : last + 1;
            if (lastTimestampMicro.compareAndSet(last, next)) {
                return next;
            }
        }
    }

}
