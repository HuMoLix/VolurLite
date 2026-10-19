package com.humolix.volur.challenge.domain.vo.awd;

import com.humolix.volur.challenge.domain.enums.AwdCategory;
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
public class AwdChallengeListVO implements Serializable {

    private Long id;
    private String name;
    private AwdCategory category;
    private Integer difficulty;
    private String authorName;
    private String imageName;
    private Instant createdAt;

}
