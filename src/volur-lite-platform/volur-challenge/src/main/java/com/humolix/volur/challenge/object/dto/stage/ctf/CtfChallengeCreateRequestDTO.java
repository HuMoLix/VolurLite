package com.humolix.volur.challenge.object.dto.stage.ctf;

import com.humolix.volur.framework.common.dto.AttachmentDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfChallengeCreateRequestDTO {

    @NotBlank(message = "题目名称不能为空")
    private String name;

    @NotBlank(message = "题目描述不能为空")
    private String description;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotNull(message = "难度不能为空 (例如: 1简单, 2中等, 3困难)")
    private Integer difficulty;

    private String staticFlag;

    @NotNull(message = "是否为动态容器选项不能为空")
    private boolean dynamicContainer;

    private List<AttachmentDTO> attachments = new ArrayList<>();

    private String imageName;

    private Integer containerPort;

}
