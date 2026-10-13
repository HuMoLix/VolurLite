# CTF 竞技场赛题卡片等待一血占位移除方案

## 1. 变更背景与架构目标

在 Volur Lite NEXT（`src/volur-lite-view-NEXT`）前端工程中，CTF 赛题卡片组件 `CtfCompetitionChallengeCard.vue` 负责在赛题列表网格中渲染单个赛题的基本概览信息，包括题目分类、当前分值、容器环境标记、题目标题、前三名解题队伍（Top 3 Solvers / 一二三血）以及底部攻克状态指示条。

此前，当赛题尚未被任何战队解出（`topTeamsList.length === 0`）时，卡片底部展示带有时钟图标的占位信息 `暂无队伍解出 · 等待一血`（`t('competition.awaiting_first_blood')`）以及上方的分割线（`pt-3 border-t`）。为保持卡片视觉极简与整洁、减少初始状态下大量题目卡片重复显示等待占位带来的视觉噪音，需删除该占位元素，使得没有队伍解出题目时不渲染该区域及空分割线。

---

## 2. 核心改动点与技术实现

### 2.1 赛题卡片占位元素与底部结构精简 (`CtfCompetitionChallengeCard.vue`)

针对 `src/views/competition/components/CtfCompetitionChallengeCard.vue` 进行了以下优化：
1. **删除等待一血占位节点**：
   - 移除 `v-else` 分支中的 `<div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted italic flex items-center gap-1 py-0.5">...</div>`（包含时钟图标 `TimeOutline` 与 `t('competition.awaiting_first_blood')` 文本）；
2. **条件提升与分割线优化**：
   - 将原先在内部列表的 `v-if="topTeamsList.length > 0"` 提升至包含上边框的父级容器 `<div v-if="topTeamsList.length > 0" class="pt-3 border-t ...">`；
   - 彻底避免在未解出状态下残留一条孤立且空白的 `border-t` 分割线，保持未攻克题目卡片的内敛与纯粹；
3. **清理无用图标依赖**：
   - 从 `@vicons/ionicons5` 导入列表中移除仅在该占位节点中使用的 `TimeOutline` 图标引用，保持代码整洁。

---

## 3. 安全隐患防范与架构边界

1. **零注释与代码纯洁性**：严格遵守《纯粹代码与零注释铁律》，组件代码中不包含冗余注释，设计方案与改动溯源统一收敛于本文档；
2. **样式与布局稳定**：外层卡片使用 `flex flex-col justify-between`，当底部无解出队伍时，分类与分值行维持在顶部，标题居中自适应，底部状态指示条保持底端贴合，网格排版无坍塌与抖动；
3. **语言包兼容性**：保留相关多语言键值（如详情模态框中的使用），不破坏其他依赖该文本的视图结构。

---

## 4. 影响范围与验证结果

- **影响文件**：
  - `src/views/competition/components/CtfCompetitionChallengeCard.vue`
- **验证结果**：
  - 执行 `vue-tsc --noEmit && vite build`：构建成功通过（0 errors，生产资源打包完成）。
