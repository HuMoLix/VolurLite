package com.humolix.volur.competition.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionAnnouncementVO implements Serializable {

    private Long id;
    private Long competitionId;
    private String title;
    private String content;
    private String level;
    private Boolean isPinned;
    private Long challengeId;
    private String challengeName;
    private Long createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;

}
