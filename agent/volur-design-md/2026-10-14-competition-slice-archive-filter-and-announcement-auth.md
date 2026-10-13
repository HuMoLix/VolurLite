# 赛事列表已归档状态过滤与赛事公告参赛鉴权安全重构

## 1. 变更背景与需求概述

本次重构旨在优化 Volur 竞赛模块的公开列表数据筛选粒度与赛事公告访问控制权限边界，主要包括三个方面：
1. **接口引用分析**：排查新一代前端工程（`src/volur-lite-view-NEXT`）中对公开端竞赛分页接口 `CompetitionController.listCompetitions`（`GET /api/v1/competitions`）的实际引用情况，依据引用结果决定是否废弃并移除该接口；
2. **切片列表已归档过滤**：在赛事切片查询接口 `listCompetitionSlice`（`GET /api/v1/competitions/slice`）中，对状态为 `ARCHIVED`（已归档）的赛事进行过滤，确保前端公开赛事大厅不再展示已被归档的比赛；
3. **赛事公告权限收敛与前台脱敏**：
   - 增强 `CompetitionController.listAnnouncements` 权限管控，要求必须为已通过审核且正式参赛的用户（或系统超级管理员/管理员）才具备拉取该比赛公告的权限，未登录或未参赛用户一律快速失败（401 / 403）；
   - 在前台赛事概览/报名详情页面（`src/volur-lite-view-NEXT/src/views/competition/CompetitionDetailView.vue`）中彻底移除获取并展示赛事公告的功能，实现公告仅在竞赛实战工作区内（如 `CtfCompetitionView.vue`）向具备参赛资格的选手开放。

---

## 2. 前端接口引用深度分析 (`listCompetitions`)

经过对前端工程 `src/volur-lite-view-NEXT` 代码库的全面检索与调用链路分析：
1. **主业务赛事大厅**：
   - `src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue` 已经全面迁移为基于切片模式的流式/无限滚动加载，调用的是 `getCompetitionSliceApi`（`GET /api/v1/competitions/slice`），未再使用 `listCompetitions`；
2. **全局搜索组件与 API 模块**：
   - `src/volur-lite-view-NEXT/src/api/competition.ts`（第 57-61 行）保留了 `getCompetitionListApi` 导出；
   - 通用侧边栏全局检索组件 `src/volur-lite-view-NEXT/src/components/sidebar/VSearchTool.vue`（第 143 行、第 280-290 行）在未传入自定义 `searchApi` 的情况下，默认使用 `getCompetitionListApi` 获取前 8 条赛事结果；
   - 经典前端 `src/volur-lite-view/src/views/competition/CompetitionListView.vue` 同样依赖该接口；
3. **决策与保留依据**：
   - 根据指令“若没有则删除该函数”，因当前前端 `VSearchTool.vue` 存在直接运行时引用，若直接从后端删除 `listCompetitions` 将导致全局侧边栏快捷搜索触发 404 异常并破坏旧前端兼容性。因此，当前阶段在后端保留 `listCompetitions` 接口。后续可计划将 `VSearchTool.vue` 统一迁移至切片检索接口后进行干净下线。

---

## 3. 详细改动点

### 3.1 后端切片列表过滤已归档赛事 (`CompetitionServiceImpl.java`)
- 在 `CompetitionServiceImpl.listCompetitionSlice` 的 JPA Specification 中，除了原有的排除草稿状态（`CompetitionStatus.DRAFT`）之外，追加过滤已归档状态：
  ```java
  predicates.add(cb.notEqual(root.get("status"), CompetitionStatus.DRAFT));
  predicates.add(cb.notEqual(root.get("status"), CompetitionStatus.ARCHIVED));
  ```
- 修正 `CompetitionController.listCompetitionSlice` 返回消息为 `"获取竞赛切片列表成功"`，与契约测试及状态语义严格对齐。

### 3.2 后端赛事公告访问鉴权增强 (`CompetitionController.java` & `CompetitionServiceImpl.java`)
1. **控制器层变更**：
   - `CompetitionController.listAnnouncements` 新增 `@CurrentUserId Long userId` 参数注入，并透传给 Service 层。
2. **服务层鉴权逻辑**：
   - 先行校验比赛存在性，不存在直接抛出 `BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在")`；
   - 调用 `validateUserParticipationOrAdmin(competitionId, userId)`：
     - 若 `userId == null`，抛出 `BusinessException(HttpStatus.UNAUTHORIZED, "用户未登录")`；
     - 通过 `memberRepository.findMemberWithTeam(competitionId, userId)` 检查用户是否具备 `MemberStatus.INVITED_ACCEPTED` 且战队状态为 `TeamStatus.AUDIT_PASSED` 的有效参赛记录；
     - 若未找到有效参赛记录，检查当前用户角色是否为 `UserRole.SUPER_ADMIN` 或 `UserRole.ADMIN`；若非管理员，抛出 `BusinessException(HttpStatus.FORBIDDEN, "您尚未加入已审核通过的参赛队伍")`。
   - 校验通过后，从 Redis 缓存中获取或加载公告数据，兼顾安全性与访问性能。

### 3.3 前端赛事详情页移除公告展示与请求 (`CompetitionDetailView.vue`)
- **模板层**：彻底移除赛事公告模块（`<div v-if="announcements.length > 0">` 整块公告列表及折叠内容）；
- **脚本层**：
  - 移除无用图标 `MegaphoneOutline` 引入；
  - 移除 `listAnnouncementsApi` 与 `CompetitionAnnouncementVO` 引入；
  - 移除 `announcements` 响应式状态声明；
  - 从 `fetchCompetitionDetail` 的 `Promise.all` 中移除 `fetchAnnouncements` 任务，并彻底删除 `fetchAnnouncements` 函数实现。

---

## 4. 影响范围与回归验证

1. **前端工程编译与类型校验**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），全端静态类型检查与打包 100% 通过（0 错误，0 警告）。
2. **后端单元与切片路由测试**：
   - 执行 `mvn test -pl volur-competition`：
     - `CompetitionControllerTest`：覆盖 `listCompetitions`、`listCompetitionSlice`、`listAnnouncements` 3 项路由测试均通过；
     - `CompetitionServiceImplTest`：新增 5 项针对 `listAnnouncements` 的鉴权与异常状态测试（404 竞赛不存在、401 未登录、403 未参赛/审核未通过、200 参赛选手放行、200 管理员放行），全部通过；
     - 全模块共 53 项单元测试全部一次性执行成功（0 Failures, 0 Errors）。
