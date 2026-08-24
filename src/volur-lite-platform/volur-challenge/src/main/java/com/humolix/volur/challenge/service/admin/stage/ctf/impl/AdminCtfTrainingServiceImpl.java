package com.humolix.volur.challenge.service.admin.stage.ctf.impl;

import com.humolix.volur.challenge.entity.stage.ctf.CtfChallenge;
import com.humolix.volur.challenge.entity.stage.ctf.CtfTrainingChallenge;
import com.humolix.volur.challenge.object.dto.stage.ctf.CtfTrainingChallengeCreateRequestDTO;
import com.humolix.volur.challenge.object.dto.stage.ctf.CtfTrainingChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.object.vo.admin.AdminTrainingChallengeListResponseVO;
import com.humolix.volur.challenge.repository.stage.ctf.CtfChallengeRepository;
import com.humolix.volur.challenge.repository.stage.ctf.CtfTrainingChallengeRepository;
import com.humolix.volur.challenge.service.admin.stage.ctf.AdminCtfTrainingService;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminCtfTrainingServiceImpl implements AdminCtfTrainingService {

    private final CtfTrainingChallengeRepository trainingChallengeRepository;
    private final CtfChallengeRepository challengeRepository;

    private AdminTrainingChallengeListResponseVO toVO(CtfTrainingChallenge tc) {
        if (tc == null) return null;
        CtfChallenge c = tc.getChallenge();
        return AdminTrainingChallengeListResponseVO.builder()
                .id(tc.getId())
                .challengeId(c != null ? c.getId() : null)
                .name(c != null ? c.getName() : null)
                .categoryName(c != null && c.getCategory() != null ? c.getCategory().getName() : null)
                .initialPoints(tc.getInitialPoints())
                .minimumPoints(tc.getMinimumPoints())
                .currentPoints(tc.getCurrentPoints())
                .solveCount(tc.getSolveCount())
                .dynamicScore(tc.isDynamicScore())
                .decayCoefficient(tc.getDecayCoefficient())
                .active(tc.isActive())
                .dynamicContainer(c != null && c.isDynamicContainer())
                .createdAt(tc.getCreatedAt())
                .build();
    }

    @Override
    public Page<AdminTrainingChallengeListResponseVO> pageTrainingChallenges(Integer page, Integer size, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);
        Specification<CtfTrainingChallenge> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("challenge").get("name"), "%" + keyword + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return trainingChallengeRepository.findAll(spec, pageable).map(this::toVO);
    }

    @Override
    @Transactional
    public AdminTrainingChallengeListResponseVO createTrainingChallenge(CtfTrainingChallengeCreateRequestDTO request) {
        CtfChallenge challenge = challengeRepository.findByIdAndDeletedFalse(request.getChallengeId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题目不存在"));

        if (trainingChallengeRepository.existsByChallengeId(request.getChallengeId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "该题目已被加入训练场");
        }

        CtfTrainingChallenge tc = CtfTrainingChallenge.builder()
                .challenge(challenge)
                .initialPoints(request.getInitialPoints())
                .minimumPoints(request.getMinimumPoints())
                .currentPoints(request.getInitialPoints())
                .solveCount(0)
                .dynamicScore(Boolean.TRUE.equals(request.getIsDynamicScore()))
                .decayCoefficient(request.getDecayCoefficient())
                .active(Boolean.TRUE.equals(request.getIsActive()))
                .cpuLimit(request.getCpuLimit())
                .memoryLimit(request.getMemoryLimit())
                .deleted(false)
                .build();

        return toVO(trainingChallengeRepository.save(tc));
    }

    @Override
    @Transactional
    public AdminTrainingChallengeListResponseVO updateTrainingChallenge(Long id, CtfTrainingChallengeUpdateRequestDTO request) {
        CtfTrainingChallenge tc = trainingChallengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "训练场题目不存在"));

        if (request.getInitialPoints() != null) tc.setInitialPoints(request.getInitialPoints());
        if (request.getMinimumPoints() != null) tc.setMinimumPoints(request.getMinimumPoints());
        if (request.getIsDynamicScore() != null) tc.setDynamicScore(request.getIsDynamicScore());
        if (request.getDecayCoefficient() != null) tc.setDecayCoefficient(request.getDecayCoefficient());
        if (request.getIsActive() != null) tc.setActive(request.getIsActive());
        if (request.getCpuLimit() != null) tc.setCpuLimit(request.getCpuLimit());
        if (request.getMemoryLimit() != null) tc.setMemoryLimit(request.getMemoryLimit());

        return toVO(trainingChallengeRepository.save(tc));
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        CtfTrainingChallenge tc = trainingChallengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "训练场题目不存在"));
        tc.setActive(!tc.isActive());
        trainingChallengeRepository.save(tc);
    }

    @Override
    @Transactional
    public void deleteTrainingChallenge(Long id) {
        CtfTrainingChallenge tc = trainingChallengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "训练场题目不存在"));
        tc.setDeleted(true);
        tc.setDeletedAt(Instant.now());
        trainingChallengeRepository.save(tc);
    }

}
