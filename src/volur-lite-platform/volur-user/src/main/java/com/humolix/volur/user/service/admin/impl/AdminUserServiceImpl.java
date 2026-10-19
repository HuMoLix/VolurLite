package com.humolix.volur.user.service.admin.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.domain.dto.AdminUserCreateRequestDTO;
import com.humolix.volur.user.domain.dto.AdminUserPageRequestDTO;
import com.humolix.volur.user.domain.dto.AdminUserUpdateRequestDTO;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.entity.UserProfile;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.domain.vo.AdminUserDetailsVO;
import com.humolix.volur.user.domain.vo.AdminUserStatisticsVO;
import com.humolix.volur.user.domain.vo.RoleVO;
import com.humolix.volur.user.event.UserStatusAction;
import com.humolix.volur.user.event.UserStatusChangedEvent;
import com.humolix.volur.user.mapper.UserMapper;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.service.admin.AdminUserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserServiceImpl implements AdminUserService {

    public static final String SUPER_ADMIN_USERNAME = "VolurSuperAdmin";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<AdminUserDetailsVO> getUserList(AdminUserPageRequestDTO pageRequest) {
        AdminUserPageRequestDTO request = pageRequest != null ? pageRequest : new AdminUserPageRequestDTO();
        Pageable pageable = request.toPageable(User.class, "id");

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(request.getKeyword())) {
                String cleanKeyword = request.getKeyword();
                if (cleanKeyword.length() > 50) {
                    cleanKeyword = cleanKeyword.substring(0, 50);
                }
                String pattern = "%" + cleanKeyword.toLowerCase() + "%";
                Predicate usernameLike = cb.like(cb.lower(root.get("username")), pattern);
                Predicate emailLike = cb.like(cb.lower(root.get("email")), pattern);

                List<Long> orgUserIds = userProfileRepository.findUserIdsByOrgLike(cleanKeyword);
                if (!orgUserIds.isEmpty()) {
                    predicates.add(cb.or(usernameLike, emailLike, root.get("id").in(orgUserIds)));
                } else {
                    predicates.add(cb.or(usernameLike, emailLike));
                }
            }

            if (StringUtils.hasText(request.getRole())) {
                try {
                    String roleStr = request.getRole().trim().toUpperCase();
                    if (roleStr.startsWith("ROLE_")) {
                        roleStr = roleStr.substring(5);
                    }
                    UserRole userRole = UserRole.valueOf(roleStr);
                    predicates.add(cb.equal(root.get("role"), userRole));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (request.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getStartTime()));
            }

            if (request.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getEndTime()));
            }

            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> entityPage = userRepository.findAll(spec, pageable);
        List<Long> userIds = entityPage.getContent().stream().map(User::getId).toList();
        java.util.Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(UserProfile::getId, p -> p));
        return entityPage.map(u -> userMapper.toAdminUserDetailsVO(u, profileMap.get(u.getId())));
    }

    @Override
    public AdminUserStatisticsVO getUserStatistics() {
        long totalUsers = userRepository.count();
        long adminCount = userRepository.countByRoleIn(List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN));
        long bannedCount = userRepository.countByStatus(false);
        long activeCount = userRepository.countByStatus(true);

        return AdminUserStatisticsVO.builder()
                .totalUsers(totalUsers)
                .adminCount(adminCount)
                .bannedCount(bannedCount)
                .activeCount(activeCount)
                .build();
    }

    @Override
    public AdminUserDetailsVO getUserDetails(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        UserProfile profile = userProfileRepository.findById(id).orElse(null);
        return userMapper.toAdminUserDetailsVO(user, profile);
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

        UserRole role = UserRole.USER;
        if (request.getRoleName() != null && !request.getRoleName().isEmpty()) {
            try {
                String roleStr = request.getRoleName().toUpperCase();
                if (roleStr.startsWith("ROLE_")) {
                    roleStr = roleStr.substring(5);
                }
                role = UserRole.valueOf(roleStr);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "指定的角色不存在: " + request.getRoleName());
            }
            if (role == UserRole.SUPER_ADMIN) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "超级管理员(SUPER_ADMIN)仅限系统唯一账号" + SUPER_ADMIN_USERNAME + "，不可手动创建");
            }
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .username(request.getUsername())
                .password(encodedPassword)
                .email(request.getEmail())
                .role(role)
                .status(true)
                .isHidden(false)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        UserProfile profile = UserProfile.builder()
                .id(savedUser.getId())
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

        boolean isSuperAdmin = SUPER_ADMIN_USERNAME.equals(user.getUsername());

        if (request.getUsername() != null && !request.getUsername().isEmpty() && !request.getUsername().equals(user.getUsername())) {
            if (isSuperAdmin) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "超级管理员用户名不可变更");
            }
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BusinessException(HttpStatus.CONFLICT, "用户名已存在");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().isEmpty() && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException(HttpStatus.CONFLICT, "邮箱已被注册");
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoleName() != null && !request.getRoleName().isEmpty()) {
            if (isSuperAdmin) {
                if (!UserRole.SUPER_ADMIN.name().equalsIgnoreCase(request.getRoleName())) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "超级管理员角色不可变更");
                }
            } else {
                try {
                    String roleStr = request.getRoleName().toUpperCase();
                    if (roleStr.startsWith("ROLE_")) {
                        roleStr = roleStr.substring(5);
                    }
                    UserRole targetRole = UserRole.valueOf(roleStr);
                    if (targetRole == UserRole.SUPER_ADMIN) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, "超级管理员在系统中只能存在一个(" + SUPER_ADMIN_USERNAME + ")，禁止分配超级管理员角色");
                    }
                    user.setRole(targetRole);
                } catch (IllegalArgumentException e) {
                    throw new BusinessException(HttpStatus.NOT_FOUND, "指定的角色不存在");
                }
            }
        }

        if (request.getStatus() != null) {
            if (isSuperAdmin && Boolean.FALSE.equals(request.getStatus())) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "超级管理员账号不可禁用");
            }
            user.setStatus(request.getStatus());
        }

        if (request.getOrg() != null || request.getBio() != null) {
            UserProfile profile = userProfileRepository.findById(id)
                    .orElseGet(() -> UserProfile.builder().id(id).isHidden(false).build());
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

        if (SUPER_ADMIN_USERNAME.equals(user.getUsername())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "超级管理员账号禁止删除");
        }

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
    public void toggleHiddenStatus(Long id, boolean isHidden) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        user.setIsHidden(isHidden);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long id, boolean status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (SUPER_ADMIN_USERNAME.equals(user.getUsername()) && !status) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "超级管理员账号不可禁用");
        }
        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void toggleUsersStatus(List<Long> ids, boolean status) {
        if (ids != null) {
            List<User> users = userRepository.findAllById(ids);
            for (User user : users) {
                if (SUPER_ADMIN_USERNAME.equals(user.getUsername()) && !status) {
                    continue; // 跳过超级管理员，防止被意外批量禁用
                }
                user.setStatus(status);
            }
            userRepository.saveAll(users);
        }
    }

    @Override
    public List<RoleVO> getAllRoles() {
        return Arrays.stream(UserRole.values())
                .map(r -> RoleVO.builder()
                        .name(r.name())
                        .description(r.getDescription())
                        .build())
                .toList();
    }

}
