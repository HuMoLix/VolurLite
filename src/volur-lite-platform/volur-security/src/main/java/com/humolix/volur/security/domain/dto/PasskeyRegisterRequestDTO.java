package com.humolix.volur.security.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyRegisterRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String label;

    @NotNull
    private Map<String, Object> credential;

}
