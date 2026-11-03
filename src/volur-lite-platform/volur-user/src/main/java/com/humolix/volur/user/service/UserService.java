package com.humolix.volur.user.service;

import com.humolix.volur.user.domain.dto.UpdateUserProfileRequestDTO;
import com.humolix.volur.user.domain.vo.UserProfileResponseVO;
import com.humolix.volur.user.domain.vo.UserSummaryVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserSummaryVO getCurrentUser(Long userId);

    UserProfileResponseVO getUserProfileById(Long userId);

    UserProfileResponseVO updateUserProfile(Long userId, UpdateUserProfileRequestDTO request);

    String updateAvatar(Long userId, String avatarUrl);

    String uploadAndSetAvatar(Long userId, MultipartFile file);

}
