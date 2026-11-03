package com.humolix.volur.competition.domain.vo.export;

import com.humolix.volur.competition.domain.enums.MemberRole;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.user.domain.enums.Gender;
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
public class CtfTeamMemberExportVO implements Serializable {

    private Long teamId;
    private String teamName;
    private String organization;
    private MemberRole role;
    private Long userId;
    private String username;
    private String realName;
    private Gender gender;
    private String phone;
    private String email;
    private String idCard;
    private String employeeNumber;
    private String department;
    private String position;
    private MemberStatus status;
    private Instant joinedAt;

}
