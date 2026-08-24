package com.humolix.volur.challenge.repository;

import com.humolix.volur.challenge.entity.Submission;
import com.humolix.volur.challenge.entity.enums.ChallengeType;
import com.humolix.volur.challenge.entity.enums.SceneType;
import com.humolix.volur.challenge.entity.enums.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Page<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId, Pageable pageable);

    Page<Submission> findByUserIdAndSceneTypeAndSceneIdOrderBySubmittedAtDesc(Long userId, SceneType sceneType, Long sceneId, Pageable pageable);

    Page<Submission> findByChallengeIdAndChallengeTypeAndStatusOrderBySubmittedAtAsc(Long challengeId, ChallengeType challengeType, SubmissionStatus status, Pageable pageable);

    List<Submission> findBySceneTypeAndSceneIdAndChallengeIdAndChallengeTypeAndUserIdOrderBySubmittedAtDesc(
            SceneType sceneType, Long sceneId, Long challengeId, ChallengeType challengeType, Long userId);

    @Query("SELECT s.challengeId FROM Submission s WHERE s.userId = :userId AND s.challengeType = :challengeType AND s.challengeId IN :challengeIds AND s.status = :status")
    List<Long> findSolvedChallengeIds(@Param("userId") Long userId, @Param("challengeType") ChallengeType challengeType,
                                      @Param("challengeIds") List<Long> challengeIds, @Param("status") SubmissionStatus status);

    Long countBySceneTypeAndSceneIdAndChallengeIdAndChallengeTypeAndStatus(
            SceneType sceneType, Long sceneId, Long challengeId, ChallengeType challengeType, SubmissionStatus status);

    boolean existsByChallengeIdAndChallengeTypeAndUserIdAndStatus(Long challengeId, ChallengeType challengeType, Long userId, SubmissionStatus status);

    void deleteByUserId(Long userId);

    @Modifying
    @Query("UPDATE Submission s SET s.deleted = :deleted, s.deletedAt = :deletedAt WHERE s.userId = :userId")
    void updateDeletedByUserId(@Param("userId") Long userId, @Param("deleted") boolean deleted, @Param("deletedAt") Instant deletedAt);

    void deleteBySceneTypeAndSceneId(SceneType sceneType, Long sceneId);

    long countByChallengeId(Long challengeId);

    List<Submission> findByChallengeId(Long challengeId);

}
