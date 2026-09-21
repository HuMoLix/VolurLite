package com.humolix.volur.competition.domain.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamUpdateRequestDTO {

    private String teamName;

    private String avatarUrl;

    @jakarta.validation.constraints.Size(max = 512, message = "队伍简介长度不能超过 512 个字符")
    private String description;

    @jakarta.validation.constraints.Size(max = 128, message = "队伍单位长度不能超过 128 个字符")
    private String organization;

    private com.humolix.volur.competition.domain.enums.OrganizationType organizationType;

    @Valid
    private List<TeamMemberSignupDTO> members = new ArrayList<>();

    @Valid
    private List<TeamLeadersSignupDTO> leaders = new ArrayList<>();

}
