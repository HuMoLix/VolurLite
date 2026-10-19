package com.humolix.volur.competition.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTeamStatsVO {

    private Long total;

    private Long pending;

    private Long approved;

    private Long rejected;
 
    private Long banned;

    private Long ended;

}
