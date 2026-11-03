package com.humolix.volur.framework.id.generator;

import com.humolix.volur.framework.id.constant.SnowflakeConstants;
import com.humolix.volur.framework.id.exception.SnowflakeClockBackwardsException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class SnowflakeIdGenerator implements IdGenerator {

    @Getter
    private final long workerId;

    @SuppressWarnings("unused")
    private p8 p1, p2, p3, p4, p5, p6, p7;

    private final AtomicReference<State> stateRef;

    @SuppressWarnings("unused")
    private p8 p8, p9, p10, p11, p12, p13, p14;

    private static class p8 {
        @SuppressWarnings("unused")
        long p1, p2, p3, p4, p5, p6, p7;
    }

    private record State(long lastTimestamp, long sequence) {
    }

    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > SnowflakeConstants.MAX_WORKER_ID) {
            throw new IllegalArgumentException("Worker ID out of range: " + workerId);
        }
        this.workerId = workerId;
        this.stateRef = new AtomicReference<>(new State(-1L, 0L));
        log.info("[SnowflakeIdGenerator] Initialized with CAS & Cache Line Padding, WorkerId: {}", workerId);
    }

    @Override
    public long nextId() {
        while (true) {
            long currentTimestamp = timeGen();
            State currentState = stateRef.get();
            long lastTimestamp = currentState.lastTimestamp;
            long sequence = currentState.sequence;

            if (currentTimestamp < lastTimestamp) {
                long offset = lastTimestamp - currentTimestamp;
                if (offset <= SnowflakeConstants.MAX_BACKWARD_MS) {
                    currentTimestamp = tilNextMillis(lastTimestamp);
                } else {
                    throw new SnowflakeClockBackwardsException("Clock moved backwards by " + offset + " ms, refusing to generate id");
                }
            }

            long newSequence;
            long newTimestamp = currentTimestamp;

            if (lastTimestamp == currentTimestamp) {
                newSequence = (sequence + 1) & SnowflakeConstants.MAX_SEQUENCE;
                if (newSequence == 0) {
                    newTimestamp = tilNextMillis(lastTimestamp);
                }
            } else {
                newSequence = ThreadLocalRandom.current().nextLong(0, 10);
            }

            State nextState = new State(newTimestamp, newSequence);
            if (stateRef.compareAndSet(currentState, nextState)) {
                return ((newTimestamp - SnowflakeConstants.START_TIMESTAMP) << SnowflakeConstants.TIMESTAMP_LEFT_SHIFT)
                        | (this.workerId << SnowflakeConstants.WORKER_ID_SHIFT)
                        | newSequence;
            }

            Thread.onSpinWait();
        }
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            Thread.onSpinWait();
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

}
