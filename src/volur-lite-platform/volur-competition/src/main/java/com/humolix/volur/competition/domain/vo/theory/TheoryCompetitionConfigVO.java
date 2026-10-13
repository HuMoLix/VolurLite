package com.humolix.volur.competition.domain.vo.theory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TheoryCompetitionConfigVO implements Serializable {

    private Long competitionId;
    private List<Long> bankIds;
    private List<TheoryBankSummaryVO> banks;
    @Builder.Default
    private Integer singleChoiceCount = 0;
    @Builder.Default
    private Integer singleChoiceScore = 2;
    @Builder.Default
    private Integer multipleChoiceCount = 0;
    @Builder.Default
    private Integer multipleChoiceScore = 4;
    @Builder.Default
    private Integer trueFalseCount = 0;
    @Builder.Default
    private Integer trueFalseScore = 2;
    @Builder.Default
    private Integer essayCount = 0;
    @Builder.Default
    private Integer essayScore = 10;
    @Builder.Default
    private Boolean isRandom = true;
    @Builder.Default
    private Boolean shuffleOptions = true;
    @Builder.Default
    private Integer totalQuestions = 0;
    @Builder.Default
    private Integer totalScore = 0;

}
