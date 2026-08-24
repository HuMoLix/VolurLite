package com.humolix.volur.challenge.object.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTrainingChallengeListResponseVO implements Serializable {

    private Long id;
    private Long challengeId;
    private String name;
    private String categoryName;
    private Integer initialPoints;
    private Integer minimumPoints;
    private Integer currentPoints;
    private Integer solveCount;
    private boolean dynamicScore;
    private Float decayCoefficient;
    private boolean active;
    private boolean dynamicContainer;
    private Instant createdAt;

}
