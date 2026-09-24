package com.humolix.volur.framework.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 当前请求用户上下文快照 (Schema)
 * <p>
 * 统一聚合了当前 HTTP 请求或任务执行时的身份主体 (ID, 用户名, 角色列表) 与网络环境 (实时 IP, 认证状态)。
 * 作为全系统执行上下文单一真实数据源 (Single Source of Truth)，保证在任意时刻调用均不为 null。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserSchema implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Builder.Default
    private Long id = 0L;

    @Builder.Default
    private String username = "anonymous";

    @Builder.Default
    private String ip = "127.0.0.1";

    @Builder.Default
    private boolean authenticated = false;

    @Builder.Default
    private List<String> roles = List.of();

}
