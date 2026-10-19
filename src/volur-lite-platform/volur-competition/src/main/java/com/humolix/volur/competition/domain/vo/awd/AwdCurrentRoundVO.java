package com.humolix.volur.competition.domain.vo.awd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwdCurrentRoundVO implements Serializable {

    private Integer roundNumber;
    private Instant startTime;
    private Instant endTime;
    private Integer remainingSeconds;
    private List<AwdTargetStatusVO> targets;

}
