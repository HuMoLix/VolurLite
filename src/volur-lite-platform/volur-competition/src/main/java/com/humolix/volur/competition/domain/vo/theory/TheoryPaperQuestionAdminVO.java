package com.humolix.volur.competition.domain.vo.theory;

import com.humolix.volur.challenge.domain.enums.TheoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TheoryPaperQuestionAdminVO implements Serializable {

    private Long paperId;
    private Long challengeId;
    private String name;
    private String bankName;
    private String category;
    private TheoryType type;
    private Integer score;
    private Integer sortOrder;

}
