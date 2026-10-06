package com.humolix.volur.auth.init;

import cn.hutool.core.util.RandomUtil;
import com.humolix.volur.security.crypto.Sm3PasswordEncoder;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.entity.UserProfile;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class SuperAdminInitializer implements ApplicationRunner {

    public static final String SUPER_ADMIN_USERNAME = "VolurSuperAdmin";
    private static final String SUPER_ADMIN_DEFAULT_EMAIL = "superadmin@volur.local";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final Sm3PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        String rawPassword = generateSecurePassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);

        Optional<User> superAdminOpt = userRepository.findByUsername(SUPER_ADMIN_USERNAME);
        if (superAdminOpt.isPresent()) {
            User superAdmin = superAdminOpt.get();
            superAdmin.setPassword(encodedPassword);
            superAdmin.setRole(UserRole.SUPER_ADMIN);
            superAdmin.setStatus(true);
            superAdmin.setIsHidden(false);
            userRepository.save(superAdmin);
        } else {
            User newSuperAdmin = User.builder()
                    .username(SUPER_ADMIN_USERNAME)
                    .password(encodedPassword)
                    .email(SUPER_ADMIN_DEFAULT_EMAIL)
                    .role(UserRole.SUPER_ADMIN)
                    .status(true)
                    .isHidden(false)
                    .build();
            User savedUser = userRepository.saveAndFlush(newSuperAdmin);

            UserProfile profile = UserProfile.builder()
                    .id(savedUser.getId())
                    .org("Volur Lite")
                    .bio("Volur Lite System Super Administrator")
                    .isHidden(false)
                    .build();
            userProfileRepository.save(profile);
        }

        List<User> rogueSuperAdmins = userRepository.findByRoleAndUsernameNot(UserRole.SUPER_ADMIN, SUPER_ADMIN_USERNAME);
        if (!rogueSuperAdmins.isEmpty()) {
            for (User rogue : rogueSuperAdmins) {
                rogue.setRole(UserRole.ADMIN);
            }
            userRepository.saveAll(rogueSuperAdmins);
            log.warn("[Volur Lite] 已自动降级 {} 个非法的 SUPER_ADMIN 账号为 ADMIN 角色，严格确保超级管理员唯一性。", rogueSuperAdmins.size());
        }

        log.info("""
                
                ========================================================================================
                >> [Volur Lite] SuperAdmin Credentials Initialized:
                >> Username : {}
                >> Password : {}
                >> Notice   : A new secure random password is dynamically generated on every system restart.
                ========================================================================================""",
                SUPER_ADMIN_USERNAME, rawPassword);
    }

    private String generateSecurePassword() {
        return RandomUtil.randomStringUpper(4) +
                RandomUtil.randomString(8) +
                RandomUtil.randomNumbers(4) +
                "#" +
                RandomUtil.randomString(3);
    }

}
