package com.humolix.volur.notification.domain.dto;

import com.humolix.volur.notification.domain.enums.BroadcastDisplayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastMessageDTO {

    @NotBlank(message = "广播标题不能为空")
    private String title;

    @NotBlank(message = "广播内容不能为空")
    private String content;

    @NotNull(message = "展示类型不能为空")
    private BroadcastDisplayType displayType;

    private String coverImageUrl;

    private String linkUrl;

    private Boolean forcePopup;

    private Integer priority;

    private Instant expireTime;

}
