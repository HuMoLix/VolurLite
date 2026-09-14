package com.humolix.volur.competition.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

    private String teamSlogan;

    private String avatarUrl;

    @NotEmpty(message = "参赛成员名册不能为空")
    @Valid
    private List<TeamMemberSignupDTO> members = new ArrayList<>();

}
