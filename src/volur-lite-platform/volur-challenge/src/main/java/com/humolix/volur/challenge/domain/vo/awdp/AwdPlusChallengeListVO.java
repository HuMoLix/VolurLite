package com.humolix.volur.challenge.domain.vo.awdp;

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
public class AwdPlusChallengeListVO implements Serializable {

    private Long id;
    private String name;
    private String category;
    private Integer difficulty;
    private String authorName;
    private String imageName;
    private Instant createdAt;

}
