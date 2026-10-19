package com.humolix.volur.competition.service.admin.ctf.impl;

import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeRepository;
import com.humolix.volur.competition.domain.dto.ctf.CtfChallengeBatchMountRequestDTO;
import com.humolix.volur.competition.domain.dto.ctf.CtfChallengeMountRequestDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionChallenge;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionChallengeVO;
import com.humolix.volur.competition.mapper.CompetitionMapper;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionChallengeRepository;
import com.humolix.volur.competition.service.admin.ctf.AdminCtfMountService;
import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
import com.humolix.volur.framework.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCtfMountServiceImpl implements AdminCtfMountService {

    private final CompetitionRepository competitionRepository;
    private final CtfChallengeRepository rawChallengeRepository;
    private final CtfCompetitionChallengeRepository compChallengeRepository;
    private final CompetitionMapper competitionMapper;
    private final VolurCacheTemplate cacheTemplate;

    @Override
    public List<CtfCompetitionChallengeVO> listMountedChallenges(Long competitionId) {
        List<CtfCompetitionChallenge> list = compChallengeRepository.findByCompetitionIdOrderBySortOrderAsc(competitionId);
        return competitionMapper.toCtfChallengeVOList(list);
    }

    @Override
    @Transactional
    public void mountChallenge(Long competitionId, CtfChallengeMountRequestDTO request) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        if (competition.getType() != CompetitionType.CTF) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该竞赛类型不是CTF赛制，无法挂载CTF赛题");
        }

        if (compChallengeRepository.existsByCompetitionIdAndChallengeId(competitionId, request.getChallengeId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "该赛题已挂载至本场竞赛");
        }

        CtfChallenge challenge = rawChallengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题库中题目不存在"));

        int initial = request.getInitialPoints() != null ? request.getInitialPoints() : 1000;
        int min = request.getMinPoints() != null ? request.getMinPoints() : 100;
        int decay = request.getDecayCount() != null ? request.getDecayCount() : 30;

        CtfCompetitionChallenge compChal = CtfCompetitionChallenge.builder()
                .competition(competition)
                .challenge(challenge)
                .initialPoints(initial)
                .minPoints(min)
                .decayCount(decay)
                .currentPoints(initial)
                .solvesCount(0)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isVisible(request.getIsVisible() != null ? request.getIsVisible() : true)
                .build();

        compChallengeRepository.save(compChal);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "ctf:challenges:" + competitionId);
    }

    @Override
    @Transactional
    public void batchMountChallenges(Long competitionId, CtfChallengeBatchMountRequestDTO request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return;
        }
        for (CtfChallengeMountRequestDTO item : request.getItems()) {
            mountChallenge(competitionId, item);
        }
    }

    @Override
    @Transactional
    public void updateMountedChallenge(Long competitionId, Long competitionChallengeId, CtfChallengeMountRequestDTO request) {
        CtfCompetitionChallenge compChal = compChallengeRepository.findById(competitionChallengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "赛题不存在"));
        if (request.getInitialPoints() != null) {
            compChal.setInitialPoints(request.getInitialPoints());
            if (compChal.getSolvesCount() == null || compChal.getSolvesCount() == 0) {
                compChal.setCurrentPoints(request.getInitialPoints());
            }
        }
        if (request.getMinPoints() != null) compChal.setMinPoints(request.getMinPoints());
        if (request.getDecayCount() != null) compChal.setDecayCount(request.getDecayCount());
        if (request.getSortOrder() != null) compChal.setSortOrder(request.getSortOrder());
        if (request.getIsVisible() != null) compChal.setIsVisible(request.getIsVisible());
        if (compChal.getSolvesCount() != null && compChal.getSolvesCount() > 0) {
            double decay = Math.pow((double) (compChal.getSolvesCount() - 1) / (double) Math.max(1, compChal.getDecayCount()), 2);
            double points = compChal.getMinPoints() + (compChal.getInitialPoints() - compChal.getMinPoints()) * (1.0 / (1.0 + decay));
            compChal.setCurrentPoints((int) Math.round(Math.max(compChal.getMinPoints(), points)));
        }
        compChallengeRepository.save(compChal);

        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "ctf:challenges:" + competitionId);
        cacheTemplate.evict(CacheNames.CTF_CHALLENGE_DETAIL, "comp:" + competitionId + ":chal:" + competitionChallengeId);
    }

    @Override
    @Transactional
    public void unmountChallenge(Long competitionId, Long competitionChallengeId) {
        compChallengeRepository.deleteById(competitionChallengeId);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "ctf:challenges:" + competitionId);
        cacheTemplate.evict(CacheNames.CTF_CHALLENGE_DETAIL, "comp:" + competitionId + ":chal:" + competitionChallengeId);
    }

}
