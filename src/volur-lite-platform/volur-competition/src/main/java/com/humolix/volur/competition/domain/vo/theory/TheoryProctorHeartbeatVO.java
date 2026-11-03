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
public class TheoryProctorHeartbeatVO implements Serializable {

    @Builder.Default
    private Boolean acknowledged = true;

    @Builder.Default
    private Boolean isBanned = false;

    private Integer violationCount;

    private Long serverTime;

}
