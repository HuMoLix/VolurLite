package com.humolix.volur.challenge.service.spi;

/**
 * 题库删除防护端口：由引用题库题目的模块（如 volur-training）实现，
 * 题库物理删除前校验是否被引用，避免训练池/解题记录悬空。
 * 接口放在题库模块、实现在上层模块，依赖方向不反转。
 */
public interface ChallengeDeletionGuard {

    /** @return true 表示该题库题目仍被引用，应拒绝物理删除 */
    boolean isReferenced(Long challengeId);

}
