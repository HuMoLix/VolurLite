package com.humolix.volur.competition.domain.vo;

import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionMemberVO implements Serializable {

    private Long id;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String realName;
    private com.humolix.volur.user.domain.enums.Gender gender;
    private String phone;
    private String email;
    private String idCard;
    private String employeeNumber;
    private String department;
    private String position;
    private MemberRole role;
    private MemberStatus status;
    private Instant joinedAt;

}
