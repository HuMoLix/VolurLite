package com.humolix.volur.challenge.object.vo.admin;

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
public class AdminChallengeListResponseVO implements Serializable {

    private Long id;
    private String name;
    private String categoryName;
    private String authorName;
    private Integer difficulty;
    private boolean dynamicContainer;
    private boolean deleted;
    private Instant createdAt;

}
