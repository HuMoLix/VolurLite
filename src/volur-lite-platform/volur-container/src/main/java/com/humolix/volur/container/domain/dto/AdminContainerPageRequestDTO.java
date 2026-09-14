package com.humolix.volur.container.domain.dto;

import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import com.humolix.volur.framework.domain.dto.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端容器实例分页查询请求参数")
public class AdminContainerPageRequestDTO extends PageRequestDTO {

    @Schema(description = "模糊搜索关键词（题目名称、用户名、容器ID、Flag）")
    private String keyword;

    @Schema(description = "容器状态筛选")
    private InstanceStartupStatus status;

    @Schema(description = "题目 ID")
    private Long challengeId;

    @Schema(description = "目标用户 ID")
    private Long userId;

    @Schema(description = "赛事/阶段 ID (0 为练习赛，大于0为指定赛事)")
    private Long stageId;

}
