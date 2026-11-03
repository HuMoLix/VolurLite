package com.humolix.volur.notification.domain.vo;

import com.humolix.volur.notification.domain.enums.BroadcastDisplayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastMessageVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String content;

    @Builder.Default
    private BroadcastDisplayType displayType = BroadcastDisplayType.MODAL;

    private String coverImageUrl;

    private String linkUrl;

    @Builder.Default
    private Boolean forcePopup = false;

    @Builder.Default
    private Integer priority = 0;

    private String publisher;

    private String category;

    @Builder.Default
    private Instant publishTime = Instant.now();

    private Instant createdAt;

    private Instant expireTime;

}
