# Volur Lite 分页响应模型与业务场景选型规范落地归档

- **执行日期**：2026-08-25
- **施工模块**：`agent/volur-design-skills` (`Rules.md`), `GEMINI.md`
- **文档类型**：编码规范与 AI 指令集升级归档

---

## 1. 规范更新背景

在分布式安全平台架构演进中，针对不同业务场景的列表查询，盲目套用传统分页会导致性能与用户体验瓶颈：
- **大数据量 `COUNT(*)` 性能黑洞**：传统分页必须执行总量统计，在百万级表中严重拖慢响应速度；
- **流式信息流数据漂移**：站内信、实时动态流若使用普通 `offset` 分页，随着新数据写入会导致翻页重复与遗漏；
- **底层框架泄露**：直接对外暴露 Spring Data `Page<T>` 会引入冗余内部字段。

为此，在 [`agent/volur-design-skills/Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md) 与根规范指令 [`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md) 中正式确立**业务驱动的分页模型前置分析与选型约束**。

---

## 2. 核心约束点与决策树

### 2.1 三大分页模型选型依据
1. **标准传统分页模型 (`PageResponse<T>`)**：
   - 字段：`list`, `total`, `totalPages`, `currentPage`
   - 适用：管理端后台表格、强需总数与跳页导航的中小型数据集业务。
2. **游标 / 流式无限滚动分页模型 (`CursorResponse<T>`)**：
   - 字段：`list`, `nextPageToken`, `hasMore`
   - 适用：站内信通知列表、实时动态流、操作日志等无限滚动或高频写入场景，基于唯一索引游标定位，消除数据漂移与深分页性能劣化。
3. **无总数切片分页模型 (`SliceResponse<T>`)**：
   - 字段：`list`, `hasNext`, `hasPrevious`, `currentPage`, `pageSize`
   - 适用：海量公开题库、广场列表等高并发顺序浏览场景，仅做下一页探测，免除昂贵的 `COUNT(*)` 性能开销。

### 2.2 选型决策流程
所有涉及列表分页的业务开发，必须严格遵循以下决策链：
- **分析业务交互形态**（表格跳页 vs 瀑布流滚动 vs 顺序浏览）；
- **评估数据规模与并发压力**；
- **根据业务结论精准匹配 `PageResponse` / `CursorResponse` / `SliceResponse`**。

---

## 3. 文档修改清单

1. **[`agent/volur-design-skills/Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md)**：
   - 新增第 8 节《分页响应模型与业务场景选型规范 (Pagination Models & Selection Guidelines)》；
   - 包含设计原则、模型对比、Mermaid 决策流程图、选型速查矩阵、反模式与推荐实践代码。
2. **[`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md)**：
   - 新增第 1.8 节《分页响应模型与业务选型规范 (Pagination Models & Selection)》；
   - 在第 3 节 AI 行为指引中补充“生成与重构分页代码时先做业务分析并选择合适分页模型”的硬性约束。
