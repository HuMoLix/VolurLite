package com.humolix.volur.user.domain.dto;

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
    private String avatarUrl;

}
