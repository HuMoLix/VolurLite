package com.humolix.volur.challenge.domain.dto.awd;

import com.humolix.volur.challenge.domain.enums.AwdCategory;
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
    private String name;

    private String description;

    @NotNull(message = "分类不能为空")
    private AwdCategory category;

    private Integer difficulty;

    private String attachmentUrl;

    @NotBlank(message = "Docker镜像名不能为空")
    private String imageName;

    @NotNull(message = "服务端口不能为空")
    private Integer containerPort;

    private String checkScriptUrl;

    private String flagPath;

}
