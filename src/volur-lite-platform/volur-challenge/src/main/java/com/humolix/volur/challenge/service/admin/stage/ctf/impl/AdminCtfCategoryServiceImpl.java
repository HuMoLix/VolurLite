package com.humolix.volur.challenge.service.admin.stage.ctf.impl;

import com.humolix.volur.challenge.entity.stage.ctf.CtfChallengeCategory;
import com.humolix.volur.challenge.object.dto.stage.ctf.CtfChallengeCategoryCreateRequestDTO;
import com.humolix.volur.challenge.object.dto.stage.ctf.CtfChallengeCategoryUpdateRequestDTO;
import com.humolix.volur.challenge.object.vo.stage.ctf.CtfChallengeCategoryResponseVO;
import com.humolix.volur.challenge.repository.stage.ctf.CtfChallengeCategoryRepository;
import com.humolix.volur.challenge.repository.stage.ctf.CtfChallengeRepository;
import com.humolix.volur.challenge.service.admin.stage.ctf.AdminCtfCategoryService;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCtfCategoryServiceImpl implements AdminCtfCategoryService {

    private final CtfChallengeCategoryRepository categoryRepository;
    private final CtfChallengeRepository challengeRepository;

    private CtfChallengeCategoryResponseVO toResponseVO(CtfChallengeCategory cat) {
        if (cat == null) return null;
        return CtfChallengeCategoryResponseVO.builder()
                .id(cat.getId())
                .name(cat.getName())
                .description(cat.getDescription())
                .iconUrl(cat.getIconUrl())
                .build();
    }

    @Override
    public List<CtfChallengeCategoryResponseVO> listCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toResponseVO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<CtfChallengeCategoryResponseVO> pageCategories(Integer page, Integer size, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);
        Specification<CtfChallengeCategory> spec = (root, query, cb) -> {
            if (StringUtils.hasText(keyword)) {
                return cb.like(root.get("name"), "%" + keyword + "%");
            }
            return cb.conjunction();
        };
        return categoryRepository.findAll(spec, pageable).map(this::toResponseVO);
    }

    @Override
    @Transactional
    public CtfChallengeCategoryResponseVO createCategory(CtfChallengeCategoryCreateRequestDTO request) {
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "分类名称已存在");
        }

        CtfChallengeCategory cat = CtfChallengeCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .build();

        CtfChallengeCategory saved = categoryRepository.save(cat);
        return toResponseVO(saved);
    }

    @Override
    @Transactional
    public CtfChallengeCategoryResponseVO updateCategory(Long id, CtfChallengeCategoryUpdateRequestDTO request) {
        CtfChallengeCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "分类不存在"));

        if (StringUtils.hasText(request.getName())) {
            cat.setName(request.getName());
        }
        if (request.getDescription() != null) {
            cat.setDescription(request.getDescription());
        }
        if (request.getIconUrl() != null) {
            cat.setIconUrl(request.getIconUrl());
        }

        return toResponseVO(categoryRepository.save(cat));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "分类不存在");
        }
        Long challengeCount = challengeRepository.countByCategoryId(id);
        if (challengeCount != null && challengeCount > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该分类下存在题目，无法直接删除");
        }
        categoryRepository.deleteById(id);
    }

}
