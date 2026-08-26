package com.humolix.volur.competition.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamJoinRequestDTO {

    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;

    /**
     * 自定义报名表单提交数据 (fieldKey -> fieldValue)
     */
    private Map<String, String> dynamicFormData;

}
