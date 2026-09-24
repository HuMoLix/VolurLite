package com.humolix.volur.verification.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRiskStatisticsVO {

    @Builder.Default
    private Long totalBannedCount = 0L;

    @Builder.Default
    private Long activeBannedCount = 0L;

    @Builder.Default
    private Long totalWhitelistedCount = 0L;

    @Builder.Default
    private Long todayAutoBannedCount = 0L;

    @Builder.Default
    private Long todayManualBannedCount = 0L;

}
