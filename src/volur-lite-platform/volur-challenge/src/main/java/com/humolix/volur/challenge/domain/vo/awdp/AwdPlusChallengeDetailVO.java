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
public class AwdPlusChallengeDetailVO implements Serializable {

    private Long id;
    private String name;
    private String description;
    private String category;
    private Long authorId;
    private String authorName;
    private Integer difficulty;
    private String sourceCodeUrl;
    private String imageName;
    private Integer containerPort;
    private String patchVerifyScript;
    private String exploitVerifyScript;
    private Instant createdAt;
    private Instant updatedAt;

}
