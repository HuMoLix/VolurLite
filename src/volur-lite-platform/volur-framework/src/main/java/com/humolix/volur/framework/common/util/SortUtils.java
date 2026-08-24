package com.humolix.volur.framework.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SortUtils {

    private SortUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static final Map<Class<?>, Map<String, Class<?>>> ENTITY_FIELDS_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Boolean>> VALID_FIELDS_RESULT_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> DESC_KEYWORDS = Set.of("desc", "descending");

    public static Pageable createPageable(Integer page, Integer size) {
        int pageNum = Math.max(0, (page == null ? 0 : page - 1));
        int pageSize = (size == null || size < 1) ? 20 : Math.min(size, 100);
        return PageRequest.of(pageNum, pageSize);
    }

    public static Pageable createPageable(Integer page, Integer size, Class<?> entityClass,
                                          String sortField, String sortOrder,
                                          String defaultField, Sort.Direction defaultDirection) {
        int pageNum = Math.max(0, (page == null ? 0 : page - 1));
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 100);

        Sort sort = buildSort(entityClass, sortField, sortOrder, defaultField, defaultDirection);
        return PageRequest.of(pageNum, pageSize, sort);
    }

    public static Sort buildSort(Class<?> entityClass, String sortField, String sortOrder,
                                 String defaultField, Sort.Direction defaultDirection) {

        if (!StringUtils.hasText(sortField) || !isValidField(entityClass, sortField)) {
            return Sort.by(defaultDirection, defaultField);
        }

        Sort.Direction direction = (StringUtils.hasText(sortOrder) && DESC_KEYWORDS.contains(sortOrder.toLowerCase(Locale.ROOT)))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return Sort.by(direction, sortField);
    }

    private static boolean isValidField(Class<?> clazz, String fieldName) {
        if (clazz == null || !StringUtils.hasText(fieldName)) {
            return false;
        }

        var classCache = VALID_FIELDS_RESULT_CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
        Boolean cachedResult = classCache.get(fieldName);
        if (cachedResult != null) {
            return cachedResult;
        }

        boolean isValid = validateFieldPath(clazz, fieldName);
        classCache.put(fieldName, isValid);
        return isValid;
    }

    private static boolean validateFieldPath(Class<?> clazz, String fieldName) {
        if (!fieldName.contains(".")) {
            return getFieldsMap(clazz).containsKey(fieldName);
        }
        String[] parts = fieldName.split("\\.");
        Class<?> currentClass = clazz;

        for (String part : parts) {
            var fieldsMap = getFieldsMap(currentClass);
            Class<?> fieldType = fieldsMap.get(part);
            if (fieldType == null) {
                return false;
            }
            currentClass = fieldType;
        }
        return true;
    }

    private static Map<String, Class<?>> getFieldsMap(Class<?> clazz) {
        return ENTITY_FIELDS_CACHE.computeIfAbsent(clazz, k -> {
            Map<String, Class<?>> fieldsMap = new ConcurrentHashMap<>();
            Class<?> currentClass = clazz;
            while (currentClass != null && currentClass != Object.class) {
                for (Field field : currentClass.getDeclaredFields()) {
                    fieldsMap.putIfAbsent(field.getName(), field.getType());
                }
                currentClass = currentClass.getSuperclass();
            }
            return fieldsMap;
        });
    }

}
