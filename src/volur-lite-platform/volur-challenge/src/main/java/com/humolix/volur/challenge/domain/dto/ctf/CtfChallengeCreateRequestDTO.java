package com.humolix.volur.challenge.domain.dto.ctf;

import com.humolix.volur.challenge.domain.enums.ChallengeDifficulty;
import com.humolix.volur.challenge.domain.enums.ChallengeType;
import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfChallengeCreateRequestDTO {

    @NotBlank(message = "题目名称不能为空")
    @Trimmed
    private String name;

    private String description;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotNull(message = "题目类型不能为空")
    private ChallengeType type;

    private ChallengeDifficulty difficulty;

    @Trimmed
    private String flag;

    @Trimmed
    private String attachmentUrl;

    @Trimmed
    private String attachmentName;

    private java.util.List<CtfAttachmentDTO> attachments;

    @Trimmed
    private String imageName;

    private Integer containerPort;

    private Integer cpuLimit;

    private Integer memoryLimit;

}
