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
public class CompetitionWriteupVO implements Serializable {

    private Long id;
    private Long competitionId;
    private Long teamId;
    private String teamName;
    private Long userId;
    private String username;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String status;
    private Integer score;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;

}
