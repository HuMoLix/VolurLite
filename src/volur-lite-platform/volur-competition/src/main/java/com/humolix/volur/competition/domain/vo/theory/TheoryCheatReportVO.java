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
public class TheoryCheatReportVO implements Serializable {

    private Integer violationCount;

    private Integer maxAllowedViolations;

    @Builder.Default
    private Boolean isBanned = false;

    private String message;

}
