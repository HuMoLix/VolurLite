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
public class AwdChallengeDetailVO implements Serializable {

    private Long id;
    private String name;
    private String description;
    private AwdCategory category;
    private Long authorId;
    private String authorName;
    private Integer difficulty;
    private String attachmentUrl;
    private String imageName;
    private Integer containerPort;
    private String checkScriptUrl;
    private String flagPath;
    private Instant createdAt;
    private Instant updatedAt;

}
