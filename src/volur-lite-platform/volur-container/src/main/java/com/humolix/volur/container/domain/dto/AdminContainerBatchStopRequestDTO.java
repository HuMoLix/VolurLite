package com.humolix.volur.container.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端批量销毁容器实例请求参数")
public class AdminContainerBatchStopRequestDTO {

    @Schema(description = "待停止的实例 ID 列表")
    @NotEmpty(message = "实例 ID 列表不能为空")
    private List<Long> instanceIds;

}
