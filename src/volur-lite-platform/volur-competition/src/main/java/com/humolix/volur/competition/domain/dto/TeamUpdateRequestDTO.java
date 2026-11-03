package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
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

    @Trimmed
    private String teamName;

    @Trimmed
    private String avatarUrl;

    @jakarta.validation.constraints.Size(max = 512, message = "队伍简介长度不能超过 512 个字符")
    @Trimmed
    private String description;

    @jakarta.validation.constraints.Size(max = 128, message = "队伍单位长度不能超过 128 个字符")
    @Trimmed
    private String organization;

    private com.humolix.volur.competition.domain.enums.OrganizationType organizationType;

    private List<@Valid TeamMemberSignupDTO> members = new ArrayList<>();

    private List<@Valid TeamLeadersSignupDTO> leaders = new ArrayList<>();

}
