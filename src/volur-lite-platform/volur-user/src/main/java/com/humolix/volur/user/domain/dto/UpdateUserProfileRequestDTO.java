package com.humolix.volur.user.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import com.humolix.volur.user.domain.enums.Gender;
import com.humolix.volur.user.domain.enums.PrivacyType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequestDTO implements Serializable {

    private Gender gender;

    private LocalDate birthday;

    @Size(max = 128, message = "学校/组织机构长度不能超过128个字符")
    @Trimmed
    private String org;

    @Size(max = 128, message = "专业/方向长度不能超过128个字符")
    @Trimmed
    private String major;

    @Size(max = 500, message = "个人简介长度不能超过500个字符")
    @Trimmed
    private String bio;

    private PrivacyType emailPrivacy;

    private PrivacyType phonePrivacy;

    private PrivacyType orgPrivacy;

}
