# CtfCompetitionArenaView Tabs 拆分与目录结构重构文档

**日期**：2026-09-18  
**类型**：前端架构重构 / 模块化解耦  
**影响模块**：`src/volur-lite-view`（竞赛竞技场子系统）

---

## 1. 变更背景与诉求

在原先的设计与实现中：
1. 主竞技场视图组件 [`CtfCompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/ctf/CtfCompetitionArenaView.vue) 平铺在 `src/views/competition/` 根目录下，且单文件代码量超过 1000 行，将赛题演练、动态靶机管理、Flag 校验提交、比赛专属公告、排行榜及战队信息等所有业务逻辑与样式全部混杂在单一组件中。
2. 历史留存的兼容性代理组件 `src/views/competition/CompetitionArenaView.vue` 仅作为一个转发壳（8 行代码），需要审计是否仍存在外部引用，若无引用应予以彻底清除。
3. 为了提高竞赛竞技场模块的可维护性、可拓展性与代码内聚度，需将 `CtfCompetitionArenaView.vue` 归入专有模块目录 `src/views/competition/ctf/`，并将其四大 Tab 页面（题目演练、比赛公告、实时排行榜、我的战队）拆分下沉至 `src/views/competition/ctf/tabs/` 独立组件中。

---

## 2. 引用分析与冗余文件清理

### 2.1 `CompetitionArenaView.vue` 引用排查
- 全工程 Grep 搜索 `CompetitionArenaView`，结果确认：
  - 路由定义文件 [`routes.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/router/routes.ts) 早已将 `/arena/:slug` 的子路由直接指向了 `CtfCompetitionArenaView.vue`；
  - 没有任何其他 Vue 组件或业务代码导入 `CompetitionArenaView.vue`；
  - 仅历史审计记录文档中存在过往提及。
- **结论**：[`CompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionArenaView.vue) 确实已无任何有效代码引用，确认为冗余文件，已予以安全删除。

### 2.2 历史平铺组件清理
- 原位于 `src/views/competition/CtfCompetitionArenaView.vue` 的平铺旧文件在完成目录搬迁与重构后，已一并清理删除，杜绝同名组件在多处并存造成维护混淆。

---

## 3. 目录结构与 Tab 解耦拆分方案

### 3.1 调整后的目录拓扑

```text
src/views/competition/
├── CompetitionDetailView.vue       # 竞赛详情主视图
├── CompetitionListView.vue         # 竞赛列表视图
├── components/                     # 竞赛公用弹窗/组件
│   ├── TeamFormModal.vue
│   └── TeamInvitationsModal.vue
└── ctf/                            # CTF 竞技场专有模块
    ├── CtfCompetitionArenaView.vue # 竞技场主视图控制器与 Tab 容器
    └── tabs/                       # 竞技场 Tab 独立组件库
        ├── ChallengesTab.vue       # Tab 1: 题目演练、分类筛选与靶机控制台
        ├── AnnouncementsTab.vue    # Tab 2: 比赛公告列表（支持置顶与 Markdown）
        ├── ScoreboardTab.vue       # Tab 3: 实时排行榜数据表
        ├── TeamTab.vue             # Tab 4: 参赛战队信息与成员名单
        └── index.ts                # 统一聚合导出出口
```

### 3.2 各拆分子组件职责说明

1. **`ChallengesTab.vue`**：
   - 负责 CTF Jeopardy 赛题分类（ALL, Web, Pwn, Reverse, Crypto, Misc）与题目卡片渲染；
   - 兼顾非 CTF 模式（THEORY, AWD, AWD+, ISW）的引导信息展示；
   - 内置赛题详情弹窗、动态容器靶机启停/续期控制（`handleStartInstance`, `handleStopInstance`, `handleExtendInstance`）及 Flag 提交（`handleFlagSubmit`）业务逻辑；
   - 样式完全作用域化隔离（`.challenges-tab`, `.challenge-card`, `.instance-box` 等）。

2. **`AnnouncementsTab.vue`**：
   - 负责拉取和展示指定赛事的公告通报列表；
   - 支持置顶（`isPinned`）醒目样式，采用 `renderMarkdown` 渲染公告富文本正文；
   - 提供独立手动刷新机制及加载骨架。

3. **`ScoreboardTab.vue`**：
   - 负责拉取和展示指定赛事的战队排行榜数据表；
   - 支持实时榜单刷新与积分展示。

4. **`TeamTab.vue`**：
   - 负责展示当前登录选手的参赛战队信息与战队审核状态；
   - 展示战队成员阵容表格（队长/队员标签及状态）；
   - 联动全局 `useCompetitionStore()` 维护的当前战队状态，具备单点去重加载机制。

5. **`CtfCompetitionArenaView.vue`（主视图容器）**：
   - 仅负责顶栏 Tab 切换控制（保留符合 Volur 规范的自适应宽度选项卡卡片 `arena-nav-bar`）与竞赛基本信息的初始化绑定；
   - 通过 `activeTab` 状态按需切换渲染子 Tab 组件；
   - 通过模板引用（`ref`）在 Tab 切换时触发子组件对应的数据同步。

---

## 4. 路由配置更新

在 [`src/router/routes.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/router/routes.ts) 中，将 `/arena/:slug` 路由的组件导入路径由原先平铺的：
```ts
component: () => import('@/views/competition/CtfCompetitionArenaView.vue')
```
更新为：
```ts
component: () => import('@/views/competition/ctf/CtfCompetitionArenaView.vue')
```

---

## 5. 质量保证与构建验证

1. **TypeScript 类型静态分析**：
   - 执行 `npx vue-tsc --noEmit`，结果退出码为 0，0 语法错误，0 类型告警。
2. **生产构建测试**：
   - 执行 `npm run build`，成功完成 Vite 构建打包，产出包含 `dist/assets/CtfCompetitionArenaView-DMI3Fkjp.js (16.49 kB)` 在内的全量产物，耗时 5.67s。
