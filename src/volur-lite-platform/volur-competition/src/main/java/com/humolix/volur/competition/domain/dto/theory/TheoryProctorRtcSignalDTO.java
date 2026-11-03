package com.humolix.volur.competition.domain.dto.theory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheoryProctorRtcSignalDTO {

    private Long targetUserId;

    private String type;

    private String sdp;

    private String candidate;

    private String sdpMid;

    private Integer sdpMLineIndex;

    private String sessionId;

}