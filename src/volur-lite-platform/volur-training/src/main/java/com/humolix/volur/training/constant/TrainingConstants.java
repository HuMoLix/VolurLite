package com.humolix.volur.training.constant;

import com.humolix.volur.challenge.domain.enums.ChallengeDifficulty;

import java.time.Duration;
import java.util.Map;

/**
 * 训练模块常量：难度默认分值、flag 提交限流与靶机启动限速参数。
 * <p>详见 docs/training-api.md §6、§7。
 */
public final class TrainingConstants {

    private TrainingConstants() {
    }

    /** 导入时按难度赋的默认分值（管理端可单题修改） */
    public static final Map<ChallengeDifficulty, Integer> DEFAULT_POINTS_BY_DIFFICULTY = Map.of(
            ChallengeDifficulty.EASY, 100,
            ChallengeDifficulty.NORMAL, 200,
            ChallengeDifficulty.MID, 300,
            ChallengeDifficulty.HARD, 400,
            ChallengeDifficulty.DIFFICULT, 500
    );

    /** flag 提交全局限速：每用户每窗口最多 N 次（所有题共享额度） */
    public static final int FLAG_GLOBAL_LIMIT = 1;
    public static final Duration FLAG_GLOBAL_WINDOW = Duration.ofSeconds(2);

    /** 单题冷却：答错后同题冷却窗口 */
    public static final Duration FLAG_COOLDOWN = Duration.ofSeconds(5);

    /** 错误锁定：同题累计错 N 次锁题 */
    public static final int FLAG_WRONG_LOCK_THRESHOLD = 10;
    public static final Duration FLAG_WRONG_COUNT_WINDOW = Duration.ofMinutes(5);
    public static final Duration FLAG_LOCK_DURATION = Duration.ofMinutes(5);

    /** 靶机启动限速：每用户每窗口最多 N 次 */
    public static final int INSTANCE_START_LIMIT = 1;
    public static final Duration INSTANCE_START_WINDOW = Duration.ofSeconds(10);

    /** Redis key 前缀 */
    public static final String KEY_FLAG_SUBMIT = "training:rate:submit:";
    public static final String KEY_FLAG_COOLDOWN = "training:rate:cooldown:";
    public static final String KEY_FLAG_WRONG = "training:rate:wrong:";
    public static final String KEY_FLAG_LOCK = "training:rate:lock:";
    public static final String KEY_INSTANCE_START = "training:rate:instance:";

    /** 训练模块错误码（6 位规范 A-B-C-D-EE，模块码 9），详见 docs/errorCode.md 与 docs/training-api.md §8 */
    public static final int EC_TRAINING_CHALLENGE_NOT_FOUND = 153901;
    public static final int EC_TRAINING_CHALLENGE_DISABLED = 153902;
    public static final int EC_IMPORT_SOURCE_NOT_FOUND = 153903;
    public static final int EC_NO_RUNNING_INSTANCE = 153904;
    public static final int EC_FLAG_GLOBAL_RATE_LIMITED = 123901;
    public static final int EC_FLAG_COOLDOWN = 123902;
    public static final int EC_FLAG_LOCKED = 123903;
    public static final int EC_INSTANCE_START_RATE_LIMITED = 123905;
    public static final int EC_CHALLENGE_REFERENCED_BY_TRAINING = 153905;

}
