package com.humolix.volur.user.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.entity.User;
import com.humolix.volur.user.entity.UserProfile;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.service.UserService;
import com.humolix.volur.user.domain.vo.UserProfileResponseVO;
import com.humolix.volur.user.domain.vo.UserSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    public UserSummaryVO getCurrentUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        User user = userRepository.findByIdWithProfileAndRole(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        UserProfile profile = user.getUserProfile();
        return UserSummaryVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .role(user.getRole() != null ? user.getRole().getName() : "USER")
                .org(profile != null ? profile.getOrg() : null)
                .bio(profile != null ? profile.getBio() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public UserProfileResponseVO getUserProfileById(Long userId) {
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        User user = userRepository.findByIdWithProfileAndRole(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        UserProfile profile = user.getUserProfile();
        Integer rank = userRepository.findRankByUserId(userId);

        return UserProfileResponseVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .birthday(user.getBirthday())
                .role(user.getRole() != null ? user.getRole().getName() : "USER")
                .org(profile != null ? profile.getOrg() : null)
                .major(profile != null ? profile.getMajor() : null)
                .bio(profile != null ? profile.getBio() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .bannerUrl(profile != null ? profile.getBannerUrl() : null)
                .notice(profile != null ? profile.getNotice() : null)
                .emailPrivacy(profile != null ? profile.getEmailPrivacy() : null)
                .phonePrivacy(profile != null ? profile.getPhonePrivacy() : null)
                .orgPrivacy(profile != null ? profile.getOrgPrivacy() : null)
                .totalPoints(user.getTotalPoints() != null ? user.getTotalPoints() : 0.0)
                .solves(user.getSolves() != null ? user.getSolves() : 0L)
                .ranking(rank)
                .createdAt(user.getCreatedAt())
                .build();
    }

}
