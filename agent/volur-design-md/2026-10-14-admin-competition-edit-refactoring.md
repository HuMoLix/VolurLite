# 竞赛管理基础信息视图重构与隐藏状态支持方案

## 1. 变更背景与需求概述

本次重构主要针对管理员竞赛运维模块中 CTF 竞赛编辑与理论竞赛编辑的前端视图与后端领域状态进行系统性对齐与升级，满足精细化赛事配置与隐私管控要求：
1. **文案规范化**：将“基本信息与排程”（或“基础信息与赛程”）统一调整为简洁明确的“基础信息”，对齐平台设计规范；
2. **保存按钮位置优化**：移除浮动于表单顶部的独立保存按钮栏，参考 `AdminCtfCompetitionChallengeDetailView.vue` 的交互设计，将“保存基本信息”精简为“保存”，并直接放置在卡片标题行右侧，与页面各模块的小标题保持水平对齐；
3. **队伍人数限制配置**：在“基础信息”选项卡中，于“赛程与时间设置”卡片下方新增“队伍人数限制”卡片，提供“单队人数下限”（`minTeamMembers`）、“单队人数上限”（`maxTeamMembers`）、“最大领队人数”（`maxLeaders`）与“参赛队伍上限”（`maxTeams`）的数值配置能力，并在提交时严格校验下限不得大于上限；
4. **自定义报名要求与须知配置**：在“竞赛简介与规则”下方新增“自定义报名要求与须知”（`regRequirements`）表单项，集成自研 `VMarkdownEditor.vue` 组件，支持赛事主办方编写富文本/Markdown 格式的赛事准入指引；
5. **隐藏状态（HIDDEN）全链路支持**：新增竞赛状态 `HIDDEN`，实现隐藏赛事仅在管理员控制台（后台竞赛列表）可见，而在前台普通用户赛事列表（`listCompetitions` 与切片接口 `listCompetitionSlice`）中完全过滤与脱敏。

---

## 2. 架构设计与改动明细

### 2.1 后端工程改造 (`volur-competition`)

1. **领域枚举扩展 (`CompetitionStatus.java`)**：
   - 增加枚举项 `HIDDEN(7, "HIDDEN")`，对齐现有的 0-6 状态体系。
2. **状态自动计算与保护 (`Competition.java`)**：
   - 在 `Competition.resolveStatusByTime()` 中加入 `this.status == CompetitionStatus.HIDDEN` 的状态保留逻辑，防止被后台定时排程或实体刷新任务意外自动覆盖推进至 `RUNNING`、`REGISTRATION` 或 `ENDED`。
3. **管理员更新状态放行 (`AdminCompetitionServiceImpl.java`)**：
   - 在 `updateCompetition` 状态切换判定分支中增加对 `CompetitionStatus.HIDDEN` 的支持，允许管理员自由在 `DRAFT`、`PUBLISHED` 与 `HIDDEN` 之间进行切换。
4. **公开列表脱敏与过滤 (`CompetitionServiceImpl.java`)**：
   - 在常规分页接口 `listCompetitions` 中添加判定条件 `cb.notEqual(root.get("status"), CompetitionStatus.HIDDEN)`；
   - 在流式切片接口 `listCompetitionSlice` 中同步追加 `cb.notEqual(root.get("status"), CompetitionStatus.HIDDEN)`；
   - 管理员查询接口 `AdminCompetitionServiceImpl.listCompetitions` 不加过滤，管理员可完整检索并运维处于隐藏状态的赛事。

### 2.2 前端工程改造 (`src/volur-lite-view-NEXT`)

1. **TypeScript 类型对齐 (`types/competition/index.ts`)**：
   - `CompetitionStatus` 联合类型追加 `'HIDDEN'`；
   - 同步对齐 `src/volur-lite-view/src/types/competition/index.ts`。
2. **多语言词条补全 (`locales/**/competition.json`)**：
   - `zh-CN`: `"status_hidden": "隐藏"`；
   - `zh-TW`: `"status_hidden": "隱藏"`；
   - `en-US`: `"status_hidden": "Hidden"`。
3. **主控编辑视图 (`AdminCtfCompetitionEditView.vue` & `AdminTheoryCompetitionEditView.vue`)**：
   - tabs 配置中将基础信息 Tab 文本统一更新为 `t('admin.competitions.tab_basic', '基础信息')`。
4. **基础信息详情视图 (`AdminCtfCompetitionDetailView.vue` & `AdminTheoryCompetitionDetailView.vue`)**：
   - **标题与保存按钮**：卡片顶栏重构为 `flex items-center justify-between` 布局，左侧为小标题“基础信息”，右侧为 `VBtn`（文案统一为 `保存`，绑定 `SaveOutline` 图标）；
   - **自定义报名要求与须知**：在“竞赛简介与规则”后追加 `VMarkdownEditor`，双向绑定 `formData.regRequirements`；
   - **队伍人数限制卡片**：新增卡片容器，包含单队人数下限、单队人数上限、最大领队人数、参赛队伍上限 4 项配置；
   - **隐藏状态下拉选择**：`statusOptions` 增加“隐藏 (`HIDDEN`)”选项；`fetchDetail` 与 `handleSave` 完整支持 `HIDDEN` 状态的反显与保存。
5. **前台组件状态防御 (`VCompetitionCard.vue` & `VSearchTool.vue` & `CompetitionDetailView.vue`)**：
   - `effectiveStatus` 与各状态标签函数中同步加入 `HIDDEN` 处理，保障状态机流转健壮性。

---

## 3. 验证与审计结论

1. **后端自动化单元测试**：
   - 运行 `mvn test -pl volur-competition`，53 个用例全部执行通过（`BUILD SUCCESS`）；
2. **前端类型检查与生产构建**：
   - 运行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程 TypeScript 强类型校验 100% 通过，打包构建成功无警告。
