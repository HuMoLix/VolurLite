package com.humolix.volur.container.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainerEnvVarDTO implements Serializable {

    private String name;

    private String value;

}
