# 理论知识考核视图重构与答题卡分组及自动保存审计文档

## 1. 变更背景与需求
针对理论知识赛考试界面（`TheoryCompetitionView.vue`）及其关联答题卡组件（`TheoryPaperSheet.vue`），统一进行以下视图精简、体验增强与防数据丢失优化：
1. **删除 Section 顶部胶囊**：移除赛事概览卡片中“理论知识考核/理论知识赛”的微型胶囊标识；
2. **删除副标题（介绍）**：移除概览卡片中的副标题描述文本，仅保留主标题与试卷统计概览；
3. **答题卡分类分组**：在答题卡（`TheoryPaperSheet.vue`）中按照“单选题”、“多选题”、“判断题”进行分组渲染。当试卷中不存在某一类型题目时，该分组不予展示；
4. **每 3 秒自动保存答题状态**：后台定时持久化用户当前填写的作答内容、标记题号与当前浏览题号至本地存储，防止意外刷新导致未提交数据丢失。

---

## 2. 核心改动点说明

### 2.1 赛事概览卡片精简 (`TheoryCompetitionView.vue`)
- **胶囊删除**：移除 `<div class="inline-flex ..."><span>{{ t('competition.type_theory') }}</span></div>` 胶囊徽章。
- **副标题删除**：移除 `<p class="text-xs sm:text-sm text-google-text-desc ...">` 描述文本，保留 `<h1>` 标题与右侧试卷题目总数、总分及最终得分统计。

### 2.2 答题卡多题型分组展示 (`TheoryPaperSheet.vue`)
- **题型分组计算属性 (`questionGroups`)**：
  - 遍历 `props.questions`，将试卷题目按 `SINGLE_CHOICE`（单选题）、`MULTIPLE_CHOICE`（多选题）、`TRUE_FALSE`（判断题）、`ESSAY`（简答题）等类型归类；
  - 记录题目原始下标（`index: number`），确保在分组视图中题目序号正确显示为试卷全局全局序号（`item.index + 1`），跳转动作精准匹配原试卷题目。
  - **空题型过滤**：仅当某题型题目数量大于 0 时，才将该分组追加到分组渲染列表中，满足“若没有某一分类则不显示分组”的设计要求。

### 2.3 答题进度 3 秒定时自动保存与恢复 (`TheoryCompetitionView.vue`)
- **隔离命名空间 Key**：采用 `volur_theory_draft_${competitionId}_${userId}` 作为存储 Key，防止不同用户或不同考核试卷之间草稿数据相互污染。
- **定时与时序保障**：
  - 在 `onMounted` 中启动 `setInterval(saveDraftAnswers, 3000)`，每 3 秒执行一次快照持久化；
  - 绑定 `window.addEventListener('beforeunload', saveDraftAnswers)` 及 `onUnmounted` 卸载钩子，确保在用户直接按 F5 刷新或路由切换时即时落盘；
  - 在 `applyMockData()` 与 `fetchCompetitionAndPaper()` 获取到试卷题目后，若试卷未提交（`!isSubmitted.value`），则自动调用 `restoreDraftAnswers()` 将草稿数据还原到响应式状态中；
  - 试卷成功提交（`handleConfirmSubmit`）后立即调用 `clearDraftAnswers()` 清理本地草稿。

---

## 3. 影响范围与测试验证
- **受影响文件**：
  - `src/volur-lite-view-NEXT/src/views/competition/TheoryCompetitionView.vue`
  - `src/volur-lite-view-NEXT/src/components/competition/theory/TheoryPaperSheet.vue`
  - `src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue`（TS 编译类型兼容性对齐）
- **验证结论**：
  - 运行 `npm run build`，`vue-tsc --noEmit` 与 Vite 生产构建均顺利通过，无类型及语法错误。
  - 代码严格遵循零内联注释（零解释性注释）铁律与纯粹代码自解释准则。
