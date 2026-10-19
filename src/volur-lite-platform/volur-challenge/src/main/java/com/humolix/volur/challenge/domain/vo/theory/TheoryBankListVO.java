package com.humolix.volur.challenge.domain.vo.theory;

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
public class TheoryBankListVO implements Serializable {

    private Long id;
    private String name;
    private String description;
    private String category;
    @Builder.Default
    private Long questionCount = 0L;
    private String authorName;
    private Instant createdAt;
    private Instant updatedAt;

}
