package com.humolix.volur.competition.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionAnnouncementDTO {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 128, message = "公告标题长度不能超过128字符")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    private String level;

    private Boolean isPinned;

    private Long challengeId;

    private String challengeName;

}
