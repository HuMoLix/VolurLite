# 理论竞赛考题规则配置与选手专属试卷动态生成设计与施工报告

## 1. 变更背景与需求说明
在原有的理论答题赛制设计中，管理员在后台编辑理论竞赛时，需要点击“生成 / 重构试卷”按钮，由系统统一在全局生成一份固定的考题列表并存储在数据库中，所有参赛选手进入考试时均拉取该同一份全局试卷。这种设计存在以下问题：
1. **题目固定缺乏个性化与防作弊能力**：无法根据题库动态为每个选手随机抽取不同试题。
2. **答题顺序与选项固定**：选手之间容易出现对题或抄袭答案的情况。
3. **运维操作繁琐**：管理员必须在后台预先静态生成试卷并浏览考题列表，与现代在线考试系统的“配置抽题规则 -> 考生入场实时动态生成专属试卷”的体验脱节。

根据最新业务需求，对理论竞赛的前后端逻辑进行了全链路重构：
1. **管理端题库与规则配置精简**：
   - 赛题管理中仅需配置引用的题库、各题型（单选、多选、判断、主观问答）的题目抽取数量与分值。
   - 增加两项核心规则勾选开关：“是否随机抽取题目并打乱题目顺序”以及“是否打乱答案顺序”。
   - 彻底移除后台繁琐的“当前试卷考题列表”与“生成 / 重构试卷”按钮，改为直观的“保存赛题配置”。
2. **选手端专属试卷动态生成与持久化状态机**：
   - 选手进入考试向后端请求试卷时，后端检测该选手是否已有试卷。若尚未生成，则根据管理员配置的规则（题目分布、是否随机抽题、是否打乱选项）从题库中实时抽取题目，为该选手独立生成一份专属试卷，并赋予唯一的 `paperId`（`TheoryUserPaper` 实体）。
   - 保证幂等性与一致性：选手刷新页面或重新进入考场时，直接拉取之前为其生成的唯一试卷，题目与选项顺序保持严格一致。
3. **题型块有序排列与题型内部随机抽取机制 (Block-Ordered Generation)**：
   - 原始逻辑抽完所有题型后执行了全局混排，导致单选题、多选题、判断题在整张试卷中交织混乱，前端答题卡按题型分类渲染时出现类似 2, 4, 6, 7, 8, 12 等跳跃题号。
   - 重构后强制执行**题型块有序排列**（单选题 1~N -> 多选题 -> 判断题 -> 主观问答题），并在各题型内部各自独立随机抽取并打乱题目顺序，确保整张试卷大结构井然有序，前端答题卡单选题从第 1 题开始连续线性编号。
   - 针对历史未交卷的混排乱序试卷引入无感自动探测自愈机制（`isPaperTypeOrdered`），自动清空并无损重建为全新的块有序试卷。

---

## 2. 核心架构与详细改动点

### 2.1 数据库与持久化层 (MySQL DDL 平滑增量迁移)
在迁移脚本 `scripts/upgrade/upgrade_to_latest.sql` 中增加了幂等存储过程更新逻辑：
1. **`theory_competition_config` 表**：
   - 增加字段 `shuffle_options` (TINYINT(1) DEFAULT 1)，用于持久化管理员对“是否打乱答案选项顺序”的设置。
2. **新增 `theory_user_paper` 表**：
   - 用户专属试卷主表，包含主键 `id` (BIGINT Snowflake ID，即整张试卷的唯一 `paperId`)、`competition_id`、`user_id`、`team_id`、`total_questions`、`total_score`、`is_submitted`、`my_total_score`、`created_at`、`updated_at`；
   - 设立唯一索引 `uk_theory_user_paper_comp_user` (`competition_id`, `user_id`)，保证一名选手在同一场竞赛中试卷的单一真实数据源。
3. **`theory_competition_paper` 表扩展**：
   - 增加 `user_paper_id` (BIGINT) 外键关联至 `theory_user_paper.id`；
   - 增加 `user_id` (BIGINT)；
   - 增加 `shuffled_options` (TEXT)，存储该题目针对该选手打乱后的选项 JSON 字符串；
   - 增加 `shuffled_standard_answer` (TEXT)，存储选项打乱后重新映射的转换标准答案。

### 2.2 后端领域模型与业务逻辑层 (Java & Spring Boot)
1. **实体与 Repository 建立**：
   - 新建 `TheoryUserPaper` 实体及 `TheoryUserPaperRepository`；
   - 更新 `TheoryCompetitionConfig`、`TheoryCompetitionPaper` 实体；
   - 更新 `TheoryCompetitionPaperRepository`，提供按 `userPaperId` 关联查询试卷题目的方法。
2. **数据传输对象 (DTO & VO)**：
   - 新增 `TheoryCompetitionConfigDTO`，供管理员保存赛题配置；
   - 更新 `TheoryCompetitionConfigVO`，包含 `shuffleOptions`；
   - 更新 `TheoryPaperExamVO`，返回 `paperId`（用户试卷主表唯一 ID）。
3. **块有序抽题算法与题型内部乱序 (Block-Ordered Extraction)**：
   - 按照标准考核流程严格分块抽取试题：首先抽取单选题并按数量填充 `sortOrder 1 ~ N`，随后依次抽取多选题、判断题与主观问答题；
   - 题型内部在候选集内先执行随机洗牌（`Collections.shuffle(copy)`）再按配额提取前 N 道，保证每个用户题目集随机且题型内部顺序随机，但大题型绝不混杂；
   - 在 `getPaper` 中内置自愈探测：若未交卷选手的现有试卷属于旧版本交织混排数据，自动检测并重建为标准块有序专属试卷。
4. **选项打乱与标准答案映射算法 (Option Shuffling Algorithm)**：
   - 针对单选题与多选题，解析原始选项（兼容 JSON 数组、JSON 对象及纯换行文本）；
   - 在开启 `shuffleOptions` 时，随机洗牌选项顺序并重置字母序号（A、B、C...）；
   - 构建 `oldKey -> newKey` 映射字典，自动将原题的 `standardAnswer`（如多选 `"A,D"`）重新映射并排序为当前选手专属试卷的标准答案（如 `"B,C"`），存储于 `shuffledStandardAnswer` 中；
   - 选手看到的选项与其作答的选项严格闭环对应，系统判分直接比对 `shuffledStandardAnswer`，彻底规避判分偏移或反向映射错误。
5. **服务层与控制器层实现**：
   - `AdminTheoryCompetitionController` 增加 `PUT /api/v1/admin/competitions/{competitionId}/theory/config` 接口；
   - `AdminTheoryCompetitionServiceImpl` 实现 `saveConfig` 方法，支持更新题库关联与规则配置并持久化；
   - `TheoryCompetitionServiceImpl.getPaper`：实现动态检查、并发加锁、自动抽题生成专属 `TheoryUserPaper` 与回填功能；
   - `TheoryCompetitionServiceImpl.submitPaper`：根据 `TheoryUserPaper` 关联的题目及 `shuffledStandardAnswer` 执行精确判卷，并标记 `isSubmitted = true`。

### 2.3 前端新一代工程 (`volur-lite-view-NEXT`)
1. **类型与 API 映射层**：
   - `src/types/competition/index.ts`：更新 `TheoryCompetitionConfigVO`、`TheoryPaperExamVO`，新增 `TheoryCompetitionConfigDTO`；
   - `src/api/competition.ts`：新增 `saveAdminTheoryCompetitionConfigApi`。
2. **管理端视图重构 (`AdminTheoryCompetitionEditView.vue`)**：
   - 赛题管理面板移除原“当前试卷考题列表”与“生成/重构试卷”按钮；
   - 新增“打乱选项与答案顺序 (推荐开启)”勾选框；
   - 新增“保存赛题配置”按钮，调用 `saveAdminTheoryCompetitionConfigApi`；
   - 移除已废弃的表格、行操作方法与无用依赖，确保代码符合严格类型检查。
3. **答题卡与试题卡片体验提升 (`TheoryPaperSheet.vue` & `TheoryQuestionCard.vue`)**：
   - 由于试卷按单选、多选、判断题分块顺序下发，答题卡中单选题区块按 1, 2, 3... 线性递增排列，彻底解决旧版 2, 4, 6, 7, 8 等跳跃乱序展示缺陷。

---

## 3. 安全防护与幂等性保障
1. **防重入与并发安全**：
   - 用户试卷生成方法具备线程同步控制与数据库联合唯一键 `(competition_id, user_id)` 约束，彻底防止选手在快速并发点击或多标签页开启时重复生成不同试卷。
2. **交卷防篡改**：
   - 选手提交答卷时严格校验 `TheoryUserPaper.isSubmitted` 状态，已交卷状态下禁止再次提交。
3. **全链路快速失败原则 (Fail-Fast)**：
   - 当管理员尚未配置赛题规则或题库内题目数量不足时，系统直接抛出明确的强类型 `BusinessException` 快速失败，杜绝伪造兜底假数据。
