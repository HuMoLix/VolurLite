package com.humolix.volur.competition.domain.dto.theory;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheoryCheatReportDTO {

    @NotBlank(message = "违规事件类型不能为空")
    private String eventType;

    private String detail;

    private Long timestamp;

}
