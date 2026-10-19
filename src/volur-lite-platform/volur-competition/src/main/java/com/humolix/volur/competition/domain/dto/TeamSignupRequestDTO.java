package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.competition.domain.enums.OrganizationType;
import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamSignupRequestDTO {

    @NotBlank(message = "队伍名称不能为空")
    @Trimmed
    private String teamName;

    @Trimmed
    private String avatarUrl;

    @Size(max = 512, message = "队伍简介长度不能超过 512 个字符")
    @Trimmed
    private String description;

    @Size(max = 128, message = "队伍单位长度不能超过 128 个字符")
    @Trimmed
    private String organization;

    private OrganizationType organizationType;

    @NotEmpty(message = "参赛成员名册不能为空")
    private List<@Valid TeamMemberSignupDTO> members = new ArrayList<>();

    private List<@Valid TeamLeadersSignupDTO> leaders = new ArrayList<>();

}
