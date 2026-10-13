package com.humolix.volur.competition.domain.dto.theory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheoryProctorHeartbeatDTO {

    private Boolean fullscreen;

    private Boolean screenActive;

    private String screenFrame;

    private Long timestamp;

    private Integer activeIndex;

    private Integer answeredCount;

}
