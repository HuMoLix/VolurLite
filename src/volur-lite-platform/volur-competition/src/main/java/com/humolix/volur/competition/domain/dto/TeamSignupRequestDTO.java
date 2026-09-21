package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.competition.domain.enums.OrganizationType;
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
    private String teamName;

    private String avatarUrl;

    @Size(max = 512, message = "队伍简介长度不能超过 512 个字符")
    private String description;

    @Size(max = 128, message = "队伍单位长度不能超过 128 个字符")
    private String organization;

    private OrganizationType organizationType;

    @NotEmpty(message = "参赛成员名册不能为空")
    @Valid
    private List<TeamMemberSignupDTO> members = new ArrayList<>();

    @Valid
    private List<TeamLeadersSignupDTO> leaders = new ArrayList<>();

}
