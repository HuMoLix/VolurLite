package com.humolix.volur.training.repository;

import com.humolix.volur.training.domain.entity.TrainingSolveRecord;
import com.humolix.volur.training.domain.enums.TrainingChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingSolveRecordRepository extends JpaRepository<TrainingSolveRecord, Long> {

    boolean existsByUserIdAndSourceChallengeId(Long userId, Long sourceChallengeId);

    Optional<TrainingSolveRecord> findByUserIdAndSourceChallengeId(Long userId, Long sourceChallengeId);

    List<TrainingSolveRecord> findByUserIdAndSourceChallengeIdIn(Long userId, Collection<Long> sourceChallengeIds);

    boolean existsBySourceChallengeId(Long sourceChallengeId);

    /** 每题解出人数（返回 [sourceChallengeId, count]） */
    @Query("SELECT r.sourceChallengeId, COUNT(r) FROM TrainingSolveRecord r WHERE r.sourceChallengeId IN :ids GROUP BY r.sourceChallengeId")
    List<Object[]> countSolvesBySourceChallengeIds(@Param("ids") Collection<Long> ids);

    /**
     * 排行榜聚合（只统计当前训练池内 ENABLED 的题）。
     * 返回 [userId, 总积分(Long), 解出数(Long), 最后解出时间(Instant)]
     */
    @Query("SELECT r.userId, SUM(r.pointsEarned), COUNT(r), MAX(r.solvedAt) FROM TrainingSolveRecord r " +
            "WHERE r.sourceChallengeId IN (SELECT t.sourceChallengeId FROM TrainingChallenge t WHERE t.status = :status) " +
            "GROUP BY r.userId")
    List<Object[]> aggregateLeaderboard(@Param("status") TrainingChallengeStatus status);

    /** 单用户聚合（行结构同上），无记录返回空列表 */
    @Query("SELECT r.userId, SUM(r.pointsEarned), COUNT(r), MAX(r.solvedAt) FROM TrainingSolveRecord r " +
            "WHERE r.userId = :userId AND r.sourceChallengeId IN (SELECT t.sourceChallengeId FROM TrainingChallenge t WHERE t.status = :status) " +
            "GROUP BY r.userId")
    List<Object[]> aggregateForUser(@Param("userId") Long userId, @Param("status") TrainingChallengeStatus status);

    /** 参与人数（有过解题记录的用户数，用于榜外用户的名次兜底） */
    @Query("SELECT COUNT(DISTINCT r.userId) FROM TrainingSolveRecord r " +
            "WHERE r.sourceChallengeId IN (SELECT t.sourceChallengeId FROM TrainingChallenge t WHERE t.status = :status)")
    long countParticipants(@Param("status") TrainingChallengeStatus status);

}
