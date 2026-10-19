package com.humolix.volur.competition.domain.dto.awdp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AwdPlusPatchUploadRequestDTO {

    @NotNull(message = "题目ID不能为空")
    private Long challengeId;

    @NotBlank(message = "补丁文件URL不能为空")
    private String patchFileUrl;

}
