package com.humolix.volur.challenge.service.admin.isw.impl;

import com.humolix.volur.challenge.domain.dto.isw.IswChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.isw.IswChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.entity.isw.IswChallenge;
import com.humolix.volur.challenge.domain.vo.isw.IswChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.isw.IswChallengeListVO;
import com.humolix.volur.challenge.mapper.ChallengeMapper;
import com.humolix.volur.challenge.repository.isw.IswChallengeRepository;
import com.humolix.volur.challenge.service.admin.isw.AdminIswChallengeService;
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
public class AdminIswChallengeServiceImpl implements AdminIswChallengeService {

    private final IswChallengeRepository iswRepository;
    private final UserRepository userRepository;
    private final ChallengeMapper challengeMapper;

    @Override
    public PageResponse<IswChallengeListVO> listChallenges(Integer page, Integer size, String category, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<IswChallenge> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category.trim()));
            }
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("name"), "%" + keyword.trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return PageResponse.of(iswRepository.findAll(spec, pageable).map(challengeMapper::toIswListVO));
    }

    @Override
    public IswChallengeDetailVO getChallenge(Long id) {
        IswChallenge challenge = iswRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ISW 题目不存在"));
        return challengeMapper.toIswDetailVO(challenge);
    }

    @Override
    @Transactional
    public Long createChallenge(IswChallengeCreateRequestDTO request, Long authorId) {
        if (iswRepository.existsByName(request.getName().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
        }

        User author = null;
        if (authorId != null) {
            author = userRepository.findById(authorId).orElse(null);
        }

        IswChallenge challenge = IswChallenge.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(StringUtils.hasText(request.getCategory()) ? request.getCategory().trim() : "PENETRATION")
                .author(author)
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : 1)
                .topologyJson(request.getTopologyJson())
                .stageFlagsJson(request.getStageFlagsJson())
                .build();

        return iswRepository.save(challenge).getId();
    }

    @Override
    @Transactional
    public void updateChallenge(Long id, IswChallengeUpdateRequestDTO request) {
        IswChallenge challenge = iswRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ISW 题目不存在"));

        if (request.getName() != null && !request.getName().isEmpty() && !challenge.getName().equals(request.getName())) {
            if (iswRepository.existsByNameAndIdNot(request.getName(), id)) {
                throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
            }
            challenge.setName(request.getName());
        }

        if (request.getDescription() != null) challenge.setDescription(request.getDescription());
        if (request.getCategory() != null) challenge.setCategory(request.getCategory());
        if (request.getDifficulty() != null) challenge.setDifficulty(request.getDifficulty());
        if (request.getTopologyJson() != null) challenge.setTopologyJson(request.getTopologyJson());
        if (request.getStageFlagsJson() != null) challenge.setStageFlagsJson(request.getStageFlagsJson());

        iswRepository.save(challenge);
    }

    @Override
    @Transactional
    public void deleteChallenge(Long id) {
        iswRepository.deleteById(id);
    }

}
