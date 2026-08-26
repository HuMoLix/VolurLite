package com.humolix.volur.notification.domain.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeMessageBO {

    private String eventId;

    private String eventType;

    private Long targetUserId;

    private String topic;

    private Object payload;

    @Builder.Default
    private Instant timestamp = Instant.now();

}
