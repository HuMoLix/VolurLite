package com.humolix.volur.container.domain.vo;

import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeInstanceResponseVO implements Serializable {

    private Long challengeId;
    private String accessUrl;
    private InstanceStartupStatus status;
    private Instant createdAt;
    private Instant expiresAt;
    private Integer remainingTimeSeconds;

}
