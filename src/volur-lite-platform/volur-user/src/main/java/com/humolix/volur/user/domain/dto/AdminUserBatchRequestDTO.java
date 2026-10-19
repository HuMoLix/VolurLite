package com.humolix.volur.user.domain.dto;

import jakarta.validation.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserBatchRequestDTO {

    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> ids;

    private Boolean status;
}
