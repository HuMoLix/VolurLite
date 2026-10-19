package com.humolix.volur.framework.common.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public final class PageSortUtil {

    private PageSortUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    private static final Cache<Class<?>, Set<String>> ENTITY_VALID_FIELDS_CACHE = Caffeine.newBuilder()
            .maximumSize(500)
            .build();
    private static final Set<String> DESC_KEYWORDS = Set.of("desc", "descending");

    public static Pageable createPageable(Integer page, Integer size) {
        return createPageable(page, size, null, null, null, null, null);
    }

    public static Pageable createPageable(Integer page, Integer size, Class<?> entityClass,
                                          String sortField, String sortOrder,
                                          String defaultField, Sort.Direction defaultDirection) {
        int pageNum = Math.max(0, (page == null ? 0 : page - 1));
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 200);

        Sort sort = buildSort(entityClass, sortField, sortOrder, defaultField, defaultDirection);
        return PageRequest.of(pageNum, pageSize, sort);
    }

    public static Sort buildSort(Class<?> entityClass, String sortField, String sortOrder,
                                 String defaultField, Sort.Direction defaultDirection) {

        Sort.Direction fallbackDirection = defaultDirection != null ? defaultDirection : Sort.Direction.DESC;

        if (!StringUtils.hasText(sortField)) {
            return StringUtils.hasText(defaultField)
                    ? Sort.by(fallbackDirection, defaultField)
                    : Sort.unsorted();
        }

        Sort.Direction direction = (StringUtils.hasText(sortOrder) && DESC_KEYWORDS.contains(sortOrder.trim().toLowerCase(Locale.ROOT)))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        List<Sort.Order> orders = new ArrayList<>();
        String[] fields = sortField.split(",");

        for (String field : fields) {
            String trimmedField = field.trim();
            if (isValidField(entityClass, trimmedField)) {
                orders.add(new Sort.Order(direction, trimmedField));
            }
        }

        if (orders.isEmpty()) {
            return StringUtils.hasText(defaultField)
                    ? Sort.by(fallbackDirection, defaultField)
                    : Sort.unsorted();
        }

        return Sort.by(orders);
    }

    public static boolean isValidField(Class<?> clazz, String fieldPath) {
        if (clazz == null || !StringUtils.hasText(fieldPath)) {
            return false;
        }

        Set<String> validFields = ENTITY_VALID_FIELDS_CACHE.get(clazz, PageSortUtil::extractValidFields);
        return validFields != null && validFields.contains(fieldPath);
    }

    private static Set<String> extractValidFields(Class<?> clazz) {
        Set<String> fieldPaths = new HashSet<>();
        collectFields(clazz, "", fieldPaths, 0);
        return fieldPaths;
    }

    private static void collectFields(Class<?> clazz, String prefix, Set<String> acc, int depth) {
        if (clazz == null || clazz == Object.class || depth > 2) {
            return;
        }

        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();

                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || isJpaTransient(field)) {
                    continue;
                }

                Class<?> fieldType = field.getType();
                if (Collection.class.isAssignableFrom(fieldType) || Map.class.isAssignableFrom(fieldType) || fieldType.isArray()) {
                    continue;
                }

                String fullPath = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
                acc.add(fullPath);

                if (!fieldType.isPrimitive() && !fieldType.getName().startsWith("java.")) {
                    collectFields(fieldType, fullPath, acc, depth + 1);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    private static boolean isJpaTransient(Field field) {
        return Arrays.stream(field.getAnnotations())
                .anyMatch(a -> "jakarta.persistence.Transient".equals(a.annotationType().getName())
                        || "javax.persistence.Transient".equals(a.annotationType().getName()));
    }

}
