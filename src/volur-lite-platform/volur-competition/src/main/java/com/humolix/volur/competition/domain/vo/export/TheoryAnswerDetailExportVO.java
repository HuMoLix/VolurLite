package com.humolix.volur.competition.domain.vo.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TheoryAnswerDetailExportVO implements Serializable {

    private Long userId;
    private String username;
    private String realName;
    private String teamName;
    private Long paperId;
    private String challengeName;
    private String type;
    private Integer score;
    private String userAnswer;
    private String standardAnswer;
    private Boolean isCorrect;
    private Integer scoreObtained;

}
