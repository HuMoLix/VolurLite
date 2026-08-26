package com.humolix.volur.competition.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamSignupRequestDTO {

    @NotBlank(message = "队伍名称不能为空")
    private String teamName;

    private String teamSlogan;

    private String avatarUrl;

    private Map<String, String> captainData;

    @Valid
    private List<TeamMemberInputDTO> leaders = new ArrayList<>();

    @Valid
    private List<TeamMemberInputDTO> members = new ArrayList<>();

}
