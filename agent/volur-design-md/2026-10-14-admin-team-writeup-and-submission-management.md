# 竞赛管理战队封禁、Writeup批阅与CTF提交记录全链路实现施工记录

## 1. 变更背景与需求概述

针对后台管理竞赛运维模块及前台参赛校验，本次任务完成了以下三部分关键功能重构与新增：
1. **战队管理工具栏与封禁功能 (`AdminTheoryCompetitionTeamView.vue` 与 `AdminCtfCompetitionTeamView.vue`)**：
   - 移除搜索栏、状态过滤框及查询按钮所属卡片工具栏的背景色与边框，实现清爽扁平排版；
   - 在表格操作列中新增“封禁”按钮并支持弹窗确认；
   - 扩展后端 `TeamStatus.BANNED` 状态，并在战队审核服务中支持封禁状态持久化与站内信通知；
   - 同步在 CTF 与理论考核的前后台校验链中实施封禁拦截，被封禁战队的成员全链路无法再进入或参与对应比赛。
2. **CTF 题解管理重构 (`AdminCtfCompetitionWriteupView.vue`)**：
   - 在小标题下方增加 Flex 布局工具栏，使用 `VSearch.vue` 实现搜索，右侧配置“搜索”与“刷新列表”按钮；
   - 在题解列表中增加“操作”列及“批阅”按钮；
   - 增加题解批阅弹窗，支持对评阅状态（`REVIEWED`/`SUBMITTED`/`REJECTED`）、得分和评语进行录入并调用后端批阅接口；
   - 使用 `VPagination.vue` 结合后端 `PageResponse<CompetitionWriteupVO>` 实现分页。
3. **CTF 竞赛 Tabs 新增“提交记录” (`AdminCtfCompetitionSubmissionView.vue` 与 `AdminCtfCompetitionEditView.vue`)**：
   - 在 CTF 竞赛管理 Tabs 导航中增加“提交记录”页签；
   - 表格包含字段：序号、操作人、战队、题目名称、题目类型、提交内容、提交时间、提交状态；
   - 工具栏包含 `VSearch.vue` 搜索栏、按题目下拉筛选、查询与重置按钮；
   - 后端新增 `AdminCtfSubmissionController`、`AdminCtfSubmissionService`、`AdminCtfSubmissionServiceImpl` 以及 `CtfCompetitionSubmissionVO`，提供分页与按题目/关键词多维度过滤。

---

## 2. 详细改动点与文件清单

### 2.1 后端改动清单 (Java / Spring Boot)

1. **`com.humolix.volur.competition.domain.enums.TeamStatus`**
   - 新增枚举值 `BANNED(3, "BANNED")`。
2. **`com.humolix.volur.competition.domain.vo.AdminTeamStatsVO`**
   - 新增 `private Long banned` 统计字段。
3. **`com.humolix.volur.competition.service.admin.impl.AdminTeamServiceImpl`**
   - 在 `getTeamStats` 中增加 `countByCompetitionIdAndStatus(competitionId, TeamStatus.BANNED)` 统计；
   - 在 `auditTeam` 中增加对 `TeamStatus.BANNED` 的分支处理，落库封禁状态并向该队伍全员异步广播 `COMPETITION_AUDIT` 封禁通知。
4. **`com.humolix.volur.competition.service.ctf.impl.CtfCompetitionServiceImpl`**
   - 在 `getMyApprovedTeam` 与 `validateUserParticipationOrAdmin` 中增加封禁判定：当队伍状态为 `TeamStatus.BANNED` 时，抛出 `HttpStatus.FORBIDDEN` 明确拒绝：“您的战队已被封禁，无法参加此场比赛”。
5. **`com.humolix.volur.competition.service.theory.impl.TheoryCompetitionServiceImpl`**
   - 同步在 `getMyApprovedTeam` 与 `validateUserParticipationOrAdmin` 中增加 `TeamStatus.BANNED` 判定并抛出 403 异常。
6. **`com.humolix.volur.competition.service.impl.CompetitionServiceImpl`**
   - 同步在 `getMyApprovedTeam` 与 `validateUserParticipationOrAdmin` 中增加 `TeamStatus.BANNED` 判定并抛出 403 异常。
7. **`com.humolix.volur.competition.repository.CompetitionWriteupRepository`**
   - 新增带 `Pageable` 分页参数的 `searchByCompetitionIdAndKeyword` 查询方法。
8. **`com.humolix.volur.competition.controller.v1.admin.AdminCompetitionWriteupController`**
   - `listWriteups` 返回类型升级为 `PageResponse<CompetitionWriteupVO>`，支持 `page`、`size` 与 `keyword` 参数。
9. **`com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionSubmissionVO`**
   - 新建 VO，封装提交记录所需字段（`id`, `userId`, `username`, `teamId`, `teamName`, `challengeId`, `challengeName`, `challengeCategory`, `submittedFlag`, `status`, `createdAt`）。
10. **`com.humolix.volur.competition.repository.ctf.CtfCompetitionSubmissionRepository`**
    - 继承 `JpaSpecificationExecutor<CtfCompetitionSubmission>`，支持基于 Criteria 的动态条件检索。
11. **`com.humolix.volur.competition.service.admin.ctf.AdminCtfSubmissionService` & `AdminCtfSubmissionServiceImpl`**
    - 新增提交记录管理服务接口与实现类，提供对 `competitionId`、`challengeId`、`keyword`（队伍名/用户名/赛题名/Flag）的多条件分页查询。
12. **`com.humolix.volur.competition.controller.v1.admin.ctf.AdminCtfSubmissionController`**
    - 新增 REST 控制器 `GET /api/v1/admin/competitions/{competitionId}/ctf/submissions`，提供分页提交记录查询。

### 2.2 前端改动清单 (Vue 3 / TypeScript / Naive UI)

1. **`src/types/competition/index.ts`**
   - 扩展 `TeamStatus` 包含 `'BANNED'`；
   - 扩展 `AdminTeamStatsVO` 包含 `banned?: number`；
   - 新增 `CtfCompetitionSubmissionVO` 实体定义。
2. **`src/api/competition.ts`**
   - 更新 `listAdminCompetitionWriteupsApi` 支持分页参数并返回 `PageResponse<CompetitionWriteupVO>`；
   - 新增 `listAdminCtfSubmissionsApi` 函数调用提交记录接口。
3. **`src/views/admin/competition/AdminCtfCompetitionTeamView.vue` & `AdminTheoryCompetitionTeamView.vue`**
   - 移除工具栏卡片样式（背景色与边框），采用扁平布局；
   - 在状态下拉项中增加“已封禁”选项；
   - 表格状态列根据 `status === 'BANNED'` 渲染醒目红色“已封禁”标签；
   - 表格操作列针对未被封禁的队伍增加“封禁”按钮，点击弹出确认框并调用审核接口进行封禁；
   - 封禁后无法点击“通过”或“驳回”，仅保留“详情”与“删除”。
4. **`src/views/competition/CtfCompetitionView.vue` & `TheoryCompetitionView.vue`**
   - 在用户进入工作台的权限前置检查中显式判定 `currentTeam.status === 'BANNED'`，若已封禁则 Toast 提示并强制拦截跳转出工作台。
5. **`src/views/admin/competition/AdminCtfCompetitionWriteupView.vue`**
   - 增加 Flex 布局工具栏，使用 `VSearch.vue`、搜索按钮与刷新按钮；
   - 表格操作列增加“批阅”按钮；
   - 增加批阅弹窗，支持调整评阅状态、输入得分与评阅评语并提交更新；
   - 底部引入 `VPagination.vue` 组件联动分页。
6. **`src/views/admin/competition/AdminCtfCompetitionSubmissionView.vue`**
   - 全新编写 CTF 提交记录管理组件；
   - 包含序号、操作人、战队、题目名称、题目类型、提交内容、提交时间、提交状态等列；
   - 工具栏支持关键词搜索与按挂载赛题筛选，底部支持标准分页。
7. **`src/views/admin/competition/AdminCtfCompetitionEditView.vue`**
   - 在 Tabs 中引入“提交记录”页签与图标，并挂载 `AdminCtfCompetitionSubmissionView` 组件。
8. **多语言词条 (`zh-CN`, `en-US`, `zh-TW`)**
   - 补充 `tab_submissions` 词条，保持基础信息词条命名规范。

---

## 3. 验证与审计结论

1. **后端验证**：
   - 执行 `mvn test -pl volur-competition`，53 个单元测试全部通过（0 失败，0 错误），`BUILD SUCCESS`。
2. **前端验证**：
   - 执行 `npm run build`，通过 `vue-tsc --noEmit` 严格类型检测，Vite 打包构建全部成功。
3. **代码规范审计**：
   - 全程遵循**纯粹代码与零注释铁律**，代码文件中无多余行内解释性注释；
   - 遵循 Thin Controller、Lombok 构造注入与强类型 VO/DTO 约定。
