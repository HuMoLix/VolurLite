package com.humolix.volur.user.domain.dto;

import com.humolix.volur.framework.common.annotation.Trimmed;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAvatarRequestDTO implements Serializable {

    @NotBlank(message = "头像URL不能为空")
    @Trimmed
    private String avatarUrl;

}
