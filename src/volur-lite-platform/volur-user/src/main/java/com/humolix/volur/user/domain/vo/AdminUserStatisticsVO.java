package com.humolix.volur.user.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserStatisticsVO implements Serializable {

    @Builder.Default
    private Long totalUsers = 0L;

    @Builder.Default
    private Long adminCount = 0L;

    @Builder.Default
    private Long bannedCount = 0L;

    @Builder.Default
    private Long activeCount = 0L;

}
