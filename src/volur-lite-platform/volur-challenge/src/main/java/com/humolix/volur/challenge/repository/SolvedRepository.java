package com.humolix.volur.challenge.repository;

import com.humolix.volur.challenge.entity.Solved;
import com.humolix.volur.challenge.entity.enums.SceneType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SolvedRepository extends JpaRepository<Solved, Long> {

    Optional<Solved> findByUserIdAndSceneTypeAndSceneIdAndChallengeId(Long userId, SceneType sceneType, Long sceneId, Long challengeId);

    List<Solved> findByUserIdAndSceneTypeAndSceneId(Long userId, SceneType sceneType, Long sceneId);

    Boolean existsByUserIdAndSceneTypeAndSceneIdAndChallengeId(Long userId, SceneType sceneType, Long sceneId, Long challengeId);

    Page<Solved> findByChallengeIdAndSceneTypeAndSceneIdOrderBySolvedAtAsc(Long challengeId, SceneType sceneType, Long sceneId, Pageable pageable);

    @Query("SELECT s.challengeId FROM Solved s WHERE s.userId = :userId AND s.sceneType = :sceneType AND s.sceneId = :sceneId AND s.challengeId IN :challengeIds")
    List<Long> findSolvedChallengeIds(@Param("userId") Long userId, @Param("sceneType") SceneType sceneType,
                                      @Param("sceneId") Long sceneId, @Param("challengeIds") Set<Long> challengeIds);

    long countByChallengeId(Long challengeId);

    List<Solved> findByChallengeId(Long challengeId);

}
