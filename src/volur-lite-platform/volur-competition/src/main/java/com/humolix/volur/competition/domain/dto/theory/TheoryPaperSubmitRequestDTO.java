package com.humolix.volur.competition.domain.dto.theory;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheoryPaperSubmitRequestDTO {

    @NotEmpty(message = "答卷内容不能为空")
    private List<TheoryAnswerItemDTO> answers;

}
