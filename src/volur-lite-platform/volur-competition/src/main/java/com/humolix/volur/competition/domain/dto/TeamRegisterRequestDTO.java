package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamRegisterRequestDTO {

    @NotBlank(message = "队伍名称不能为空")
    @Trimmed
    private String teamName;

    @Trimmed
    private String avatarUrl;

    /**
     * 自定义报名表单提交数据 (fieldKey -> fieldValue)
     */
    private Map<String, String> dynamicFormData;

}
