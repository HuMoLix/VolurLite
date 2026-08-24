package com.humolix.volur.user.service.admin;

import com.humolix.volur.user.domain.dto.AdminUserCreateRequestDTO;
import com.humolix.volur.user.domain.dto.AdminUserUpdateRequestDTO;
import com.humolix.volur.user.entity.Role;
import com.humolix.volur.user.domain.vo.AdminUserDetailsVO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminUserService {

    Page<AdminUserDetailsVO> getUserList(Integer page, Integer size, String keyword);

    AdminUserDetailsVO getUserDetails(Long id);

    Long createUser(AdminUserCreateRequestDTO request);

    void updateUser(Long id, AdminUserUpdateRequestDTO request);

    void deleteUser(Long id);

    void deleteUsers(List<Long> ids);

    void toggleHiddenStatus(Long id, Boolean isHidden);

    void toggleUserStatus(Long id, Boolean status);

    void toggleUsersStatus(List<Long> ids, Boolean status);

    List<Role> getAllRoles();

}
