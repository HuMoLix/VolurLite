package com.humolix.volur.container.domain.vo;

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
public class AdminClusterImageVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String repository;

    private String tag;

    private String fullImageName;

    private String imageId;

    private Long sizeBytes;

    private Boolean inRegistry;

    private Instant createdAt;

    private Instant updatedAt;

}
