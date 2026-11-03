package com.humolix.volur.container.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminContainerBatchStopRequestDTO {

    @NotEmpty(message = "实例 ID 列表不能为空")
    private List<Long> instanceIds;

}
