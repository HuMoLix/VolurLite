# 核心查询性能调优与 N+1 查询专项治理施工记录

## 1. 变更背景与治理目标
在排查管理端竞赛列表（`AdminCompetitionListView.vue`）参赛队伍与人数恒为 `0队/0人` 的问题过程中，针对前后端数据传输与底层数据访问层进行深度审计，发现系统在多处核心列表与详情 API 存在严重的 N+1 懒加载查询、只读流中写库、内存全量 Stream 过滤等性能瓶颈。

本次实施针对以下 5 个关键接口完成端到端调优与缺陷修复：
1. **`GET /api/v1/admin/competitions`**：修复规模展示恒为 `0队/0人` 的严重缺陷，消除列表循环内的单条统计 N+1，并优化状态刷新为单次批量写库；
2. **`GET /api/v1/admin/competitions/{competitionId}/teams`**：消除队长（Captain）懒加载 N+1 及队伍成员计数 N+1；
3. **队伍详情查询接口**（管理端 `GET .../teams/{teamId}` 与 用户端 `GET .../teams/me`）：消除队员关联用户（User）及头像资料（UserProfile）的级联懒加载；
4. **`GET /api/v1/admin/competitions/{competitionId}/writeups`**：废除全表拉取到 JVM 内存的 Stream 过滤，将搜索条件下推至 MySQL 数据库层；
5. **`GET /api/v1/admin/users`**：消除管理端用户分页列表遍历时触发 `userProfile` 的懒加载 N+1。

---

## 2. 具体改动点与技术实现

### 2.1 管理端赛事列表（`AdminCompetitionListVO` 与 `AdminCompetitionServiceImpl`）
- **数据契约修复**：
  在 `AdminCompetitionListVO` 中补齐 `participantCount` 字段，使后端视图与前端 TS 契约（`src/types/competition/index.ts`）1:1 严格对齐。
- **批量聚合计数**：
  - 在 `CompetitionTeamRepository` 中新增 `countTeamsByCompetitionIds`，通过 `GROUP BY t.competition.id` 批量聚合当前页所有赛事的队伍总数；
  - 在 `CompetitionMemberRepository` 中新增 `countParticipantsByCompetitionIds`，通过 `GROUP BY m.competition.id` 批量聚合当前页所有赛事的有效参赛人数（排除 `NOT_PARTICIPATE`）；
  - 将查询结果转为 Map 进行 O(1) 回填，将原本每页 `1 + 2N` 次查询缩减为固定 3 次查询。
- **状态刷新批量写库**：
  在分页查出 `Page<Competition>` 后，统一过滤状态变更实体，通过 `competitionRepository.saveAll(updatedComps)` 单次批量保存，杜绝了在 `map` 流遍历中频繁触发逐条 UPDATE 行锁的问题。

### 2.2 管理端队伍列表（`AdminTeamServiceImpl`）
- **队长动态 Fetch Join**：
  在 Specification 中判断 `Long.class != query.getResultType()` 时执行 `root.fetch("captain", JoinType.LEFT)`，规避分页 Count 报错的同时在主查询中一次性查出队长信息，消除 `team.getCaptain().getUsername()` 触发的 N 次用户懒加载。
- **批量聚合队伍成员计数**：
  在 `CompetitionMemberRepository` 中新增 `countMembersByTeamIdsAndStatus`，一次性查出当前页所有队伍的审核通过队员数量并构造成 Map 回填，彻底消除循环内单条 `countByTeamIdAndStatus` 导致的 N 次额外计数查询。查询次数由 `1 + 2N`（41 次）骤降至 2 次。

### 2.3 队伍详情查询优化（`CompetitionTeamRepository` & `CompetitionMemberRepository`）
- **队伍与队长/赛事深度关联**：
  在 `CompetitionTeamRepository` 中定义 `findByIdWithCompetitionAndCaptain`，使用 `JOIN FETCH` 一次性查出队伍及其归属赛事和队长信息。
- **队员与用户/Profile 深度关联**：
  在 `CompetitionMemberRepository` 中定义 `findByTeamIdWithUserAndProfile`，通过 `LEFT JOIN FETCH m.user u LEFT JOIN FETCH u.userProfile` 预加载成员对应的账号及头像资料（对齐 `User` 实体的 `userProfile` 属性名）。
- **服务层应用**：
  在 `AdminTeamServiceImpl.getTeam` 与 `CompetitionTeamServiceImpl.getMyTeam` 中全面替换普通查询，避免了 MapStruct 转换每个队员时触发的两次懒加载（用户表 + 用户档案表），单次队伍详情查询从十几次 SQL 缩减至精准的 2 次 SQL。

### 2.4 管理端 Writeup 列表查询（`AdminCompetitionWriteupController`）
- **搜索条件下推数据库**：
  在 `CompetitionWriteupRepository` 中编写 `@Query` 参数化查询方法 `searchByCompetitionIdAndKeyword`，将队伍名、用户名、文件名的 `LIKE` 模糊匹配下推至数据库执行。
- **移除内存 Stream 过滤**：
  废除全表拉取至 JVM 堆内存后执行 `stream().filter(...)` 的反模式，显著降低 GC 压力，保护服务器堆内存稳定。

### 2.5 管理端用户列表查询（`AdminUserServiceImpl`）
- **动态 Fetch Join Profile**：
  在 `AdminUserServiceImpl.getUserList` 的 Specification 构建中，对非 count 类主查询添加 `root.fetch("userProfile", JoinType.LEFT)`。
- **消除单条 Profile 查询**：
  在分页主查询时完成关联预加载，MapStruct 映射 `user.getUserProfile()` 时直接命中 Persistence Context，彻底消除了每页 20 次额外的 `user_profile` 表单查。

---

## 3. 影响范围与安全验证说明

- **后端测试**：
  执行 `mvn test -pl volur-competition,volur-user`，所有 36 个测试用例全部一次性无警告通过（36 Tests Run, 0 Failures, 0 Errors）。
- **前端兼容性**：
  前端视图 `AdminCompetitionListView.vue` 与 `src/types/competition/index.ts` 契约完全吻合，无需调整前端即可直接恢复正常的队伍数和人数呈现（非 `0队/0人`）。
- **数据一致性与安全性**：
  所有变更均采用标准参数化 JPQL / Specification 动态条件，严格避免 SQL 注入隐患；批量聚合统计逻辑均在事务边界内安全执行。
