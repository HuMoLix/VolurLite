package com.humolix.volur.competition.domain.vo.theory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTheoryCandidateMonitorVO implements Serializable {

    private Long userId;

    private String username;

    private Long teamId;

    private String teamName;

    @Builder.Default
    private Boolean isOnline = false;

    @Builder.Default
    private Boolean fullscreen = false;

    @Builder.Default
    private Boolean screenActive = false;

    private String screenFrame;

    private String highResFrame;

    private Long highResTimestamp;

    @Builder.Default
    private Integer violationCount = 0;

    @Builder.Default
    private Boolean isBanned = false;

    @Builder.Default
    private Integer answeredCount = 0;

    private Long lastHeartbeatTime;

    private Long connectedSince;

}
