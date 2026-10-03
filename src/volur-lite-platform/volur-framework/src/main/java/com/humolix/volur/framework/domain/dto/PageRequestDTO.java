package com.humolix.volur.framework.domain.dto;

import com.humolix.volur.framework.common.util.PageSortUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;

    private String sortField;

    @Builder.Default
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
