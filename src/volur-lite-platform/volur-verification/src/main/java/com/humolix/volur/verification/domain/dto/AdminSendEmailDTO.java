package com.humolix.volur.verification.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSendEmailDTO {

    @NotBlank(message = "收件人邮箱不能为空")
    @Email(message = "收件人邮箱格式不正确")
    @Trimmed(toLowerCase = true)
    private String recipient;

    @NotBlank(message = "邮件主题不能为空")
    @Size(max = 256, message = "邮件主题长度不能超过256个字符")
    @Trimmed
    private String subject;

    @NotBlank(message = "邮件内容不能为空")
    private String content;

    private Boolean html = true;

}
