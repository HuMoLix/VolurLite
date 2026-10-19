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
public class TheoryChallengeDetailVO implements Serializable {

    private Long id;
    private String name;
    private String category;
    private TheoryType type;
    private String content;
    private String options;
    private String standardAnswer;
    private String explanation;
    private Integer score;
    private Integer difficulty;
    private Long bankId;
    private String bankName;
    private Long authorId;
    private String authorName;
    private Instant createdAt;
    private Instant updatedAt;

}
