# 前台竞赛详情视图（CompetitionDetailView）排版重构与单页化审计说明

## 1. 变更背景与需求说明
根据用户的最新指令：
> 「现在对 @[src/volur-lite-view/src/views/competition/CompetitionDetailView.vue] 的页面排版实现修改：
> 1. 删除“竞技赛制：CTF 解题赛”的字样的提醒
> 2. 将竞赛简介与赛程、队伍与防作弊规则不要使用tabs的展示方式。整体为一页的展示方式。」

在原有设计中：
1. 主控制卡片下方常驻展示了带有 `竞技赛制：CTF 解题赛` 标题及赛制规则描述的 `n-alert` 提示框，该内容在详情页中产生了一定视觉冗余；
2. “竞赛简介与赛程”与“队伍与防作弊规则”采用了 `n-tabs` 分段标签页方式进行展示，用户需要手动来回切换 Tab 才能查看完整信息，割裂了阅读流程，且容易忽略关键的队伍与作弊防范规则。

为提供更加连贯、一目了然且聚焦的一体化浏览体验，本次任务对 [`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue) 的排版进行了单页化重构，并对中英文国际化语言包进行了同步对齐。

---

## 2. 核心改动点

### 2.1 彻底删除赛制特性指南提醒卡片
- **删除模板元素**：
  - 移除了原有的 `Section 2: 赛制特性指南` `<n-alert :type="getTypeTagType(competition.type)" :title="`竞技赛制：${getTypeLabel(competition.type)}`"> ... </n-alert>` 模块；
  - 彻底移除了“竞技赛制：CTF 解题赛”等赛制提醒文本与指南框。
- **清理无用脚本与类型**：
  - 移除了仅供该 Alert 调用的 `getTypeLabel`、`getTypeTagType`、`getTypeGuide` 方法；
  - 清理了未使用的 `CompetitionType` 类型导入。

### 2.2 竞赛简介、赛程与规则单页化平铺重构
- **废除 `n-tabs` 标签切换**：
  - 删除了 `<n-tabs type="segment">` 与 `<n-tab-pane>` 结构，无需用户点击切换；
  - 脚本中清理了 `NTabs`、`NTabPane` 组件导入，并按需引入 `NDivider`。
- **单页垂直层级结构设计**：
  在主卡片 `<n-card class="content-detail-card" :bordered="false">` 内部，统一采用一页式排版：
  1. **第一部分：竞赛简介与赛程（`section_overview`）**：
     - 区域标题：携带 `DocumentTextOutline` 图标与 `h3` 标题；
     - 赛事介绍描述：采用 `.desc-text`（支持 `white-space: pre-wrap` 文本自然换行排版，字体颜色采用规范规定的 `#444444` / `var(--v-text-desc)`）；
     - 赛程描述表格（`n-descriptions`）：清晰展示比赛时间、报名时间（如有）、参赛规模。
  2. **优雅分隔符（`n-divider`）**：
     - 使用 Naive UI 原生 `<n-divider class="section-divider" />` 进行轻量分割。
  3. **第二部分：队伍与防作弊规则（`section_rules`）**：
     - 区域标题：携带 `ShieldCheckmarkOutline` 图标与 `h3` 标题；
     - 规则描述表格（`n-descriptions`）：清晰展示队伍规则（人数限制）、报名审核要求、赛后 Writeup 提交要求。

### 2.3 国际化多语言补全（i18n）
在 `zh_CN/competition.json` 与 `en_US/competition.json` 中补全了对应的章节标题键：
- 中文（`zh_CN`）：
  - `"section_overview": "竞赛简介与赛程"`
  - `"section_rules": "队伍与防作弊规则"`
- 英文（`en_US`）：
  - `"section_overview": "Overview & Schedule"`
  - `"section_rules": "Team & Anti-Cheat Rules"`

### 2.4 SCSS 样式精炼与规范对齐
- 移除原 `.tab-pane-content` 样式；
- 新增 `.detail-section`、`.section-header`、`.section-icon`、`.section-title` 与 `.section-divider` 样式；
- 遵循 [`view-Rules.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-skills/view-Rules.md) 核心视觉规范，描述文字严格采用 `var(--v-text-desc, #444444)`，卡片背景统一采用 `var(--v-bg-card, #ffffff)`。

---

## 3. 构建与验证结果

1. **前端工程编译与类型检查**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：
     - TypeScript 静态检查与打包构建 **100% 通过（0 错误，0 告警）**；
     - `CompetitionDetailView` 产物体积进一步由 13.10 kB 缩减至 **11.87 kB**（gzip: 4.45 kB）；
     - 整体构建在 4.24s 内成功完成。

---

## 4. 影响范围与审计结论

- **影响文件清单**：
  1. [`src/volur-lite-view/src/views/competition/CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue)（视图排版重构）
  2. [`src/volur-lite-view/src/locales/zh_CN/competition.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/competition.json)（中文语言包新增标题键）
  3. [`src/volur-lite-view/src/locales/en_US/competition.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/competition.json)（英文语言包新增标题键）
- **审计结论**：
  变更完全符合指令要求，消除了赛制提示框并实现了“竞赛简介与赛程”、“队伍与防作弊规则”在单页中的流畅垂直整合，消除了多余的 tab 切换操作，代码无未使用变量与 dead code，完全符合 Volur Lite 前端设计与工程规范。
