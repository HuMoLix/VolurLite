package com.humolix.volur.framework.common.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> list,
        long total,
        int totalPages,
        int currentPage
) {

    public PageResponse(Page<T> page) {
        this(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber() + 1
        );
    }

    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(List.of(), 0L, 0, 1);
    }

    public static <T> PageResponse<T> of(List<T> list, long total, int totalPages, int currentPage) {
        return new PageResponse<>(list, total, totalPages, currentPage);
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page);
    }

}
