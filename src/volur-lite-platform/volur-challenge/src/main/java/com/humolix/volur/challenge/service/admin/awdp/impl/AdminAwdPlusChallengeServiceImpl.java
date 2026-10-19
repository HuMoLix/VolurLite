package com.humolix.volur.challenge.service.admin.awdp.impl;

import com.humolix.volur.challenge.domain.dto.awdp.AwdPlusChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.awdp.AwdPlusChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.entity.awdp.AwdPlusChallenge;
import com.humolix.volur.challenge.domain.vo.awdp.AwdPlusChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.awdp.AwdPlusChallengeListVO;
import com.humolix.volur.challenge.mapper.ChallengeMapper;
import com.humolix.volur.challenge.repository.awdp.AwdPlusChallengeRepository;
import com.humolix.volur.challenge.service.admin.awdp.AdminAwdPlusChallengeService;
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
@Transactional(readOnly = true)
public class AdminAwdPlusChallengeServiceImpl implements AdminAwdPlusChallengeService {

    private final AwdPlusChallengeRepository awdPlusRepository;
    private final UserRepository userRepository;
    private final ChallengeMapper challengeMapper;

    @Override
    public PageResponse<AwdPlusChallengeListVO> listChallenges(Integer page, Integer size, String category, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<AwdPlusChallenge> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category.trim()));
            }
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("name"), "%" + keyword.trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return PageResponse.of(awdPlusRepository.findAll(spec, pageable).map(challengeMapper::toAwdPlusListVO));
    }

    @Override
    public AwdPlusChallengeDetailVO getChallenge(Long id) {
        AwdPlusChallenge challenge = awdPlusRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "AWD+ 题目不存在"));
        return challengeMapper.toAwdPlusDetailVO(challenge);
    }

    @Override
    @Transactional
    public Long createChallenge(AwdPlusChallengeCreateRequestDTO request, Long authorId) {
        if (awdPlusRepository.existsByName(request.getName().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
        }

        User author = null;
        if (authorId != null) {
            author = userRepository.findById(authorId).orElse(null);
        }

        AwdPlusChallenge challenge = AwdPlusChallenge.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .author(author)
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : 1)
                .sourceCodeUrl(request.getSourceCodeUrl())
                .imageName(request.getImageName().trim())
                .containerPort(request.getContainerPort())
                .patchVerifyScript(request.getPatchVerifyScript())
                .exploitVerifyScript(request.getExploitVerifyScript())
                .build();

        return awdPlusRepository.save(challenge).getId();
    }

    @Override
    @Transactional
    public void updateChallenge(Long id, AwdPlusChallengeUpdateRequestDTO request) {
        AwdPlusChallenge challenge = awdPlusRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "AWD+ 题目不存在"));

        if (request.getName() != null && !request.getName().isEmpty() && !challenge.getName().equals(request.getName())) {
            if (awdPlusRepository.existsByNameAndIdNot(request.getName(), id)) {
                throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
            }
            challenge.setName(request.getName());
        }

        if (request.getDescription() != null) challenge.setDescription(request.getDescription());
        if (request.getCategory() != null) challenge.setCategory(request.getCategory());
        if (request.getDifficulty() != null) challenge.setDifficulty(request.getDifficulty());
        if (request.getSourceCodeUrl() != null) challenge.setSourceCodeUrl(request.getSourceCodeUrl());
        if (request.getImageName() != null) challenge.setImageName(request.getImageName().trim());
        if (request.getContainerPort() != null) challenge.setContainerPort(request.getContainerPort());
        if (request.getPatchVerifyScript() != null) challenge.setPatchVerifyScript(request.getPatchVerifyScript());
        if (request.getExploitVerifyScript() != null) challenge.setExploitVerifyScript(request.getExploitVerifyScript());

        awdPlusRepository.save(challenge);
    }

    @Override
    @Transactional
    public void deleteChallenge(Long id) {
        awdPlusRepository.deleteById(id);
    }

}
