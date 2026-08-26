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

    private String teamSlogan;

    private String avatarUrl;

    @Valid
    private List<TeamMemberInputDTO> leaders = new ArrayList<>();

    @Valid
    private List<TeamMemberInputDTO> members = new ArrayList<>();

}
