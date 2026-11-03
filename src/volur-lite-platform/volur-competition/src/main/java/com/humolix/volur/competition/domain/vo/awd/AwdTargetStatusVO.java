package com.humolix.volur.competition.domain.vo.awd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwdTargetStatusVO implements Serializable {

    private Long challengeId;
    private String challengeName;
    private Long teamId;
    private String teamName;
    private String host;
    private Integer port;
    private String status; // NORMAL, ATTACKED, DOWN
    private Integer currentPoints;

}
