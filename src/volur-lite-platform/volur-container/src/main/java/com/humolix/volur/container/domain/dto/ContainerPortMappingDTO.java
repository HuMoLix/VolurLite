package com.humolix.volur.container.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainerPortMappingDTO implements Serializable {

    private Integer hostPort;

    private Integer containerPort;

    private String protocol;

}
