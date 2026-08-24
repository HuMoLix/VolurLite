package com.humolix.volur.container.dto;

import com.humolix.volur.container.entity.enums.InstanceStartupStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainerRunResultDTO {

    private String containerId;
    private String accessUrl;
    private Integer mappedPort;
    private InstanceStartupStatus status;
}
