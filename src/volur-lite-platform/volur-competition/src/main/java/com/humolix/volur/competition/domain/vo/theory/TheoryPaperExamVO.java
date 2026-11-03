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
public class TheoryPaperExamVO implements Serializable {

    private Long paperId;
    private Long competitionId;
    private Integer totalQuestions;
    private Integer totalScore;
    private Boolean isSubmitted;
    private Integer myTotalScore;
    private List<TheoryPaperQuestionVO> questions;

}
