package com.humolix.volur.security.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * VLIF (Volur Login Interface Framework) 流程会话初始化响应视图 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowInitVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String dsh;

    private String tl;

    @Builder.Default
    private String flowName = "VlifWebLogin";

    @Builder.Default
    private String flowEntry = "WebServiceLogin";

    @Builder.Default
    private Integer cid = 0;

    private String fp;

    private Long serverTime;

}
