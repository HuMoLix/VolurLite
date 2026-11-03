package com.humolix.volur.framework.id.constant;

public final class SnowflakeConstants {

    private SnowflakeConstants() {
    }

    public static final long START_TIMESTAMP = 1704067200000L;

    public static final long WORKER_ID_BITS = 10L;

    public static final long SEQUENCE_BITS = 12L;

    public static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    public static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    public static final long MAX_BACKWARD_MS = 5L;

    public static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    public static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    public static final String VREDIS_FRAMEWORK_SNOWFLAKE_WORKER_KEY_PREFIX = "vredis:framework:snowflake_worker:";

    @Deprecated
    public static final String REDIS_WORKER_KEY_PREFIX = VREDIS_FRAMEWORK_SNOWFLAKE_WORKER_KEY_PREFIX;

}
