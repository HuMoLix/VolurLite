package com.humolix.volur.user.service.admin;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.user.domain.dto.AdminUserCreateRequestDTO;
import com.humolix.volur.user.domain.dto.AdminUserUpdateRequestDTO;
import com.humolix.volur.user.domain.vo.AdminUserDetailsVO;
import com.humolix.volur.user.domain.vo.RoleVO;

import java.util.List;

public interface AdminUserService {

    PageResponse<AdminUserDetailsVO> getUserList(Integer page, Integer size, String keyword);

    AdminUserDetailsVO getUserDetails(Long id);

    Long createUser(AdminUserCreateRequestDTO request);

    void updateUser(Long id, AdminUserUpdateRequestDTO request);

    void deleteUser(Long id);

    void deleteUsers(List<Long> ids);

    void toggleHiddenStatus(Long id, boolean isHidden);

    void toggleUserStatus(Long id, boolean status);

    void toggleUsersStatus(List<Long> ids, boolean status);

    List<RoleVO> getAllRoles();

}
