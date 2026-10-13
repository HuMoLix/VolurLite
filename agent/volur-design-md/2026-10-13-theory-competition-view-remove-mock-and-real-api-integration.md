# 理论知识赛考场视图取消 Mock 数据与真实 API 接入审计记录

- **记录日期**：2026-10-13
- **涉及模块**：`volur-lite-view-NEXT`（新一代前端）
- **变更文件**：
  - `src/views/competition/TheoryCompetitionView.vue`

---

## 1. 变更背景与动因

在先前的迭代中，`TheoryCompetitionView.vue` 包含了本地静态写死的 `MOCK_COMPETITION` 与 `MOCK_THEORY_PAPER`，并在请求失败或无数据时通过 `applyMockData()` 进行了假数据兜底填充；同时在交卷方法 `handleConfirmSubmit()` 中还残留了基于前端本地 `correctMap` 计算分数与正确率的模拟判分逻辑。

根据项目规范准则中的 **Fail-Fast & Zero-Mock 原则（全链路快速失败与严禁隐式 Mock / 伪造兜底）**，前端工程严禁在前后端交互或业务流转中进行任何形式的隐式 Mock、假数据或伪装成功兜底逻辑，必须百分之百对接真实后端 API，且在异常或缺失配置时直接抛出错误中断流转。

---

## 2. 详细改造点

### 2.1 彻底清理 Mock 数据与伪造判分逻辑
1. **移除静态数据模型**：彻底删除 `MOCK_COMPETITION`、`MOCK_THEORY_PAPER` 对象定义。
2. **移除兜底注入函数**：彻底废除 `applyMockData()` 方法，杜绝任何假数据污染 Vue 响应式状态。
3. **移除前端模拟评测**：彻底移除 `handleConfirmSubmit` 中基于静态答案映射表 `correctMap` 的本地算分与伪造评卷逻辑。

### 2.2 真实 API 闭环接入与链路对齐
1. **获取竞赛详情与试卷数据**：
   - 根据路由参数 `id` 或 `slug`（支持数字 ID 与 slug 双重安全兼容），调用 `getCompetitionByIdApi` / `getCompetitionBySlugApi` 获取真实竞赛详情 `CompetitionDetailVO`。
   - 携带已校验的 `competitionId` 调用 `getTheoryPaperApi(competitionId)`，直接对接后端 `TheoryCompetitionController.getPaper`（`/api/v1/competitions/{competitionId}/theory/paper`），获取试卷结构、题型及历史已作答记录。
2. **安全状态重置**：
   - 在加载新试卷时，原子化清空旧试卷的 `answers` 映射表、`markedIds` 待查标记集合，重置答题指针 `currentIndex` 至第 1 题。
3. **提交试卷与判分结果同步**：
   - 构造标准的 `TheoryPaperSubmitRequestDTO`，调用 `submitTheoryPaperApi(competitionId, payload)`，直接对接后端 `TheoryCompetitionController.submitPaper`（`/api/v1/competitions/{competitionId}/theory/submit`）。
   - 成功获取后端返回的 `TheoryPaperResultVO` 后，即时更新 `examResult`，切换试卷为已提交状态，并弹出成绩弹窗；清除草稿持久化存储。
   - 若提交失败或后端抛出业务异常（例如用户未加入审核通过的队伍等），直接向用户弹出真实的错误提示消息，杜绝伪造成功。
4. **成绩复核与状态重新同步**：
   - 用户在成绩弹窗中点击确认后，自动触发重新拉取已提交的试卷数据，展示后端持久化的得分与作答明细。

### 2.3 模板伪造默认值清洗
1. 将模板中原先带有硬编码假设的分值兜底 `paper?.totalScore || 100` 全面替换为真实安全的空值合并运算符 `paper?.totalScore ?? 0`，消除未经计算的虚假分值展示。

---

## 3. 安全隐患说明与架构合规性

1. **消除鉴权与业务绕过漏洞**：此前若接口报错，前端自动切入 Mock 判分并提示提交成功，导致未加入队伍或竞赛已结束的用户产生“已交卷成功”的假象；取消 Mock 后，后端安全守卫（`@RequiredCompetitionStatus` 及战队审核校验）得以真正生效。
2. **零注释铁律（文档外置）**：`TheoryCompetitionView.vue` 内部完全符合无多余行内注释规范，结构与变量命名高度自解释。
3. **TypeScript 严格类型校验**：全量代码通过 `vue-tsc --noEmit` 编译检查，零错误。
