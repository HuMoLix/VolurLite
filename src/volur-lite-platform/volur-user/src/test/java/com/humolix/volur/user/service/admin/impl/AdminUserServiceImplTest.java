package com.humolix.volur.user.service.admin.impl;

import com.humolix.volur.user.domain.dto.AdminUserUpdateRequestDTO;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.mapper.UserMapper;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminUserServiceImpl adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl(
                userRepository,
                userProfileRepository,
                eventPublisher,
                userMapper,
                passwordEncoder
        );
    }

    @Test
    void updateUser_withNullOriginalEmail_shouldUpdateEmailWithoutNpe() {
        User user = User.builder()
                .id(1001L)
                .username("testuser")
                .email(null)
                .role(UserRole.USER)
                .status(true)
                .build();

        given(userRepository.findById(1001L)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("newemail@example.com")).willReturn(false);

        AdminUserUpdateRequestDTO request = new AdminUserUpdateRequestDTO();
        request.setEmail("newemail@example.com");

        Assertions.assertDoesNotThrow(() -> adminUserService.updateUser(1001L, request));
        Assertions.assertEquals("newemail@example.com", user.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_withSameEmail_shouldNotTriggerEmailDuplicateCheck() {
        User user = User.builder()
                .id(1002L)
                .username("testuser2")
                .email("existing@example.com")
                .role(UserRole.USER)
                .status(true)
                .build();

        given(userRepository.findById(1002L)).willReturn(Optional.of(user));

        AdminUserUpdateRequestDTO request = new AdminUserUpdateRequestDTO();
        request.setEmail("existing@example.com");

        Assertions.assertDoesNotThrow(() -> adminUserService.updateUser(1002L, request));
        verify(userRepository).save(user);
    }
}
