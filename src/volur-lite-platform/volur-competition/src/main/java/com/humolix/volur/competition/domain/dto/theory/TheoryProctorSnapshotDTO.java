package com.humolix.volur.competition.domain.dto.theory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheoryProctorSnapshotDTO {

    private String screenFrame;

    private String reason;

    private Long timestamp;

}