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

    private String from;

    private String to;

    private String subject;

    private String content;

    private String fallbackContent;

    @Builder.Default
    private boolean html = true;

}
