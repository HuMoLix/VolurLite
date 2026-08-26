package com.humolix.volur.notification.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.humolix.volur.notification.domain.enums.BroadcastDisplayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Builder.Default
    private Instant publishTime = Instant.now();

    private Instant expireTime;

}
