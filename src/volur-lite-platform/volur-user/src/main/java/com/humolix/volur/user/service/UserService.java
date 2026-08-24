package com.humolix.volur.user.service;

import com.humolix.volur.user.domain.vo.UserProfileResponseVO;
import com.humolix.volur.user.domain.vo.UserSummaryVO;

public interface UserService {

    UserSummaryVO getCurrentUser(Long userId);

    UserProfileResponseVO getUserProfileById(Long userId);

}
