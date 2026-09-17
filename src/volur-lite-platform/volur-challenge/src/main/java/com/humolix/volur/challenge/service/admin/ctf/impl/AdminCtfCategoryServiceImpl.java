package com.humolix.volur.challenge.service.admin.ctf.impl;

import com.humolix.volur.challenge.domain.dto.ctf.CtfCategoryCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.ctf.CtfCategoryUpdateRequestDTO;
import com.humolix.volur.challenge.domain.entity.ctf.CtfChallengeCategory;
import com.humolix.volur.challenge.domain.vo.ctf.CtfCategoryVO;
import com.humolix.volur.challenge.mapper.ChallengeMapper;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeCategoryRepository;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeRepository;
import com.humolix.volur.challenge.service.admin.ctf.AdminCtfCategoryService;
import com.humolix.volur.framework.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCtfCategoryServiceImpl implements AdminCtfCategoryService {

    private final CtfChallengeCategoryRepository categoryRepository;
    private final CtfChallengeRepository challengeRepository;
    private final ChallengeMapper challengeMapper;

    @Override
    public List<CtfCategoryVO> listCategories() {
        return challengeMapper.toCtfCategoryVOList(
                categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder", "id"))
        );
    }

    @Override
    public CtfCategoryVO getCategory(Long id) {
        CtfChallengeCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "分类不存在"));
        return challengeMapper.toCtfCategoryVO(category);
    }

    @Override
    @Transactional
    public Long createCategory(CtfCategoryCreateRequestDTO request) {
        if (categoryRepository.existsByName(request.getName().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "分类名称已存在");
        }

        CtfChallengeCategory category = CtfChallengeCategory.builder()
                .name(request.getName().trim())
                .icon(StringUtils.hasText(request.getIcon()) ? request.getIcon().trim() : null)
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        return categoryRepository.save(category).getId();
    }

    @Override
    @Transactional
    public void updateCategory(Long id, CtfCategoryUpdateRequestDTO request) {
        CtfChallengeCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "分类不存在"));

        if (StringUtils.hasText(request.getName()) && !category.getName().equals(request.getName().trim())) {
            if (categoryRepository.existsByName(request.getName().trim())) {
                throw new BusinessException(HttpStatus.CONFLICT, "分类名称已存在");
            }
            category.setName(request.getName().trim());
        }

        if (request.getIcon() != null) {
            category.setIcon(StringUtils.hasText(request.getIcon()) ? request.getIcon().trim() : null);
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (challengeRepository.existsByCategoryId(id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "该分类下存在题目，无法删除");
        }
        categoryRepository.deleteById(id);
    }

}
