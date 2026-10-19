package com.humolix.volur.framework.common.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class SortUtils {

    private SortUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Pageable createPageable(Integer page, Integer size) {
        return PageSortUtil.createPageable(page, size);
    }

    public static Pageable createPageable(Integer page, Integer size, Class<?> entityClass,
                                          String sortField, String sortOrder,
                                          String defaultField, Sort.Direction defaultDirection) {
        return PageSortUtil.createPageable(page, size, entityClass, sortField, sortOrder, defaultField, defaultDirection);
    }

    public static Sort buildSort(Class<?> entityClass, String sortField, String sortOrder,
                                 String defaultField, Sort.Direction defaultDirection) {
        return PageSortUtil.buildSort(entityClass, sortField, sortOrder, defaultField, defaultDirection);
    }

    public static boolean isValidField(Class<?> clazz, String fieldName) {
        return PageSortUtil.isValidField(clazz, fieldName);
    }

}
