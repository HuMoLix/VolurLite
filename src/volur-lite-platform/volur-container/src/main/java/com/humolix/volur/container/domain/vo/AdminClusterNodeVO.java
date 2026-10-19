package com.humolix.volur.container.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class AdminClusterNodeVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String nodeName;

    private String ipAddress;

    private Integer sshPort;

    private String sshUser;

    private Boolean hasSshAuth;

    private String authType;

    private Integer dockerPort;

    private Boolean tlsEnabled;

    private String tlsCertPath;

    private String clusterType;

    private String role;

    private Boolean isResourceNode;

    private String status;

    private String osInfo;

    private String dockerVersion;

    private Integer cpuCores;

    private Long memoryBytes;

    private String memoryFormatted;

    private Integer containerCount;

    private Integer imageCount;

    private Boolean inCluster;

    private String clusterNodeId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Instant joinTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Instant lastHeartbeat;

}
