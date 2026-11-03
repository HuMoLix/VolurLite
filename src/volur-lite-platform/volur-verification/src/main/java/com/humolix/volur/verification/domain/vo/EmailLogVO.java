package com.humolix.volur.verification.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLogVO {

    private Long id;

    private String messageId;

    private String recipient;

    private String subject;

    private String content;

    private String type;

    private String status;

    private String errorMessage;

    private String sender;

    private Long operatorId;

    private String operatorIp;

    private Instant createdAt;

}
