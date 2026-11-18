package com.humolix.volur.training.service.spi;

import com.humolix.volur.challenge.service.spi.ChallengeDeletionGuard;
import com.humolix.volur.training.repository.TrainingChallengeRepository;
import com.humolix.volur.training.repository.TrainingSolveRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 题库删除防护：题库题目在训练池中，或存在历史解题记录时，禁止从题库物理删除。
 */
@Component
@RequiredArgsConstructor
public class TrainingChallengeDeletionGuard implements ChallengeDeletionGuard {

    private final TrainingChallengeRepository trainingChallengeRepository;
    private final TrainingSolveRecordRepository solveRecordRepository;

    @Override
    public boolean isReferenced(Long challengeId) {
        return trainingChallengeRepository.existsBySourceChallengeId(challengeId)
                || solveRecordRepository.existsBySourceChallengeId(challengeId);
    }

}
