package com.humolix.volur.security.session.domain;

import com.humolix.volur.security.flow.domain.Flow;
import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.session.RequestHeader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String dsh;

    private RequestHeader requestHeader;

    private RequestDeviceParams requestDeviceParams;

    private String riskLevel;

    private Flow flow;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Builder.Default
    private Long createdAt = System.currentTimeMillis();

    @Builder.Default
    private Long updatedAt = System.currentTimeMillis();

}
