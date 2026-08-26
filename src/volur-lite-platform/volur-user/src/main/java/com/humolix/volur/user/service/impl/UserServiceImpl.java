package com.humolix.volur.user.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.vo.UserProfileResponseVO;
import com.humolix.volur.user.domain.vo.UserSummaryVO;
import com.humolix.volur.user.mapper.UserMapper;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserSummaryVO getCurrentUser(Long userId) {
        User user = userRepository.findByIdWithProfileAndRole(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        return userMapper.toUserSummaryVO(user);
    }

    @Override
    public UserProfileResponseVO getUserProfileById(Long userId) {
        User user = userRepository.findByIdWithProfileAndRole(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        return userMapper.toUserProfileResponseVO(user);
    }

}
