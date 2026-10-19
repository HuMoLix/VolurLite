package com.humolix.volur.verification.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestSmtpRequestDTO {

    @NotBlank(message = "测试收件人邮箱不能为空")
    @Email(message = "测试收件人邮箱格式不正确")
    @Trimmed(toLowerCase = true)
    private String to;

}
