package com.humolix.volur.user.mapper;

import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.entity.UserProfile;
import com.humolix.volur.user.domain.vo.AdminUserDetailsVO;
import com.humolix.volur.user.domain.vo.UserProfileResponseVO;
import com.humolix.volur.user.domain.vo.UserSummaryVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.createdAt", target = "createdAt")
    @Mapping(source = "user.role", target = "role", defaultValue = "USER")
    @Mapping(source = "profile.avatarUrl", target = "avatarUrl")
    @Mapping(source = "profile.org", target = "org")
    @Mapping(source = "profile.bio", target = "bio")
    UserSummaryVO toUserSummaryVO(User user, UserProfile profile);

    default UserSummaryVO toUserSummaryVO(User user) {
        if (user == null) {
            return null;
        }
        return toUserSummaryVO(user, null);
    }

    List<UserSummaryVO> toUserSummaryVOList(List<User> users);

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.phone", target = "phone")
    @Mapping(source = "user.gender", target = "gender")
    @Mapping(source = "user.birthday", target = "birthday")
    @Mapping(source = "user.createdAt", target = "createdAt")
    @Mapping(source = "user.role", target = "role", defaultValue = "USER")
    @Mapping(source = "profile.avatarUrl", target = "avatarUrl")
    @Mapping(source = "profile.bannerUrl", target = "bannerUrl")
    @Mapping(source = "profile.bio", target = "bio")
    @Mapping(source = "profile.org", target = "org")
    @Mapping(source = "profile.major", target = "major")
    @Mapping(source = "profile.notice", target = "notice")
    @Mapping(source = "profile.emailPrivacy", target = "emailPrivacy")
    @Mapping(source = "profile.phonePrivacy", target = "phonePrivacy")
    @Mapping(source = "profile.orgPrivacy", target = "orgPrivacy")
    UserProfileResponseVO toUserProfileResponseVO(User user, UserProfile profile);

    default UserProfileResponseVO toUserProfileResponseVO(User user) {
        if (user == null) {
            return null;
        }
        return toUserProfileResponseVO(user, null);
    }

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.phone", target = "phone")
    @Mapping(source = "user.realName", target = "realName")
    @Mapping(source = "user.gender", target = "gender")
    @Mapping(source = "user.birthday", target = "birthday")
    @Mapping(source = "user.status", target = "status")
    @Mapping(source = "user.isHidden", target = "isHidden")
    @Mapping(source = "user.lastLoginIp", target = "lastLoginIp")
    @Mapping(source = "user.createdAt", target = "createdAt")
    @Mapping(source = "user.updatedAt", target = "updatedAt")
    @Mapping(source = "user.role", target = "role", defaultValue = "USER")
    @Mapping(source = "profile.avatarUrl", target = "avatarUrl")
    @Mapping(source = "profile.org", target = "org")
    @Mapping(source = "profile.major", target = "major")
    @Mapping(source = "profile.bio", target = "bio")
    AdminUserDetailsVO toAdminUserDetailsVO(User user, UserProfile profile);

    default AdminUserDetailsVO toAdminUserDetailsVO(User user) {
        if (user == null) {
            return null;
        }
        return toAdminUserDetailsVO(user, null);
    }

    List<AdminUserDetailsVO> toAdminUserDetailsVOList(List<User> users);

}
