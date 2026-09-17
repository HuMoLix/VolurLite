# 竞赛管理编辑页标题状态徽章文案精简与国际化对齐审计说明

## 1. 变更背景与问题定位

在管理后台竞赛详情管理页 [`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 中：
- 顶部导航栏右侧的状态徽章原本采用了中英文双拼硬编码拼接格式，例如：`进行中 (RUNNING)`、`草稿 (DRAFT)` 等；
- 该显示方式属于开发调试期遗留格式，既侵占了移动端/中等分辨率下的水平排版宽度，也与其他页面（如 [`AdminCompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionListView.vue)、[`CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionListView.vue)）的标准单一国际化标签呈现不一致。

---

## 2. 改动点说明

对 [`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 中的状态文本格式化函数 `getStatusText` 进行精简重构：
- 去除所有分支末尾拼接的大写英文状态后缀（如 ` (RUNNING)`、` (DRAFT)` 等）；
- 严格返回多语言国际化统一词条：
  - `RUNNING` -> `t('competition.status_running')`（中文环境下输出纯净的“进行中”，英文环境下输出“In Progress”）；
  - `DRAFT` -> `t('competition.status_draft')`（“草稿”）；
  - `PUBLISHED` -> `t('competition.status_published')`（“已发布”）；
  - `REGISTRATION` -> `t('competition.status_registration')`（“报名中”）；
  - `FROZEN` -> `t('competition.status_frozen')`（“已封榜”）；
  - `ENDED` -> `t('competition.status_ended')`（“已结束”）；
  - `ARCHIVED` -> `t('competition.status_archived')`（“已归档”）。

---

## 3. 安全隐患与规范符合性

1. **统一全站 UI 视觉体验**：与平台设计规范中的状态标签视觉规范保持 1:1 对齐，消除开发期临时调试文字；
2. **纯粹多语言响应**：依托 `vue-i18n` 的响应式映射，跟随页面中英文切换实时翻译，无生硬英文括号拼接残留。

---

## 4. 影响范围与验证

- **影响文件**：[`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue)；
- **类型检查与构建验证**：
  - `npx vue-tsc --noEmit`：0 错误，严格类型推导通过。
