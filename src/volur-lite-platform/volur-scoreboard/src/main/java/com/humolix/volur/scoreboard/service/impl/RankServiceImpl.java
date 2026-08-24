package com.humolix.volur.scoreboard.service.impl;

import com.humolix.volur.user.entity.User;
import com.humolix.volur.user.entity.UserProfile;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.scoreboard.service.RankService;
import com.humolix.volur.scoreboard.vo.RankResponseVO;
import com.humolix.volur.scoreboard.vo.UserRankResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {

    private final UserRepository userRepository;

    private UserRankResponseVO toUserRankVO(User user) {
        if (user == null) return null;
        UserProfile profile = user.getUserProfile();
        return UserRankResponseVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .org(profile != null ? profile.getOrg() : null)
                .totalPoints(user.getTotalPoints() != null ? user.getTotalPoints() : 0.0)
                .solves(user.getSolves() != null ? user.getSolves() : 0L)
                .build();
    }

    @Override
    public Page<RankResponseVO<UserRankResponseVO>> pageUserRanking(String keyword, Integer page, Integer size) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<User> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.isFalse(root.get("isHidden")));
            predicates.add(cb.isTrue(root.get("status")));

            if (query != null) {
                query.orderBy(cb.desc(root.get("totalPoints")), cb.asc(root.get("id")));
            }

            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("username"), "%" + keyword + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<User> usersPage = userRepository.findAll(spec, pageable);
        int startRank = pageable.getPageNumber() * pageable.getPageSize() + 1;

        List<RankResponseVO<UserRankResponseVO>> rankList = new ArrayList<>();
        int currentRank = startRank;
        for (User user : usersPage.getContent()) {
            rankList.add(RankResponseVO.of(currentRank++, toUserRankVO(user)));
        }

        return new PageImpl<>(rankList, pageable, usersPage.getTotalElements());
    }

    @Override
    public RankResponseVO<UserRankResponseVO> getMyUserRank(Long userId) {
        User user = userRepository.findByIdWithProfileAndRole(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        Integer rank = userRepository.findRankByUserId(userId);
        return RankResponseVO.of(rank != null ? rank : 0, toUserRankVO(user));
    }

}
