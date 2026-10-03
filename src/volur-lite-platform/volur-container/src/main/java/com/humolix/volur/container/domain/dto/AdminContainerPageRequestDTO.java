package com.humolix.volur.container.domain.dto;

import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import com.humolix.volur.framework.domain.dto.PageRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminContainerPageRequestDTO extends PageRequestDTO {

    private String keyword;

    private InstanceStartupStatus status;

    private Long challengeId;

    private Long userId;

    private Long stageId;

}
