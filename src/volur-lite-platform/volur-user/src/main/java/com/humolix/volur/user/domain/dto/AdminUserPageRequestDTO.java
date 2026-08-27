package com.humolix.volur.user.domain.dto;

import com.humolix.volur.framework.common.dto.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端用户分页筛选请求 DTO")
public class AdminUserPageRequestDTO extends PageRequestDTO {

    @Schema(description = "检索关键字（支持模糊匹配用户名/邮箱/所属机构）", example = "admin")
    private String keyword;

    @Schema(description = "用户角色（USER/ADMIN/SUPER_ADMIN 等）", example = "USER")
    private String role;

    @Schema(description = "账号状态（true: 启用, false: 禁用）", example = "true")
    private Boolean status;

    @Schema(description = "注册起始时间", example = "2026-01-01T00:00:00Z")
    private Instant startTime;

    @Schema(description = "注册截止时间", example = "2026-12-31T23:59:59Z")
    private Instant endTime;

}
