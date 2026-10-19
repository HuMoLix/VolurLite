package com.humolix.volur.challenge.domain.vo.isw;

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
public class IswChallengeDetailVO implements Serializable {

    private Long id;
    private String name;
    private String description;
    private String category;
    private Long authorId;
    private String authorName;
    private Integer difficulty;
    private String topologyJson;
    private String stageFlagsJson;
    private Instant createdAt;
    private Instant updatedAt;

}
