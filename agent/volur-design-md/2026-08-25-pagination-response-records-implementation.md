# Volur Lite 分页响应模型与标准化 Record (PageResponse/CursorResponse/SliceResponse) 设计与施工档案

- **执行日期**：2026-08-25
- **施工模块**：`volur-framework`, `volur-competition`
- **文档类型**：通用 API 分页响应架构与重构归档

---

## 1. 业务与架构设计背景

在分布式与微服务架构中，直接在 Controller 层向前端返回 Spring Data 原生的 `Page<T>` 存在以下问题：
1. **序列化冗余**：Spring Data 原生 `PageImpl` 序列化包含大量内部属性（`pageable`, `sort`, `first`, `last`, `numberOfElements`, `empty` 等），导致网络数据包膨胀；
2. **前后端契约不纯粹**：前端分页通常仅需要核心的四个字段：数据列表 `list`、总记录数 `total`、总页数 `totalPages` 以及当前页码 `currentPage`；
3. **框架强耦合**：接口响应模型直接暴露了底层持久层实现框架的内部类。

为了统一全平台分页契约，在 [`volur-framework`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/api/) 中引入标准轻量级 Record：
- [`PageResponse<T>`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/api/PageResponse.java)
- [`CursorResponse<T>`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/api/CursorResponse.java)
- [`SliceResponse<T>`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/api/SliceResponse.java)

并将 [`CompetitionController.listCompetitions`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionController.java#L20-L26) 重构适配为 `PageResponse`。

---

## 2. 核心分页响应模型定义

### 2.1 `PageResponse<T>` (标准传统分页)
```java
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
```

### 2.2 `CursorResponse<T>` (游标/流式/无限滚动分页)
```java
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
```

### 2.3 `SliceResponse<T>` (分片/无总数切片分页)
```java
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
```

---

## 3. 修改影响清单

1. **[`volur-framework`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/api/)**：
   - 新增 [`PageResponse.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/api/PageResponse.java)
   - 新增 [`CursorResponse.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/api/CursorResponse.java)
   - 新增 [`SliceResponse.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/api/SliceResponse.java)
2. **[`CompetitionController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionController.java)**：
   - 方法签名更新为：`public ApiResponse<PageResponse<CompetitionSummaryVO>> listCompetitions(...)`
3. **[`CompetitionService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionService.java) & [`CompetitionServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionServiceImpl.java)**：
   - 返回类型统一调整为 `PageResponse<CompetitionSummaryVO>`，并在 Service 层通过 `PageResponse.of(pageResult)` 完成封装。

---

## 4. 构建与验证结果

执行 Maven 编译测试：
```bash
mvn clean test-compile
```
输出结果：
- 12 个模块全部编译通过：**BUILD SUCCESS**
- 零编译错误，零依赖缺失。
