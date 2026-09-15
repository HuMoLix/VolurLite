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
public class IswChallengeListVO implements Serializable {

    private Long id;
    private String name;
    private String category;
    private Integer difficulty;
    private String authorName;
    private Instant createdAt;

}
