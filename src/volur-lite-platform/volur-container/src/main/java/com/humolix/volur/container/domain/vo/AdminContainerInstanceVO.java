package com.humolix.volur.container.domain.vo;

import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContainerInstanceVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long challengeId;

    private String challengeTitle;

    private String challengeCategory;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String username;

    private String containerId;

    private String accessUrl;

    private String flag;

    private InstanceStartupStatus status;

    private Instant createdAt;

    private Instant expiresAt;

    private Integer remainingSeconds;

    private String containerType;

    private String imageName;

    private String containerName;

    private String ports;

    private String volumes;

    private String envVars;

    private String nodeIp;

    private String nodeName;

    private String nodeRole;

    private Boolean isNodeOffline;

}
