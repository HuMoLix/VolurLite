package com.humolix.volur.competition.domain.dto;

import com.humolix.volur.competition.domain.enums.OrganizationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTeamUpdateDTO implements Serializable {

    @NotBlank(message = "队伍名称不能为空")
    @Size(min = 2, max = 32, message = "队伍名称长度必须在 2 到 32 个字符之间")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String teamName;

    @Size(max = 512, message = "队伍简介长度不能超过 512 个字符")
    private String description;

    @Size(max = 128, message = "所属单位长度不能超过 128 个字符")
    @com.humolix.volur.framework.common.annotation.Trimmed
    private String organization;

    private OrganizationType organizationType;

    private List<@Valid AdminTeamMemberDTO> members;

    private List<@Valid AdminTeamMemberDTO> leaders;

}
