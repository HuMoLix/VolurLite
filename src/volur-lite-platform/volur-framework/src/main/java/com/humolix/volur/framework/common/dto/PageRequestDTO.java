package com.humolix.volur.framework.common.dto;

import com.humolix.volur.framework.common.util.PageSortUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "基础分页请求参数 DTO")
public class PageRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Builder.Default
    @Schema(description = "当前页码（从 1 开始）", example = "1", defaultValue = "1")
    private int page = 1;

    @Builder.Default
    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    private int size = 10;

    @Schema(description = "排序字段", example = "createdAt")
    private String sortField;

    @Builder.Default
    @Schema(description = "排序方向 (ASC / DESC)", example = "DESC", defaultValue = "DESC")
    private String sortOrder = "DESC";

    public Pageable toPageable(Class<?> entityClass, String defaultSortField) {
        return PageSortUtil.createPageable(
                this.page,
                this.size,
                entityClass,
                this.sortField,
                this.sortOrder,
                defaultSortField,
                Sort.Direction.DESC
        );
    }

    public Pageable toPageable(Class<?> entityClass, String defaultSortField, Sort.Direction defaultDirection) {
        return PageSortUtil.createPageable(
                this.page,
                this.size,
                entityClass,
                this.sortField,
                this.sortOrder,
                defaultSortField,
                defaultDirection
        );
    }

    public Pageable toPageable() {
        return PageSortUtil.createPageable(this.page, this.size);
    }

}
