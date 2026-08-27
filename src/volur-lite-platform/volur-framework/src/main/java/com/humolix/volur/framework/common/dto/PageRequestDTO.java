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

    /**
     * 兼容前端或旧接口传入的 sortBy 命名
     */
    public void setSortBy(String sortBy) {
        if (StringUtils.hasText(sortBy)) {
            this.sortField = sortBy;
        }
    }

    public String getSortBy() {
        return this.sortField;
    }

    /**
     * 构建 Spring Data Pageable 对象，默认降序
     *
     * @param entityClass      实体类型（用于校验排序字段合法性，防止 SQL 注入）
     * @param defaultSortField 默认排序字段
     * @return Pageable 实例
     */
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

    /**
     * 构建 Spring Data Pageable 对象，支持自定义默认排序方向
     *
     * @param entityClass      实体类型（用于校验排序字段合法性，防止 SQL 注入）
     * @param defaultSortField 默认排序字段
     * @param defaultDirection 默认排序方向
     * @return Pageable 实例
     */
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

    /**
     * 构建基础 Spring Data Pageable 对象（无排序）
     *
     * @return Pageable 实例
     */
    public Pageable toPageable() {
        return PageSortUtil.createPageable(this.page, this.size);
    }

}
