package com.humolix.volur.challenge.domain.vo.stage.ctf;

import com.humolix.volur.challenge.domain.enums.ChallengeType;
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
public class CtfChallengeListVO implements Serializable {

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private ChallengeType type;
    private Integer difficulty;
    private String authorName;
    private Instant createdAt;

}
