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
public class TheoryPaperResultVO implements Serializable {

    private Integer totalQuestions;
    private Integer correctCount;
    private Integer totalScore;
    private Integer scoreObtained;
    private Double accuracyRate;

}
