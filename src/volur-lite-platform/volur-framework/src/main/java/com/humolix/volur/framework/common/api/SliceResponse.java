package com.humolix.volur.framework.common.api;

import org.springframework.data.domain.Slice;

import java.util.List;

public record SliceResponse<T>(
        List<T> list,
        boolean hasNext,
        boolean hasPrevious,
        int currentPage,
        int pageSize
) {

    public SliceResponse(Slice<T> slice) {
        this(
                slice.getContent(),
                slice.hasNext(),
                slice.hasPrevious(),
                slice.getNumber() + 1,
                slice.getSize()
        );
    }

    public static <T> SliceResponse<T> empty() {
        return new SliceResponse<>(List.of(), false, false, 1, 10);
    }

    public static <T> SliceResponse<T> of(List<T> list, boolean hasNext, boolean hasPrevious, int currentPage, int pageSize) {
        return new SliceResponse<>(list, hasNext, hasPrevious, currentPage, pageSize);
    }

    public static <T> SliceResponse<T> of(Slice<T> slice) {
        return new SliceResponse<>(slice);
    }

}
