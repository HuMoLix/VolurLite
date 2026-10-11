# 竞赛详情视图单页整合与报名入口文案调整记录

## 1. 变更背景与需求
在 `CompetitionDetailView.vue` 中，为了进一步提升移动端与桌面端用户浏览赛事信息的连续性与流畅度，对部分文案与信息组织架构进行精简和扁平化升级：
1. 移除报名操作卡片中多余的 “战队竞技制报名” 标签字样；
2. 将报名操作区域的主标题由 “报名参赛通道” 精简为 “报名参赛”；
3. 将原先通过页内选项卡（Tabs）分页展示的 “赛事简介与概况” 与 “规则与须知” 整合至同一页面中顺序展示，彻底移除页内 Tab 切换栏及相关响应式状态。

## 2. 改动详情

### 2.1 报名操作区文案与标签精简
- 文件：[CompetitionDetailView.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/CompetitionDetailView.vue)
- 移除了未报名状态下卡片顶部的 `战队竞技制报名` 胶囊徽标；
- 将主标题由 `报名参赛通道` 调整为 `报名参赛`，与全站其他业务卡片的操作标题保持简洁一致的风格。

### 2.2 赛事信息展示去 Tab 化与单页顺序整合
- 文件：[CompetitionDetailView.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/CompetitionDetailView.vue)
- 移除了包含 `overview` / `rules` / `announcements` 的页内切换按钮栏；
- 在主体详情卡片内，按顺序平铺展示：
  1. **赛事简介与概况**：包含赛事详细介绍（支持 Markdown 渲染）以及右侧赛程与关键指标卡片；
  2. **规则与须知**：包含组队与人员规模卡片、防作弊与提交流程卡片以及报名专属要求与须知（支持 Markdown 渲染），区块之间采用一致的细边框分割线与小标题；
  3. **赛事公告**：当赛事存在公告数据时，在规则区块下方以独立章节形式展开渲染，若无公告则不额外占据页面视觉空间。
- 移除了 `<script setup>` 中废弃的 `activeTab` 响应式变量。

## 3. 验证与审计
- 运行 `npm run build`，Vue SFC 模板与 TypeScript 类型检查全部通过；
- 遵循零注释规范与 Google Material Design 3 视觉阶梯规范。
