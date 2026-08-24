package com.humolix.volur.challenge.object.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeContainerStatusResponseVO implements Serializable {

    private String containerId;
    private String status;
    private String host;
    private Integer port;
    private String imageName;

}
