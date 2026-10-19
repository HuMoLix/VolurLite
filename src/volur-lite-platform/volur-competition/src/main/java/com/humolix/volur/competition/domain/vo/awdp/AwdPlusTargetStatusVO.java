package com.humolix.volur.competition.domain.vo.awdp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwdPlusTargetStatusVO implements Serializable {

    private Long challengeId;
    private String challengeName;
    private String category;
    private Integer initialPoints;
    private Integer currentPoints;
    private String patchStatus; // UNPATCHED, PENDING, PASSED, FAILED
    private String sourceCodeUrl;

}
