# CTF 竞赛视图模块化拆分与 VCtfCategory 竖列表组件重构方案

## 1. 变更背景与目标

原 `CtfCompetitionView` 视图承载了 CTF 竞赛场景下的全部业务逻辑（赛题挑战、官方公告、天梯排行榜、战队信息与赛题详情弹窗、动态靶机调度等），单文件行数达到 1500+ 行，代码耦合度高且不易维护。同时页面上方存在冗余的 `<section>` 卡片头部横幅，与外层通用竞赛布局组件产生重复呈现。

本次重构旨在：
1. 移除 `CtfCompetitionView` 顶部的冗余 `<section>` 信息卡片，使整体界面紧凑并由外部容器统一驱动头部状态；
2. 在 `src/views/competition/tabs` 建立独立的 Tab 视图目录，将赛题挑战、官方公告、天梯排行榜、我的战队拆分为高内聚、职责单一的独立子组件；
3. 在 `src/components/competition/ctf` 下封装全新的 `VCtfCategory.vue` 竖列表分类导航组件，采用 Google Material Design 规范，支持分类图标映射、赛题数量气泡与选中高亮，并在 `CtfCompetitionChallengeListView.vue` 中集成使用。

---

## 2. 核心改动点

### 2.1 抽取 CTF 竖列表分类组件 `VCtfCategory.vue`
- 路径：`src/components/competition/ctf/VCtfCategory.vue`
- 职责：
  - 以竖列表（Vertical List）结构展示全部赛题分类（Web、Pwn、Reverse、Crypto、Misc、全部等）；
  - 内置常见分类的 Ionicons 图标映射（`AppsOutline`, `GlobeOutline`, `TerminalOutline`, `GitBranchOutline`, `KeyOutline`, `ExtensionPuzzleOutline` 等）；
  - 支持 `modelValue` 与 `v-model` 双向绑定，支持展示各分类下的题目数量 Badge；
  - 严格遵循 Google Blue 与 Google Surface 灰黑阶梯色彩体系，适配浅色与深色模式；
  - 通过 `src/components/competition/ctf/index.ts` 与 `src/components/competition/index.ts` 进行模块化统一导出。

### 2.2 拆分四大 Tab 视图组件 (`src/views/competition/tabs/`)
1. **`CtfCompetitionChallengeListView.vue` (赛题挑战)**：
   - 引用左侧 `VCtfCategory` 竖列表分类导航，右侧集成搜索框与自适应网格赛题卡片列表；
   - 内聚赛题详情 Modal 弹窗交互：题目详细描述、离线附件下载、Flag 格式校验与提交；
   - 动态靶机容器生命周期管理：拉起、续期、销毁与剩余租期倒计时定时器清理；
   - Flag 提交成功后触发 `@flag-solved` 事件向上通知总分与排行榜同步。
2. **`CtfCompetitionAnnouncementListView.vue` (官方公告)**：
   - 负责官方赛场公告的展示，支持置顶标识（`isPinned`）、关联赛题徽章、发布时间与发布人信息；
   - 包含无公告时的空状态友好提示。
3. **`CtfCompetitionScoreboardView.vue` (计分排行榜)**：
   - 负责实时解题天梯榜的表格化呈现；
   - 前三名冠军、亚军、季军高亮奖牌数字徽章，当前登录用户所在战队整行高亮与“本队”标签；
   - 战队名、解题数、总积分与最后提交时间响应式排版。
4. **`CtfCompetitionTeamView.vue` (我的战队)**：
   - 左侧展示战队概况、组织归属、用户角色与累计总分；
   - 右侧展示全部战队成员（队长/队员权限标签）及战队已攻克题目的历史记录勋章。
5. **`src/views/competition/tabs/index.ts`**：
   - 集中导出四大 Tab 组件，供上层主视图优雅按需导入。

### 2.3 精简主视图 `CtfCompetitionView`
- 删除了顶部冗余的 `<section>` 卡片块；
- 引入四大 Tab 子组件并通过 `<VFilterBtnGroup>` 受控切换渲染；
- 保留外层 `CompetitionLayout` 倒计时与时间截止事件处理，保留 API 数据抓取与 Mock 回退机制；
- 处理 `@flag-solved` 事件并自动更新天梯榜与战队累计得分。

---

## 3. 规范与安全合规

1. **纯粹代码与零注释铁律**：全部新增及修改的 `.vue`、`.ts` 文件均不包含任何行内解释性注释或 HTML 注释，代码结构清晰自解释。
2. **Google 风格色彩体系**：严格对齐 Google Blue (`#1a73e8` / `#8ab4f8`)、Google Green、Google Yellow 以及 Google Surface 灰黑阶梯，正文无纯黑刺眼配色。
3. **类型安全与零 Mock 假值欺骗**：雪花算法 ID 严格采用 `number | string` 声明，无 `any` 类型泛滥，接口交互严格对齐后端 DTO/VO。

---

## 4. 验证结果

- 执行 `npm run build` (`vue-tsc --noEmit && vite build`)，类型检查与打包无错误通过。
