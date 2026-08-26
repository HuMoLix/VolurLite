package com.humolix.volur.framework.common.api;

import java.util.List;

public record CursorResponse<T>(
        List<T> list,
        String nextPageToken,
        boolean hasMore
) {

    public static <T> CursorResponse<T> empty() {
        return new CursorResponse<>(List.of(), null, false);
    }

    public static <T> CursorResponse<T> of(List<T> list, String nextPageToken, boolean hasMore) {
        return new CursorResponse<>(list, nextPageToken, hasMore);
    }

}
