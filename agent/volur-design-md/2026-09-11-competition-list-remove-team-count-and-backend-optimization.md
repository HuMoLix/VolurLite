# 竞赛列表视图与接口移除报名队伍（人数）审计说明

## 1. 变更背景与需求说明
根据用户的最新指令：
> 「不用显示报名队伍了。同时更新后端逻辑，在 List 的api不用返回报名人数了。」

在先前的实现中：
1. 前台公开竞赛列表视图 [`CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionListView.vue) 卡片信息栏包含“报名队伍: X 支战队”的元数据展示；
2. 后端公开竞赛分页列表接口（`GET /api/v1/competitions`）在 `CompetitionServiceImpl.listCompetitions` 中，对分页查出的每一项竞赛均执行了一次 `teamRepository.countByCompetitionIdAndStatus(comp.getId(), TeamStatus.AUDIT_PASSED)` 计数查询，存在潜在的 N+1 数据库聚合查询开销。

为简化列表页展示维度、提升前台竞赛列表在高并发场景下的检索性能，本次对前后端进行了同步精简与优化。

---

## 2. 核心改动点

### 2.1 前端：移除报名队伍相关展示与依赖
- **模板精简**：
  - 移除了卡片元数据栏中的 `<!-- 报名队伍数量 -->` 项及其所属的 `meta-item` 节点；
  - 列表项元数据栏当前聚焦于核心维度：“比赛时间”与“报名时间”。
- **脚本与资源精简**：
  - 移除了 `@vicons/ionicons5` 中仅用于该项的 `PeopleOutline` 图标导入；
  - 清理了离线 Mock 降级数据中无用的 `teamCount` 属性。
- **TypeScript 实体同步**：
  - 在 [`src/types/competition/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/competition/index.ts) 中，将 `CompetitionSummaryVO` 中的 `teamCount` 与 `participantCount` 移除，与后端保持严格 1:1 镜像对齐。

### 2.2 后端：移除 List API 的队伍数聚合统计与 VO 字段
- **服务层性能优化 (`CompetitionServiceImpl.java`)**：
  - 将原有的逐项循环查询：
    ```java
    Page<CompetitionSummaryVO> pageResult = competitionRepository.findAll(spec, pageable).map(comp -> {
        CompetitionSummaryVO vo = competitionMapper.toSummaryVO(comp);
        long teamCount = teamRepository.countByCompetitionIdAndStatus(comp.getId(), TeamStatus.AUDIT_PASSED);
        vo.setTeamCount(teamCount);
        return vo;
    });
    ```
    重构成高性能直接实体映射：
    ```java
    Page<CompetitionSummaryVO> pageResult = competitionRepository.findAll(spec, pageable)
            .map(competitionMapper::toSummaryVO);
    ```
  - **消除 N+1 查询**：不再对列表中的每场比赛统计战队数，显著降低数据库 I/O 开销与响应延迟。
- **领域视图对象精简 (`CompetitionSummaryVO.java`)**：
  - 彻底移除了 `private Long teamCount;` 字段，公开列表响应不再返回该冗余字段。
- **单元测试对齐 (`CompetitionControllerTest.java`)**：
  - 移除测试用例中的 `.teamCount(5L)` 链式调用，保证全部单元测试执行通过。

---

## 3. 构建与验证结果

1. **后端工程编译与单元测试**：
   - 执行 `mvn test-compile -pl volur-competition`：`BUILD SUCCESS`；
   - 执行 `mvn test -pl volur-competition`：`CompetitionControllerTest` 全部通过（Tests run: 1, Failures: 0, Errors: 0, Skipped: 0）。
2. **前端工程编译与类型检查**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：TypeScript 静态检查与打包构建 **100% 通过（0 错误，0 告警）**；
   - 整体构建在 4.04s 内完成。

---

## 4. 影响范围与审计结论

- **影响文件清单**：
  1. [`src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionServiceImpl.java)
  2. [`src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/CompetitionSummaryVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/CompetitionSummaryVO.java)
  3. [`src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/controller/CompetitionControllerTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/controller/CompetitionControllerTest.java)
  4. [`src/volur-lite-view/src/views/competition/CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionListView.vue)
  5. [`src/volur-lite-view/src/types/competition/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/competition/index.ts)
- **审计结论**：
  前端列表卡片已不再展示报名队伍项，后端公开列表接口已彻底剥离队伍/人数计数开销，前后端 DTO/VO 保持 1:1 严格对齐，全量测试与构建均通过。
