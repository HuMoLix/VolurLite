# 竞赛管理编辑页赛制模式与竞赛状态选项文案精简与规范对齐审计说明

## 1. 变更背景与问题定位

在管理后台竞赛详情管理与创建界面（[`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 及其挂载的基础信息表单组件 [`BasicInfoTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/BasicInfoTab.vue)）中：
- 赛制模式选择项（`typeSelectOptions`）原本包含冗余的括号中文说明（例如 `CTF (解题赛模式)`、`AWD (攻防对抗模式)` 等）；
- 竞赛状态选择项（`statusSelectOptions`）以及只读展示态标签（`DRAFT`、`ARCHIVED`）同样包含大写英文后缀（例如 `${t('competition.status_draft')} (DRAFT)`、` (ARCHIVED)` 等）；
- 此类括号说明属于早期开发调试残留格式，既破坏了界面的视觉整洁度，也与全局统一的多语言设计规范不一致。

---

## 2. 改动点说明

对 [`BasicInfoTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/BasicInfoTab.vue) 中的赛制模式选项、状态选择器与只读状态标签进行文案精简重构：

1. **赛制模式选项（`typeSelectOptions`）移除括号说明**：
   - `CTF (解题赛模式)` 精简为纯净的 `'CTF'`；
   - `AWD (攻防对抗模式)` 精简为 `'AWD'`；
   - `AWD+ (攻防增强模式)` 精简为 `'AWD+'`；
   - `ISW (实战推演模式)` 精简为 `'ISW'`；
   - `THEORY (理论答题模式)` 精简为 `'THEORY'`。

2. **竞赛状态选项（`statusSelectOptions`）移除括号英文后缀**：
   - `${t('competition.status_draft')} (DRAFT)` 精简为 `t('competition.status_draft')`（根据当前语言动态输出“草稿”或“Draft”）；
   - `${t('competition.status_published')} (PUBLISHED)` 精简为 `t('competition.status_published')`（动态输出“已发布”或“Published”）。

3. **新建竞赛与已归档只读状态标签移除括号后缀**：
   - 新建模式下的只读草稿标签由 `{{ t('competition.status_draft') }} (DRAFT)` 精简为 `{{ t('competition.status_draft') }}`；
   - 归档状态下的只读标签由 `{{ t('competition.status_archived') }} (ARCHIVED)` 精简为 `{{ t('competition.status_archived') }}`。

---

## 3. 安全隐患与规范符合性

1. **统一全站 UI 视觉体验**：与管理后台列表页及前台竞赛大厅的标签规范保持 100% 对齐，杜绝不必要的括号后缀干扰；
2. **纯粹多语言响应**：去除硬编码拼接的大写英文标记，全量依靠 `vue-i18n` 机制进行多语言渲染；
3. **数据值与绑定完全无损**：仅修改展示层 `label` 属性及模板展示文案，内部绑定的 `value`（`CTF`、`DRAFT`、`PUBLISHED` 等）保持完全一致，不影响任何后端 API 调用及入参结构。

---

## 4. 影响范围与验证

- **影响文件**：[`BasicInfoTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/BasicInfoTab.vue)（作为 [`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 的基础信息配置子组件）。
- **类型检查与构建验证**：
  - `npx vue-tsc --noEmit`：0 错误，严格类型推导通过。
