package com.humolix.volur.challenge.domain.vo.theory;

import com.humolix.volur.challenge.domain.enums.TheoryType;
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
public class TheoryChallengeListVO implements Serializable {

    private Long id;
    private String name;
    private String category;
    private TheoryType type;
    private Integer score;
    private Integer difficulty;
    private Long bankId;
    private String bankName;
    private String authorName;
    private Instant createdAt;

}
