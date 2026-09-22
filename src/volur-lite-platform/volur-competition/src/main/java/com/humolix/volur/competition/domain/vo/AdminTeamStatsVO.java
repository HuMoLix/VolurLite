package com.humolix.volur.competition.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "竞赛报名队伍统计概览视图")
public class AdminTeamStatsVO {

    @Schema(description = "报名总队伍数")
    private Long total;

    @Schema(description = "待审核队伍数")
    private Long pending;

    @Schema(description = "已通过资质队伍数")
    private Long approved;

    @Schema(description = "已驳回报名队伍数")
    private Long rejected;

}
