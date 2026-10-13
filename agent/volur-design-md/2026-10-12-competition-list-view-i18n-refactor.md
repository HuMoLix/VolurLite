# 2026-10-12 CompetitionListView 视图多语言 (i18n) 全面完善记录

## 1. 变更背景与需求概述

在 `src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue` 竞赛列表视图中，原先存在多处硬编码的中文字符串与未完全对齐的国际化调用，包括页面标题、搜索框占位符、筛选下拉菜单触发按钮标题、流式列表触底加载提示文本、底部私人竞赛导入兜底卡片的标题/说明/按钮文本，以及导入按钮点击后的交互反馈 Toast 提示。

为了确保全站多语言切换（`zh-CN`、`zh-TW`、`en-US`）的完整性与一致性，本任务对 `CompetitionListView.vue` 及其关联的多语言资源文件进行全量梳理与标准化补齐。

---

## 2. 核心改动详情

### 2.1 多语言词条扩展与定义
在 `src/volur-lite-view-NEXT/src/locales/` 下针对 `zh-CN`、`en-US`、`zh-TW` 补齐对应词条：

1. **`common.json`**：
   - 新增 `"more"`:
     - `zh-CN`: `"更多"`
     - `en-US`: `"More"`
     - `zh-TW`: `"更多"`

2. **`competition.json`**：
   - 新增 `"competition_list"`：
     - `zh-CN`: `"竞赛列表"`
     - `en-US`: `"Competitions"`
     - `zh-TW`: `"競賽列表"`
   - 新增 `"loading_more"`：
     - `zh-CN`: `"正在加载更多竞赛..."`
     - `en-US`: `"Loading more competitions..."`
     - `zh-TW`: `"正在載入更多競賽..."`
   - 新增 `"prompt_headline"`：
     - `zh-CN`: `"这里找不到您的竞赛？"`
     - `en-US`: `"Can't find your competition here?"`
     - `zh-TW`: `"這裡找不到您的競賽？"`
   - 新增 `"prompt_desc"`：
     - `zh-CN`: `"此列表仅显示公开到 Volur Studio 的竞赛。如果没有找到您所参与的，您可以导入竞赛的邀请码来将您参与的私人竞赛添加列表中。"`
     - `en-US`: `"This list only displays competitions made public to Volur Studio. If you can't find yours, you can import an invite code to add your private competition to the list."`
     - `zh-TW`: `"此列表僅顯示公開到 Volur Studio 的競賽。如果沒有找到您所參與的，您可以匯入競賽的邀請碼來將您參與的私人競賽添加列表中。"`
   - 新增 `"import_competition"`：
     - `zh-CN`: `"导入竞赛"`
     - `en-US`: `"Import Competition"`
     - `zh-TW`: `"匯入競賽"`
   - 新增 `"import_wip_tip"`：
     - `zh-CN`: `"私人竞赛导入功能正在联调中"`
     - `en-US`: `"Private competition import feature is currently in integration."`
     - `zh-TW`: `"私人競賽匯入功能正在聯調中"`

### 2.2 `CompetitionListView.vue` 模板与脚本重构
1. **标题区域**：
   - 将原硬编码 `<h2 class="competition-title">竞赛列表</h2>` 替换为响应式调用 `{{ t('competition.competition_list') }}`。
2. **更多筛选菜单按钮**：
   - 将 `:title="isDropdownTabSelected ? selectedDropdownTab?.label : t('common.more', '更多')"` 简化并规范化为 `:title="isDropdownTabSelected ? selectedDropdownTab?.label : t('common.more')"`。
3. **搜索框占位符**：
   - 将 `placeholder="搜索竞赛名称或标识..."` 改为动态绑定 `:placeholder="t('competition.search_placeholder')"`。
4. **触底加载指示器**：
   - 将硬编码 `<span>正在加载更多竞赛...</span>` 替换为 `<span>{{ t('competition.loading_more') }}</span>`。
5. **底部导入引导卡片**：
   - 标题：`{{ t('competition.prompt_headline') }}`；
   - 描述文案：`{{ t('competition.prompt_desc') }}`；
   - 导入按钮：`{{ t('competition.import_competition') }}`。
6. **脚本交互反馈**：
   - 在 `handleImportClick()` 中使用 `window.$message.info(t('competition.import_wip_tip'))` 代替原硬编码文字。

---

## 3. 规范执行与验证

- **零注释铁律与文档外置**：修改后的 Vue 代码与 JSON 国际化文件保持极致纯粹，零行内解释性注释。
- **纯粹单一数据源**：未保留任何旧命名别名或兜底桥接，各语言词条均 1:1 对齐。
- **类型检查与构建验证**：
  - 运行 `vue-tsc --noEmit` 0 错误；
  - 运行 `vite build` 成功通过打包。
