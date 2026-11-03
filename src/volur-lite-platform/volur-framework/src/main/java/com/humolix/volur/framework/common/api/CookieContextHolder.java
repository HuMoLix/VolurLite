package com.humolix.volur.framework.common.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class CookieContextHolder {

    private static final ThreadLocal<List<ApiResponseCookie>> HOLDER =
            ThreadLocal.withInitial(ArrayList::new);

    private CookieContextHolder() {
    }

    public static void setCookie(ApiResponseCookie cookie) {
        if (cookie != null) {
            HOLDER.get().add(cookie);
        }
    }

    public static void setCookies(Collection<ApiResponseCookie> cookies) {
        if (cookies != null && !cookies.isEmpty()) {
            HOLDER.get().addAll(cookies);
        }
    }

    public static List<ApiResponseCookie> pollCookies() {
        List<ApiResponseCookie> cookies = HOLDER.get();
        if (cookies == null || cookies.isEmpty()) {
            return Collections.emptyList();
        }
        List<ApiResponseCookie> copy = new ArrayList<>(cookies);
        HOLDER.remove();
        return copy;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
