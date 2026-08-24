package com.humolix.volur.challenge.service.admin.stage.ctf.impl;

import com.humolix.volur.user.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.challenge.entity.stage.ctf.CtfChallenge;
import com.humolix.volur.challenge.entity.stage.ctf.CtfChallengeCategory;
import com.humolix.volur.challenge.object.dto.stage.ctf.CtfChallengeCreateRequestDTO;
import com.humolix.volur.challenge.object.dto.stage.ctf.CtfChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.object.vo.admin.AdminChallengeDetailResponseVO;
import com.humolix.volur.challenge.object.vo.admin.AdminChallengeListResponseVO;
import com.humolix.volur.challenge.repository.stage.ctf.CtfChallengeCategoryRepository;
import com.humolix.volur.challenge.repository.stage.ctf.CtfChallengeRepository;
import com.humolix.volur.challenge.service.admin.stage.ctf.AdminCtfChallengeService;
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
public class AdminCtfChallengeServiceImpl implements AdminCtfChallengeService {

    private final CtfChallengeRepository challengeRepository;
    private final CtfChallengeCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private AdminChallengeDetailResponseVO toDetailVO(CtfChallenge c) {
        if (c == null) return null;
        return AdminChallengeDetailResponseVO.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .categoryId(c.getCategory() != null ? c.getCategory().getId() : null)
                .categoryName(c.getCategory() != null ? c.getCategory().getName() : null)
                .authorId(c.getAuthor() != null ? c.getAuthor().getId() : null)
                .authorName(c.getAuthor() != null ? c.getAuthor().getUsername() : null)
                .difficulty(c.getDifficulty())
                .staticFlag(c.getFlag())
                .dynamicContainer(c.isDynamicContainer())
                .attachments(c.getAttachments())
                .imageName(c.getImageName())
                .containerPort(c.getContainerPort())
                .deleted(c.isDeleted())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    @Override
    public Page<AdminChallengeListResponseVO> pageChallenges(Integer page, Integer size, Long categoryId, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);
        Specification<CtfChallenge> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("name"), "%" + keyword + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return challengeRepository.findAll(spec, pageable).map(c -> AdminChallengeListResponseVO.builder()
                .id(c.getId())
                .name(c.getName())
                .categoryName(c.getCategory() != null ? c.getCategory().getName() : null)
                .authorName(c.getAuthor() != null ? c.getAuthor().getUsername() : null)
                .difficulty(c.getDifficulty())
                .dynamicContainer(c.isDynamicContainer())
                .deleted(c.isDeleted())
                .createdAt(c.getCreatedAt())
                .build());
    }

    @Override
    public AdminChallengeDetailResponseVO getChallengeDetail(Long id) {
        CtfChallenge c = challengeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题目不存在"));
        return toDetailVO(c);
    }

    @Override
    @Transactional
    public AdminChallengeDetailResponseVO createChallenge(CtfChallengeCreateRequestDTO request, Long authorId) {
        CtfChallengeCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题目分类不存在"));

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "作者不存在"));

        CtfChallenge challenge = CtfChallenge.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(category)
                .author(author)
                .difficulty(request.getDifficulty())
                .flag(request.getStaticFlag())
                .dynamicContainer(request.isDynamicContainer())
                .attachments(request.getAttachments())
                .imageName(request.getImageName())
                .containerPort(request.getContainerPort())
                .deleted(false)
                .build();

        return toDetailVO(challengeRepository.save(challenge));
    }

    @Override
    @Transactional
    public AdminChallengeDetailResponseVO updateChallenge(Long id, CtfChallengeUpdateRequestDTO request) {
        CtfChallenge c = challengeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题目不存在"));

        if (StringUtils.hasText(request.getName())) c.setName(request.getName());
        if (StringUtils.hasText(request.getDescription())) c.setDescription(request.getDescription());
        if (request.getCategoryId() != null) {
            CtfChallengeCategory cat = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题目分类不存在"));
            c.setCategory(cat);
        }
        if (request.getDifficulty() != null) c.setDifficulty(request.getDifficulty());
        if (request.getStaticFlag() != null) c.setFlag(request.getStaticFlag());
        if (request.getDynamicContainer() != null) c.setDynamicContainer(request.getDynamicContainer());
        if (request.getAttachments() != null) c.setAttachments(request.getAttachments());
        if (request.getImageName() != null) c.setImageName(request.getImageName());
        if (request.getContainerPort() != null) c.setContainerPort(request.getContainerPort());

        return toDetailVO(challengeRepository.save(c));
    }

    @Override
    @Transactional
    public void deleteChallenge(Long id) {
        CtfChallenge c = challengeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题目不存在"));
        c.setDeleted(true);
        c.setDeletedAt(Instant.now());
        challengeRepository.save(c);
    }

}
