package com.humolix.volur.notification.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBroadcastStatisticsVO {

    private long totalAnnouncements;

    private long totalPromotions;

    private long urgentModals;

    @Builder.Default
    private boolean sseOnlineStatus = true;

}
