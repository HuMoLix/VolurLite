package com.humolix.volur.challenge.service.admin.awd.impl;

import com.humolix.volur.challenge.domain.dto.awd.AwdChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.awd.AwdChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.entity.awd.AwdChallenge;
import com.humolix.volur.challenge.domain.enums.AwdCategory;
import com.humolix.volur.challenge.domain.vo.awd.AwdChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.awd.AwdChallengeListVO;
import com.humolix.volur.challenge.mapper.ChallengeMapper;
import com.humolix.volur.challenge.repository.awd.AwdChallengeRepository;
import com.humolix.volur.challenge.service.admin.awd.AdminAwdChallengeService;
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
public class AdminAwdChallengeServiceImpl implements AdminAwdChallengeService {

    private final AwdChallengeRepository awdRepository;
    private final UserRepository userRepository;
    private final ChallengeMapper challengeMapper;

    @Override
    public PageResponse<AwdChallengeListVO> listChallenges(Integer page, Integer size, AwdCategory category, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<AwdChallenge> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("name"), "%" + keyword.trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return PageResponse.of(awdRepository.findAll(spec, pageable).map(challengeMapper::toAwdListVO));
    }

    @Override
    public AwdChallengeDetailVO getChallenge(Long id) {
        AwdChallenge challenge = awdRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "AWD 题目不存在"));
        return challengeMapper.toAwdDetailVO(challenge);
    }

    @Override
    @Transactional
    public Long createChallenge(AwdChallengeCreateRequestDTO request, Long authorId) {
        if (awdRepository.existsByName(request.getName().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
        }

        User author = null;
        if (authorId != null) {
            author = userRepository.findById(authorId).orElse(null);
        }

        AwdChallenge challenge = AwdChallenge.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .author(author)
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : 1)
                .attachmentUrl(request.getAttachmentUrl())
                .imageName(request.getImageName().trim())
                .containerPort(request.getContainerPort())
                .checkScriptUrl(request.getCheckScriptUrl())
                .flagPath(StringUtils.hasText(request.getFlagPath()) ? request.getFlagPath() : "/flag")
                .build();

        return awdRepository.save(challenge).getId();
    }

    @Override
    @Transactional
    public void updateChallenge(Long id, AwdChallengeUpdateRequestDTO request) {
        AwdChallenge challenge = awdRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "AWD 题目不存在"));

        if (request.getName() != null && !request.getName().isEmpty() && !challenge.getName().equals(request.getName())) {
            if (awdRepository.existsByNameAndIdNot(request.getName(), id)) {
                throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
            }
            challenge.setName(request.getName());
        }

        if (request.getDescription() != null) challenge.setDescription(request.getDescription());
        if (request.getCategory() != null) challenge.setCategory(request.getCategory());
        if (request.getDifficulty() != null) challenge.setDifficulty(request.getDifficulty());
        if (request.getAttachmentUrl() != null) challenge.setAttachmentUrl(request.getAttachmentUrl());
        if (request.getImageName() != null) challenge.setImageName(request.getImageName().trim());
        if (request.getContainerPort() != null) challenge.setContainerPort(request.getContainerPort());
        if (request.getCheckScriptUrl() != null) challenge.setCheckScriptUrl(request.getCheckScriptUrl());
        if (request.getFlagPath() != null) challenge.setFlagPath(request.getFlagPath());

        awdRepository.save(challenge);
    }

    @Override
    @Transactional
    public void deleteChallenge(Long id) {
        awdRepository.deleteById(id);
    }

}
