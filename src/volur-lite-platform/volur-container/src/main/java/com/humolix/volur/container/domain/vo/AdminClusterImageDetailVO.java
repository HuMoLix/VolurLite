package com.humolix.volur.container.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClusterImageDetailVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String repository;

    private String tag;

    private String fullImageName;

    private String imageId;

    private Long sizeBytes;

    private Boolean inRegistry;

    private String os;

    private String architecture;

    private String dockerVersion;

    private String author;

    private String workingDir;

    private String user;

    private List<String> entrypoint;

    private List<String> cmd;

    private List<String> env;

    private List<String> exposedPorts;

    private Map<String, String> labels;

    private List<DockerImageLayerVO> layers;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Instant updatedAt;

}
