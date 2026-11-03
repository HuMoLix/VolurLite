package com.humolix.volur.competition.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionWriteupAuditDTO {

    private String status;

    private Integer score;

    private String comment;

}
