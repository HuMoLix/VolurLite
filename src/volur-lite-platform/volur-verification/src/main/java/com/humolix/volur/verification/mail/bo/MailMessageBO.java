package com.humolix.volur.verification.mail.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailMessageBO {

    /**
     * 发件人邮箱地址 (RFC 5321 / RFC 5322 addr-spec，如 service@volur.local)
     */
    private String from;

    /**
     * 发件人显示昵称 (Personal display name，如 Volur Lite 安全演练平台)
     */
    private String senderName;

    /**
     * 邮件全局唯一追踪 ID (RFC 5322 Message-ID)
     */
    private String messageId;

    private String to;

    private String subject;

    private String content;

    private String fallbackContent;

    @Builder.Default
    private boolean html = true;

    private String type;

    private Long operatorId;

    private String operatorIp;
}
