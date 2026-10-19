package com.humolix.volur.user.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.domain.vo.StorageResponseVO;
import com.humolix.volur.system.service.StorageService;
import com.humolix.volur.user.domain.dto.UpdateUserProfileRequestDTO;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.entity.UserProfile;
import com.humolix.volur.user.domain.vo.UserProfileResponseVO;
import com.humolix.volur.user.domain.vo.UserSummaryVO;
import com.humolix.volur.user.mapper.UserMapper;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;
    private final StorageService storageService;

    @Override
    public UserSummaryVO getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        return userMapper.toUserSummaryVO(user, profile);
    }

    @Override
    public UserProfileResponseVO getUserProfileById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        return userMapper.toUserProfileResponseVO(user, profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileResponseVO updateUserProfile(Long userId, UpdateUserProfileRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> UserProfile.builder()
                        .id(userId)
                        .isHidden(false)
                        .build());

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBirthday() != null) {
            user.setBirthday(request.getBirthday());
        }
        if (request.getOrg() != null) {
            profile.setOrg(request.getOrg());
        }
        if (request.getMajor() != null) {
            profile.setMajor(request.getMajor());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getEmailPrivacy() != null) {
            profile.setEmailPrivacy(request.getEmailPrivacy());
        }
        if (request.getPhonePrivacy() != null) {
            profile.setPhonePrivacy(request.getPhonePrivacy());
        }
        if (request.getOrgPrivacy() != null) {
            profile.setOrgPrivacy(request.getOrgPrivacy());
        }

        userRepository.save(user);
        userProfileRepository.save(profile);

        return userMapper.toUserProfileResponseVO(user, profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateAvatar(Long userId, String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "头像地址不能为空");
        }

        if (!userRepository.existsById(userId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> UserProfile.builder()
                        .id(userId)
                        .isHidden(false)
                        .build());

        profile.setAvatarUrl(avatarUrl.trim());
        userProfileRepository.save(profile);

        return profile.getAvatarUrl();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAndSetAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "头像文件不能为空");
        }

        if (!userRepository.existsById(userId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        String oldAvatarUrl = profile != null ? profile.getAvatarUrl() : null;

        // 校验新上传文件与旧头像文件的 MD5 摘要值，若完全相同则取消冗余上传
        if (StringUtils.hasText(oldAvatarUrl)) {
            String newMd5 = storageService.calculateMd5(file);
            String oldMd5 = storageService.getFileMd5(oldAvatarUrl);
            if (StringUtils.hasText(oldMd5) && oldMd5.equalsIgnoreCase(newMd5)) {
                log.info("[UserAvatar] User {} uploaded identical avatar (MD5: {}), cancelled upload", userId, newMd5);
                return oldAvatarUrl;
            }
        }

        StorageResponseVO storageResponse = storageService.uploadImage(file, "avatars");
        String newAvatarUrl = updateAvatar(userId, storageResponse.getFilePath());

        // 头像更新后，清理旧头像物理文件以释放空间（避免误删默认头像等系统预置资源）
        if (StringUtils.hasText(oldAvatarUrl) && !oldAvatarUrl.equals(newAvatarUrl) && !oldAvatarUrl.contains("default_avatar")) {
            storageService.deleteFile(oldAvatarUrl);
        }

        return newAvatarUrl;
    }

}
