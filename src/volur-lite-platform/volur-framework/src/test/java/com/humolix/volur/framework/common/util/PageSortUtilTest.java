package com.humolix.volur.framework.common.util;

import jakarta.persistence.Transient;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

class PageSortUtilTest {

    @Data
    static class SampleNestedEntity {
        private String detailName;
        private Integer level;
    }

    @Data
    static class SampleEntity {
        private Long id;
        private String username;
        private Integer score;
        private SampleNestedEntity nested;

        @Transient
        private String transientField;
    }

    @Test
    @DisplayName("Test basic Pageable creation with defaults")
    void testBasicPageable() {
        Pageable p1 = PageSortUtil.createPageable(null, null);
        assertEquals(0, p1.getPageNumber());
        assertEquals(10, p1.getPageSize());

        Pageable p2 = PageSortUtil.createPageable(2, 25);
        assertEquals(1, p2.getPageNumber());
        assertEquals(25, p2.getPageSize());

        // Max size bounded at 200
        Pageable p3 = PageSortUtil.createPageable(1, 500);
        assertEquals(200, p3.getPageSize());
    }

    @Test
    @DisplayName("Test valid field sorting and SQL injection protection")
    void testValidFieldSort() {
        Sort sort = PageSortUtil.buildSort(SampleEntity.class, "username", "desc", "id", Sort.Direction.ASC);
        Sort.Order order = sort.getOrderFor("username");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());

        // Nested valid field
        Sort nestedSort = PageSortUtil.buildSort(SampleEntity.class, "nested.detailName", "ASC", "id", Sort.Direction.DESC);
        assertNotNull(nestedSort.getOrderFor("nested.detailName"));

        // SQL injection / invalid field fallback to defaultField
        Sort invalidSort = PageSortUtil.buildSort(SampleEntity.class, "sleep(5)--", "desc", "id", Sort.Direction.DESC);
        assertNull(invalidSort.getOrderFor("sleep(5)--"));
        assertNotNull(invalidSort.getOrderFor("id"));

        // Transient field should be excluded
        Sort transientSort = PageSortUtil.buildSort(SampleEntity.class, "transientField", "desc", "id", Sort.Direction.DESC);
        assertNull(transientSort.getOrderFor("transientField"));
        assertNotNull(transientSort.getOrderFor("id"));
    }

    @Test
    @DisplayName("Test multi-field sorting")
    void testMultiFieldSort() {
        Sort sort = PageSortUtil.buildSort(SampleEntity.class, "username,score", "desc", "id", Sort.Direction.ASC);
        assertNotNull(sort.getOrderFor("username"));
        assertNotNull(sort.getOrderFor("score"));
    }

}
