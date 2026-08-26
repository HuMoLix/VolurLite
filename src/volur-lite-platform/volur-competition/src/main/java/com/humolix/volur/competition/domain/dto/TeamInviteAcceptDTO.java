package com.humolix.volur.competition.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamInviteAcceptDTO {

    private Map<String, String> dynamicFormData;

}
