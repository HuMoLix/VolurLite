package com.humolix.volur.challenge.service.admin.theory.impl;

import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.theory.TheoryChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.entity.theory.TheoryChallenge;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeListVO;
import com.humolix.volur.challenge.mapper.ChallengeMapper;
import com.humolix.volur.challenge.repository.theory.TheoryChallengeRepository;
import com.humolix.volur.challenge.service.admin.theory.AdminTheoryChallengeService;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTheoryChallengeServiceImpl implements AdminTheoryChallengeService {

    private final TheoryChallengeRepository theoryRepository;
    private final UserRepository userRepository;
    private final ChallengeMapper challengeMapper;

    @Override
    public PageResponse<TheoryChallengeListVO> listChallenges(Integer page, Integer size, TheoryType type, String category, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<TheoryChallenge> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category.trim()));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), pattern),
                        cb.like(root.get("content"), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return PageResponse.of(theoryRepository.findAll(spec, pageable).map(challengeMapper::toTheoryListVO));
    }

    @Override
    public TheoryChallengeDetailVO getChallenge(Long id) {
        TheoryChallenge challenge = theoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "理论题目不存在"));
        return challengeMapper.toTheoryDetailVO(challenge);
    }

    @Override
    @Transactional
    public Long createChallenge(TheoryChallengeCreateRequestDTO request, Long authorId) {
        if (theoryRepository.existsByName(request.getName().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
        }

        User author = null;
        if (authorId != null) {
            author = userRepository.findById(authorId).orElse(null);
        }

        TheoryChallenge challenge = TheoryChallenge.builder()
                .name(request.getName().trim())
                .category(request.getCategory())
                .type(request.getType())
                .content(request.getContent())
                .options(request.getOptions())
                .standardAnswer(request.getStandardAnswer().trim())
                .explanation(request.getExplanation())
                .score(request.getScore() != null ? request.getScore() : 5)
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : 1)
                .author(author)
                .build();

        return theoryRepository.save(challenge).getId();
    }

    @Override
    @Transactional
    public void updateChallenge(Long id, TheoryChallengeUpdateRequestDTO request) {
        TheoryChallenge challenge = theoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "理论题目不存在"));

        if (StringUtils.hasText(request.getName()) && !challenge.getName().equals(request.getName().trim())) {
            if (theoryRepository.existsByName(request.getName().trim())) {
                throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
            }
            challenge.setName(request.getName().trim());
        }

        if (request.getCategory() != null) challenge.setCategory(request.getCategory());
        if (request.getType() != null) challenge.setType(request.getType());
        if (request.getContent() != null) challenge.setContent(request.getContent());
        if (request.getOptions() != null) challenge.setOptions(request.getOptions());
        if (request.getStandardAnswer() != null) challenge.setStandardAnswer(request.getStandardAnswer().trim());
        if (request.getExplanation() != null) challenge.setExplanation(request.getExplanation());
        if (request.getScore() != null) challenge.setScore(request.getScore());
        if (request.getDifficulty() != null) challenge.setDifficulty(request.getDifficulty());

        theoryRepository.save(challenge);
    }

    @Override
    @Transactional
    public void deleteChallenge(Long id) {
        theoryRepository.deleteById(id);
    }

}
