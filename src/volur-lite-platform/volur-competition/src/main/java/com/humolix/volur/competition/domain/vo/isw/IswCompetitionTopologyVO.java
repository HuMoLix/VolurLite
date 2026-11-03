package com.humolix.volur.competition.domain.vo.isw;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IswCompetitionTopologyVO implements Serializable {

    private Long challengeId;
    private String name;
    private String description;
    private String category;
    private Integer initialPoints;
    private Integer currentPoints;
    private String topologyJson;
    private Integer unlockedStage;

}
