package com.humolix.volur.competition.domain.dto.theory;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheoryAnswerItemDTO {

    @NotNull(message = "试卷题目ID不能为空")
    private Long paperId;

    private String answer;

}
