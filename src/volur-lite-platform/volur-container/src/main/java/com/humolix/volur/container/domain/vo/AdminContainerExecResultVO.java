package com.humolix.volur.container.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContainerExecResultVO {

    private String output;

    private String errorOutput;

    @Builder.Default
    private Integer exitCode = 0;

    private String workingDir;

    @Builder.Default
    private Long executionTimeMs = 0L;

    private String containerShortId;

    private String currentUser;

    private String prompt;

}
