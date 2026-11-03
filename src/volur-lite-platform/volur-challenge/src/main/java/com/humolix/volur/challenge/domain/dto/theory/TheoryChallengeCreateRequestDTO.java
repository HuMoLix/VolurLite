package com.humolix.volur.challenge.domain.dto.theory;

import com.humolix.volur.challenge.domain.enums.TheoryType;
import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheoryChallengeCreateRequestDTO {

    @NotBlank(message = "题目名称不能为空")
    @Trimmed
    private String name;

    @Trimmed
    private String category;

    @NotNull(message = "题目类型不能为空")
    private TheoryType type;

    @NotBlank(message = "题干内容不能为空")
    private String content;

    private String options;

    @NotBlank(message = "标准答案不能为空")
    @Trimmed
    private String standardAnswer;

    private String explanation;

    private Integer score;

    private Integer difficulty;

    private Long bankId;

}
