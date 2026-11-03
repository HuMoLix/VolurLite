package com.humolix.volur.container.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainerVolumeMappingDTO implements Serializable {

    private String hostPath;

    private String containerPath;

    private String mode;

}
