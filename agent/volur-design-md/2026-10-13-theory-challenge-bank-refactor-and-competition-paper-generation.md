# 理论题库管理重构与理论竞赛试卷生成机制设计与实施报告

## 1. 变更背景与业务需求

在原有的理论赛题设计中，理论题目（`TheoryChallenge`）是以扁平列表形式平铺在题目管理中，竞赛对理论题目的引用缺乏清晰的层级聚合体系，无法支撑高复用、成体系的知识库管理。本次重构针对理论题目管理及理论竞赛组卷流程进行全链路升级：

1. **题目列表重构为“题库”列表**：
   - 列表原“题目名称”调整为“题库名称”，列项严格设置为：“题库名称”、“题目数量”、“创建人”、“创建时间”、“更新时间”、“操作”。
   - 操作按钮将“新增题目”重构为“新增题库”。
   - 搜索组件占位提示统一调整为“搜索题库名称或创建人”，移除题型（单选/多选/判断/简答）单项过滤选择框。
2. **题库导入导出与题库题目管理**：
   - 在列表工具栏“导出题库”按钮左侧新增“导入题库”按钮（支持 JSON / ZIP 包格式）。
   - 在每个题库的管理抽屉/弹窗中，提供完整的题目管理功能，并新增“导入题目”与“导出题目”按钮，允许在题库内部批量注入试题或归档题库资产。
3. **理论竞赛引用题库与动态试卷生成**：
   - 竞赛编辑界面（`AdminCompetitionEditView.vue`）中，当赛制为 `THEORY` 时，管理员可多选关联已创建的多个题库。
   - 提供按题型（单选题、多选题、判断题、简答题）的抽题数量与单题分值配置面板，以及“是否随机打乱抽题”开关。
   - 后端根据配置策略从关联的题库池中按题型筛选并随机/顺序抽题，生成竞赛专属试卷（`TheoryCompetitionPaper`），并实时统计总题数与总分值。

---

## 2. 架构设计与实现细节

### 2.1 数据库结构升级与幂等性迁移

本次在 [`scripts/upgrade/upgrade_to_latest.sql`](file:///H:/HuMoLix/github/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql) 中添加了严格具备幂等性的 DDL 增量脚本：

1. **新建 `theory_bank` 题库表**：
   - 包含主键 `id`、题库名称 `name`、题库描述 `description`、题目数量 `question_count`、创建人 `created_by`、更新人 `updated_by` 及时间戳。
2. **`theory_challenge` 关联字段升级**：
   - 新增 `bank_id` BIGINT 字段与外键索引 `idx_theory_bank_id`，标识题目所属的题库。
3. **新建 `theory_competition_bank` 竞赛题库关联表**：
   - 记录 `competition_id` 与 `bank_id` 的多对多绑定关系，包含唯一组合索引 `uk_comp_bank` 与反向检索索引。
4. **新建 `theory_competition_config` 理论组卷配置表**：
   - 记录单场理论赛事的组卷规则：单选题数量/分值、多选题数量/分值、判断题数量/分值、简答题数量/分值、是否随机抽题（`is_random`）、卷面总题数与总分值。

### 2.2 后端服务实现 (`volur-challenge` & `volur-competition`)

1. **`volur-challenge` 模块**：
   - 领域实体：新增 `TheoryBank`，`TheoryChallenge` 实体增加 `@ManyToOne` 关联 `bank`。
   - 持久化层：`TheoryBankRepository` 与 `TheoryChallengeRepository` 增加根据 `bankId` 统计与查询方法（`countByBankId`, `findByBankId`, `findByBankIdIn`）。
   - 服务层：`AdminTheoryChallengeService` 实现题库的分页查询、创建、编辑、删除、题库导入/导出（支持 JSON 与 ZIP 归档包），以及题库内题目的分页查询与导入/导出。
   - 控制器层：`AdminTheoryChallengeController` 暴露统一 API：
     - `GET /api/v1/admin/challenges/theory/banks`
     - `POST /api/v1/admin/challenges/theory/banks`
     - `PUT /api/v1/admin/challenges/theory/banks/{id}`
     - `DELETE /api/v1/admin/challenges/theory/banks/{id}`
     - `POST /api/v1/admin/challenges/theory/banks/import`
     - `GET /api/v1/admin/challenges/theory/banks/export`
     - `GET /api/v1/admin/challenges/theory/banks/{bankId}/questions`
     - `POST /api/v1/admin/challenges/theory/banks/{bankId}/questions/import`
     - `GET /api/v1/admin/challenges/theory/banks/{bankId}/questions/export`
2. **`volur-competition` 模块**：
   - 领域实体：`TheoryCompetitionBank` 与 `TheoryCompetitionConfig`。
   - 服务层：`AdminTheoryCompetitionService` 与实现类 `AdminTheoryCompetitionServiceImpl`：
     - `getConfig`: 读取指定比赛的组卷配置与关联题库概要信息；
     - `generatePaper`: 校验比赛是否存在且是否已有选手提交答卷（若已有答卷则禁止重新生成以防数据错乱快速失败）；解析勾选的题库集合，从 `TheoryChallengeRepository` 批量查询题目候选池；按单选、多选、判断、简答分别进行数量过滤校验（题库题目不足时抛出明确异常快速失败）；依据 `isRandom` 决定是否采用 `Collections.shuffle` 打乱随机抽取；批量持久化试卷项 `TheoryCompetitionPaper`。
     - `getPaperQuestions` & `removePaperQuestion`: 获取当前试卷题目列表并支持单题移除。
   - 控制器层：`AdminTheoryCompetitionController` 挂载路径 `/api/v1/admin/competitions/{competitionId}/theory`：
     - `GET /config`
     - `POST /paper/generate`
     - `GET /paper`
     - `DELETE /paper/{paperId}`

### 2.3 前端视图重构 (`volur-lite-view-NEXT`)

1. **`AdminTheoryChallengeView.vue`**：
   - 核心表格列调整为：题库名称、题目数量、创建人、创建时间、更新时间、操作。
   - 工具栏：主操作按钮为“新增题库”，右侧操作按钮区包含“导入题库”和“导出题库”。
   - 搜索栏：占位符设置为“搜索题库名称或创建人”，移除题型下拉筛选。
   - 题库弹窗：支持创建和编辑题库名称与描述。
   - 题库题目管理抽屉：点击操作栏“题目管理”弹出题库专属抽屉，展示题库内部题目列表，包含“导入题目”、“导出题目”与“新增题目”功能，全面实现闭环管理。
2. **`AdminCompetitionEditView.vue`**：
   - 当竞赛类型为 `THEORY` 时，动态渲染“理论试卷生成与题库策略”配置模块。
   - 支持多选关联系统中的公开题库，并实时配置四类题型的抽题数量与单题分值，以及是否随机抽题选项。
   - 一键点击“生成竞赛试卷”，前端即时刷新当前已生成的试卷题目明细表格。
3. **API 层完整性补齐 (`src/volur-lite-view-NEXT/src/api/competition.ts`)**：
   - 整合所有竞赛端前后台接口（竞赛详情、公告、战队报名与审核、Writeup 提交与审核、CTF 挂载与实例管理、操作日志审计、理论组卷策略配置与试卷生成）。
   - 遵循规范严格使用強类型 `ApiResponse<T>`，并确保所有视图的 import 正常解析。

---

## 3. 规范与安全合规核对

1. **零注释铁律 (Zero Comments Rule)**：
   - 前后端所有涉及变动与新建的代码文件（`.vue`、`.ts`、`.java`、`.sql`）均严格杜绝行内解释性注释与冗余说明，代码保持高内聚自解释。
2. **全链路快速失败与零 Mock (Fail-Fast & Zero-Mock Rule)**：
   - 题目不足、题库不存在、答卷已产生等边界场景均直接抛出强类型业务异常（如 `BusinessException` / HTTP 400/404/409），前端不使用假数据或默认值兜底。
3. **构建与编译验证**：
   - 后端 Maven 模块 `volur-challenge` 与 `volur-competition` 执行 `mvn test-compile` 均通过（`BUILD SUCCESS`）。
   - 前端工程 `volur-lite-view-NEXT` 执行 `vue-tsc --noEmit && vite build` 完整打包通过（`built in 12.49s`），无任何 TypeScript 或模板错误。
