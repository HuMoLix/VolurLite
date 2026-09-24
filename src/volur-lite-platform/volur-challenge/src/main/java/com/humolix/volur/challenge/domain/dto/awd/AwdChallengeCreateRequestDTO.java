package com.humolix.volur.challenge.domain.dto.awd;

import com.humolix.volur.challenge.domain.enums.AwdCategory;
import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AwdChallengeCreateRequestDTO {

    @NotBlank(message = "题目名称不能为空")
    @Trimmed
    private String name;

    private String description;

    @NotNull(message = "分类不能为空")
    private AwdCategory category;

    private Integer difficulty;

    @Trimmed
    private String attachmentUrl;

    @NotBlank(message = "Docker镜像名不能为空")
    @Trimmed
    private String imageName;

    @NotNull(message = "服务端口不能为空")
    private Integer containerPort;

    @Trimmed
    private String checkScriptUrl;

    @Trimmed
    private String flagPath;

}
