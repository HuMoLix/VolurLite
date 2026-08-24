package com.humolix.volur.user.service.admin.impl;

import cn.hutool.crypto.SmUtil;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.user.domain.dto.AdminUserCreateRequestDTO;
import com.humolix.volur.user.domain.dto.AdminUserUpdateRequestDTO;
import com.humolix.volur.user.entity.Role;
import com.humolix.volur.user.entity.User;
import com.humolix.volur.user.entity.UserProfile;
import com.humolix.volur.user.event.UserStatusAction;
import com.humolix.volur.user.event.UserStatusChangedEvent;
import com.humolix.volur.user.repository.RoleRepository;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.service.admin.AdminUserService;
import com.humolix.volur.user.domain.vo.AdminUserDetailsVO;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    private AdminUserDetailsVO toAdminDetailsResponse(User user) {
        if (user == null) return null;
        UserProfile profile = user.getUserProfile();
        return AdminUserDetailsVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .realName(user.getRealName())
                .gender(user.getGender())
                .birthday(user.getBirthday())
                .role(user.getRole() != null ? user.getRole().getName() : "USER")
                .org(profile != null ? profile.getOrg() : null)
                .major(profile != null ? profile.getMajor() : null)
                .bio(profile != null ? profile.getBio() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .status(user.getStatus())
                .isHidden(user.getIsHidden())
                .lastLoginIp(user.getLastLoginIp())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    public Page<AdminUserDetailsVO> getUserList(Integer page, Integer size, String keyword) {
        Pageable pageable = SortUtils.createPageable(page, size);
        Specification<User> spec = (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return null;
            String pattern = "%" + keyword + "%";
            Join<User, UserProfile> profileJoin = root.join("userProfile", JoinType.LEFT);
            return cb.or(
                    cb.like(root.get("username"), pattern),
                    cb.like(root.get("email"), pattern),
                    cb.like(profileJoin.get("org"), pattern)
            );
        };
        return userRepository.findAll(spec, pageable).map(this::toAdminDetailsResponse);
    }

    @Override
    public AdminUserDetailsVO getUserDetails(Long id) {
        User user = userRepository.findByIdWithProfileAndRole(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        return toAdminDetailsResponse(user);
    }

    @Override
    @Transactional
    public Long createUser(AdminUserCreateRequestDTO request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已存在");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(HttpStatus.CONFLICT, "邮箱已存在");
        }

        Role role = roleRepository.findByName(StringUtils.hasText(request.getRoleName()) ? request.getRoleName() : "USER")
                .orElseGet(() -> roleRepository.findByName("USER").orElse(null));

        String encodedPassword = SmUtil.sm3(request.getPassword());

        User user = User.builder()
                .username(request.getUsername())
                .password(encodedPassword)
                .email(request.getEmail())
                .role(role)
                .status(true)
                .isHidden(false)
                .totalPoints(0.001 - (System.currentTimeMillis() * 0.0000000000000001))
                .solves(0L)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        UserProfile profile = UserProfile.builder()
                .user(savedUser)
                .avatarUrl("/api/v1/uploads/avatar/default_avatar.jpg")
                .isHidden(false)
                .build();
        userProfileRepository.save(profile);

        return savedUser.getId();
    }

    @Override
    @Transactional
    public void updateUser(Long id, AdminUserUpdateRequestDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (StringUtils.hasText(request.getUsername()) && !user.getUsername().equals(request.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BusinessException(HttpStatus.CONFLICT, "用户名已存在");
            }
            user.setUsername(request.getUsername());
        }

        if (StringUtils.hasText(request.getEmail()) && !user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException(HttpStatus.CONFLICT, "邮箱已被注册");
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(SmUtil.sm3(request.getPassword()));
        }

        if (StringUtils.hasText(request.getRoleName())) {
            Role role = roleRepository.findByName(request.getRoleName())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "指定的角色不存在"));
            user.setRole(role);
        }

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        UserProfile profile = user.getUserProfile();
        if (profile != null) {
            if (request.getOrg() != null) profile.setOrg(request.getOrg());
            if (request.getBio() != null) profile.setBio(request.getBio());
            userProfileRepository.save(profile);
        }

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        user.setStatus(false);
        long timestamp = System.currentTimeMillis();
        user.setUsername(user.getUsername() + "_deleted_" + timestamp);
        user.setEmail(user.getEmail() + "_deleted_" + timestamp);
        if (user.getPhone() != null) {
            user.setPhone(null);
        }
        userRepository.save(user);
        eventPublisher.publishEvent(new UserStatusChangedEvent(this, id, UserStatusAction.SOFT_DELETE));
    }

    @Override
    @Transactional
    public void deleteUsers(List<Long> ids) {
        if (ids != null) {
            for (Long id : ids) {
                deleteUser(id);
            }
        }
    }

    @Override
    @Transactional
    public void toggleHiddenStatus(Long id, Boolean isHidden) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        user.setIsHidden(isHidden);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long id, Boolean status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void toggleUsersStatus(List<Long> ids, Boolean status) {
        if (ids != null) {
            List<User> users = userRepository.findAllById(ids);
            for (User user : users) {
                user.setStatus(status);
            }
            userRepository.saveAll(users);
        }
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

}
