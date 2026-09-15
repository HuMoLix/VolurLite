package com.humolix.volur.challenge.service.admin.ctf.impl;

import com.humolix.volur.challenge.domain.dto.ctf.CtfChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.ctf.CtfChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.challenge.domain.entity.ctf.CtfChallengeCategory;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeListVO;
import com.humolix.volur.challenge.mapper.ChallengeMapper;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeCategoryRepository;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeRepository;
import com.humolix.volur.challenge.service.admin.ctf.AdminCtfChallengeService;
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
public class AdminCtfChallengeServiceImpl implements AdminCtfChallengeService {

    private final CtfChallengeRepository challengeRepository;
    private final CtfChallengeCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ChallengeMapper challengeMapper;

    @Override
    public PageResponse<CtfChallengeListVO> listChallenges(Integer page, Integer size, Long categoryId, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<CtfChallenge> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("name"), "%" + keyword.trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return PageResponse.of(challengeRepository.findAll(spec, pageable).map(challengeMapper::toCtfListVO));
    }

    @Override
    public CtfChallengeDetailVO getChallenge(Long id) {
        CtfChallenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题目不存在"));
        return challengeMapper.toCtfDetailVO(challenge);
    }

    @Override
    @Transactional
    public Long createChallenge(CtfChallengeCreateRequestDTO request, Long authorId) {
        if (challengeRepository.existsByName(request.getName().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
        }

        CtfChallengeCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "指定的分类不存在"));

        User author = null;
        if (authorId != null) {
            author = userRepository.findById(authorId).orElse(null);
        }

        CtfChallenge challenge = CtfChallenge.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(category)
                .author(author)
                .type(request.getType())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : 1)
                .flag(request.getFlag())
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentName(request.getAttachmentName())
                .imageName(request.getImageName())
                .containerPort(request.getContainerPort())
                .cpuLimit(request.getCpuLimit() != null ? request.getCpuLimit() : 1)
                .memoryLimit(request.getMemoryLimit() != null ? request.getMemoryLimit() : 128)
                .build();

        return challengeRepository.save(challenge).getId();
    }

    @Override
    @Transactional
    public void updateChallenge(Long id, CtfChallengeUpdateRequestDTO request) {
        CtfChallenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题目不存在"));

        if (StringUtils.hasText(request.getName()) && !challenge.getName().equals(request.getName().trim())) {
            if (challengeRepository.existsByName(request.getName().trim())) {
                throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
            }
            challenge.setName(request.getName().trim());
        }

        if (request.getCategoryId() != null) {
            CtfChallengeCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "指定的分类不存在"));
            challenge.setCategory(category);
        }

        if (request.getDescription() != null) challenge.setDescription(request.getDescription());
        if (request.getType() != null) challenge.setType(request.getType());
        if (request.getDifficulty() != null) challenge.setDifficulty(request.getDifficulty());
        if (request.getFlag() != null) challenge.setFlag(request.getFlag());
        if (request.getAttachmentUrl() != null) challenge.setAttachmentUrl(request.getAttachmentUrl());
        if (request.getAttachmentName() != null) challenge.setAttachmentName(request.getAttachmentName());
        if (request.getImageName() != null) challenge.setImageName(request.getImageName());
        if (request.getContainerPort() != null) challenge.setContainerPort(request.getContainerPort());
        if (request.getCpuLimit() != null) challenge.setCpuLimit(request.getCpuLimit());
        if (request.getMemoryLimit() != null) challenge.setMemoryLimit(request.getMemoryLimit());

        challengeRepository.save(challenge);
    }

    @Override
    @Transactional
    public void deleteChallenge(Long id) {
        challengeRepository.deleteById(id);
    }

}
