package com.humolix.volur.competition.domain.vo.export;

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
public class TheoryScoreExportVO implements Serializable {

    private Long competitionId;
    private String competitionTitle;
    private String exportScope;
    private Instant exportTime;
    private List<TheoryCandidateScoreExportVO> rankings;
    private List<TheoryAnswerDetailExportVO> details;

}
