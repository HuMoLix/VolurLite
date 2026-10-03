package com.humolix.volur.container.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContainerTerminalInfoVO {

    private Boolean ready;

    private String containerShortId;

    private String currentUser;

    private String workingDir;

    private String prompt;

    private String taskState;

    private String message;

}
