package com.humolix.volur.user.service.admin;

import com.humolix.volur.user.domain.dto.AdminUserCreateRequestDTO;
import com.humolix.volur.user.domain.dto.AdminUserPageRequestDTO;
import com.humolix.volur.user.domain.dto.AdminUserUpdateRequestDTO;
import com.humolix.volur.user.domain.vo.AdminUserDetailsVO;
import com.humolix.volur.user.domain.vo.AdminUserStatisticsVO;
import com.humolix.volur.user.domain.vo.RoleVO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminUserService {

    Page<AdminUserDetailsVO> getUserList(AdminUserPageRequestDTO pageRequest);

    AdminUserStatisticsVO getUserStatistics();

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
