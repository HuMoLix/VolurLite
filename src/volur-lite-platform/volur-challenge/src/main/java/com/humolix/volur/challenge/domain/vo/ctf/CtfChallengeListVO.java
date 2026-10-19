package com.humolix.volur.challenge.domain.vo.ctf;

import com.humolix.volur.challenge.domain.enums.ChallengeDifficulty;
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
    private String categoryIcon;
    private ChallengeType type;
    private ChallengeDifficulty difficulty;
    private String authorName;
    private Instant createdAt;

}
