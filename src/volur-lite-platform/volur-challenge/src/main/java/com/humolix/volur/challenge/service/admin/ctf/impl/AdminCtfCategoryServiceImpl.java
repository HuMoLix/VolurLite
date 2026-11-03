package com.humolix.volur.challenge.service.admin.ctf.impl;

import com.humolix.volur.challenge.domain.dto.ctf.CtfCategoryCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.ctf.CtfCategoryUpdateRequestDTO;
import com.humolix.volur.challenge.domain.entity.ctf.CtfChallengeCategory;
import com.humolix.volur.challenge.domain.vo.ctf.CtfCategoryVO;
import com.humolix.volur.challenge.mapper.ChallengeMapper;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeCategoryRepository;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeRepository;
import com.humolix.volur.challenge.service.admin.ctf.AdminCtfCategoryService;
import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
    @Cacheable(cacheNames = CacheNames.DICT, key = "'ctf:category:' + #id", sync = true)
    public CtfCategoryVO getCategory(Long id) {
        CtfChallengeCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "分类不存在"));
        return challengeMapper.toCtfCategoryVO(category);
    }

    @Override
    @Transactional
    public Long createCategory(CtfCategoryCreateRequestDTO request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException(HttpStatus.CONFLICT, "分类名称已存在");
        }

        CtfChallengeCategory category = CtfChallengeCategory.builder()
                .name(request.getName())
                .icon(request.getIcon() != null && request.getIcon().isEmpty() ? null : request.getIcon())
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        return categoryRepository.save(category).getId();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.DICT, key = "'ctf:category:' + #id")
    public void updateCategory(Long id, CtfCategoryUpdateRequestDTO request) {
        CtfChallengeCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "分类不存在"));

        if (request.getName() != null && !request.getName().isEmpty() && !category.getName().equals(request.getName())) {
            if (categoryRepository.existsByNameAndIdNot(request.getName(), id)) {
                throw new BusinessException(HttpStatus.CONFLICT, "分类名称已存在");
            }
            category.setName(request.getName());
        }

        if (request.getIcon() != null) {
            category.setIcon(request.getIcon().isEmpty() ? null : request.getIcon());
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
    @CacheEvict(cacheNames = CacheNames.DICT, key = "'ctf:category:' + #id")
    public void deleteCategory(Long id) {
        if (challengeRepository.existsByCategoryId(id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "该分类下存在题目，无法删除");
        }
        categoryRepository.deleteById(id);
    }

}
