package com.humolix.volur.challenge.object.vo.stage.ctf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CtfTrainingChallengePageResponseVO implements Serializable {

    private Long id;
    private String name;
    private String category;
    private Integer points;
    private Integer solvedCount;
    private Integer difficulty;
    private boolean solved;
    private boolean dynamicContainer;
    private String containerUrl;
    private Integer remainingTimeSeconds;

}
