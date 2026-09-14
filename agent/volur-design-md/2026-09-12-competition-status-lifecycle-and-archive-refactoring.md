# 竞赛生命周期状态流转与归档机制前后端对齐施工文档

## 1. 变更背景与需求说明

在之前的实现中，管理员在新建及编辑竞赛时可随意选择竞赛状态（包括 `RUNNING`、`ENDED` 等），缺少基于赛程起止时间的自动化状态流转机制，且缺乏独立的竞赛归档与状态安全锁。为了规范赛事全生命周期管理，需要重构竞赛状态流转与归档机制：
1. **新建竞赛**：管理员在创建竞赛时不可自主选择状态，系统强制初始化为草稿状态 (`DRAFT`)。
2. **管理编辑竞赛**：管理员仅可在 `DRAFT`（草稿/下架）与 `PUBLISHED`（已发布）之间手动切换配置。
3. **自动化时间驱动流转**：
   - 处于 `PUBLISHED` 状态的竞赛，当到达比赛开始时间（`startTime`）时，后端自动流转为 `RUNNING`（进行中）；
   - 到达比赛结束时间（`endTime`）时，后端自动流转为 `ENDED`（已结束）；
   - 后端通过读取时的即时刷新（Lazy Refresh）和每分钟后台定时任务（Scheduled Sync）双重保障状态一致性。
4. **独立归档机制**：
   - 归档操作通过独立的 REST API（`POST /api/v1/admin/competitions/{id}/archive`）触发，归档后状态为 `ARCHIVED`；
   - 归档状态下直接修改竞赛状态被后端硬性拦截，前端状态选择器禁用并提示管理员必须先取消归档；
   - 提供取消归档 API（`POST /api/v1/admin/competitions/{id}/unarchive`），取消后状态重置为 `DRAFT`；
   - 前端管理后台编辑页顶部与列表操作栏均挂载“归档”/“取消归档”带确认框的气泡弹窗交互。

---

## 2. 改动点汇总

### 2.1 后端改动 (Java & Spring Boot)

1. **枚举定义扩展**：
   - 文件：[`CompetitionStatus.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/enums/CompetitionStatus.java)
   - 新增 `PUBLISHED(1, "PUBLISHED")`，作为管理员手动设置比赛发布的基准状态。
2. **实体层动态计算与状态刷新封装**：
   - 文件：[`Competition.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/Competition.java)
   - 新增 `resolveStatusByTime()` 与 `refreshStatus()` 方法：
     - 若当前状态为 `DRAFT` 或 `ARCHIVED`，时间不影响状态；
     - 若处于发布体系（`PUBLISHED`、`RUNNING`、`ENDED`），比对 `Instant.now()` 与 `startTime` / `endTime`，动态返回 `PUBLISHED`（未到开始时间）、`RUNNING`（进行中）或 `ENDED`（已结束）。
3. **Repository 层**：
   - 文件：[`CompetitionRepository.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/repository/CompetitionRepository.java)
   - 新增 `findByStatusIn(Collection<CompetitionStatus> statuses)` 便于定时任务批量同步发布中与进行中的活跃赛事。
4. **Service 接口与实现**：
   - 文件：[`AdminCompetitionService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/AdminCompetitionService.java) 及 [`AdminCompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminCompetitionServiceImpl.java)
   - `createCompetition`: 强制设置 `comp.setStatus(CompetitionStatus.DRAFT)`。
   - `updateCompetition`: 校验人工状态修改仅限 `DRAFT` 与 `PUBLISHED`，对已归档竞赛直接改状态予以拦截；更新发布态后即刻调用 `comp.refreshStatus()` 刷新。
   - 实现 `archiveCompetition(Long id)` 与 `unarchiveCompetition(Long id)`。
   - 新增 `@Scheduled(fixedRate = 60000)` 定时任务 `syncCompetitionStatusSchedule()` 定期持久化活跃赛事的最新状态。
   - 文件：[`CompetitionServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionServiceImpl.java)
   - 在公开端接口（`listCompetitions`、`getCompetitionBySlug`、`getCompetitionById`、`getCompetitionAbout`）中统一调用 `comp.refreshStatus()` 保证前台选手视图即时感知比赛状态转变。
5. **Admin 控制器路由扩展**：
   - 文件：[`AdminCompetitionController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/admin/AdminCompetitionController.java)
   - 新增 `POST /api/v1/admin/competitions/{id}/archive` 和 `POST /api/v1/admin/competitions/{id}/unarchive`。

---

### 2.2 前端改动 (Vue 3 & TypeScript)

1. **类型定义与 API 封装**：
   - 文件：[`src/types/competition/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/competition/index.ts)
   - 扩展 `CompetitionStatus` 联合类型，增加 `'PUBLISHED'`。
   - 文件：[`src/api/competition/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/competition/index.ts)
   - 导出 `archiveCompetitionApi(id)` 和 `unarchiveCompetitionApi(id)`。
2. **中英文多语言词条补全**：
   - 文件：[`zh_CN/competition.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/competition.json)、[`en_US/competition.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/competition.json)
   - 新增 `status_published` 键。
   - 文件：[`zh_CN/admin/competitions.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/competitions.json)、[`en_US/admin/competitions.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/competitions.json)
   - 新增 `action_archive`, `action_unarchive`, `archive_confirm`, `unarchive_confirm`, `archive_success`, `unarchive_success`, `form_status_tip` 等多语言键。
3. **编辑与创建基础信息 Tab (`BasicInfoTab.vue`)**：
   - 文件：[`BasicInfoTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/BasicInfoTab.vue)
   - 顶部右侧新增“归档”/“取消归档”气泡确认按钮（根据 `isArchived` 响应式切换）。
   - 新建竞赛时，状态表单项渲染为只读 Tag（固定为 `DRAFT`），无法下拉选择。
   - 已归档状态下，状态表单项显示只读已归档 Tag，并引导管理员通过上方操作栏取消归档。
   - 编辑竞赛时，下拉选项收敛为仅提供 `DRAFT`（草稿）与 `PUBLISHED`（已发布），纯净呈现简洁选择框，去除冗余的当前运行状态行与提示说明。
   - `createDTO` 提交固定为 `status: 'DRAFT'`。
   - 取消表单标题与赛制模式标签中的“ (一赛一制)”字样，统一为更直观的“创建竞赛”与“赛制模式”。
   - 优化横幅图片已存在场景下的操作交互：彻底去除半透明蒙层背景与 ghost 透明按钮，统一采用平台白色实体背景（`#ffffff`），去除基准边框色（默认透明边框防止尺寸抖动）与阴影投影（`box-shadow: none`），文字与图标统一为 `#333333`；交互反馈上，“更换横幅”在 hover 时采用绿色描边（`#18a058`），“移除横幅”在 hover 时采用红色描边（`#d03050`），无论底图明暗均平整纯粹、响应灵敏。
4. **管理列表与布局组件状态对齐**：
   - 文件：[`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue)：标题旁状态 Tag 支持 `PUBLISHED`，展示友好多语言文本。
   - 文件：[`AdminCompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionListView.vue)：`statusMap` 增加 `PUBLISHED`，并在操作列增加快捷“归档”/“取消归档”交互。
   - 文件：[`CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionListView.vue)：状态标签与类型增加对 `PUBLISHED` 的适配（展示为 info 蓝色标签）。
   - 文件：[`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue)：`isUpcoming` 与 `isRegistrationOpen` 计算属性全面接入 `PUBLISHED` 并在归档/结束时严格关闭报名。
   - 文件：[`CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/layout/CompetitionLayout.vue)：`statusMap` 和 `statusTagType` 补齐 `PUBLISHED`，修复 TypeScript 编译缺失属性报错。

---

## 3. 安全隐患修复与质量保证

1. **防恶意越权状态变更**：
   后端控制层与服务层切断了客户端直接将竞赛状态设置为 `RUNNING`、`ENDED`、`ARCHIVED` 的接口后门，杜绝管理员误操作或脚本攻击将未开始竞赛标记为进行中、或者绕过归档安全审计直接封存。
2. **单一数据真实源 (Single Source of Truth)**：
   赛程状态根据当前时间戳结合 `startTime` 与 `endTime` 动态裁决，杜绝了由于管理人员忘记修改状态而导致比赛已开始/已结束却依旧显示草稿或报名的严重时序不一致风险。
3. **编译与构建验证**：
   - 后端执行 `mvn clean compile -DskipTests`，全部 12 个模块编译成功（0 错误）。
   - 前端执行 `vue-tsc --noEmit && vite build`，全量打包构建成功（0 TypeScript 类型错误与构建警告）。
