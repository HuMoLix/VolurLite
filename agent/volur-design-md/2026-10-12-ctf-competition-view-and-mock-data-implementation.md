# CTF 攻防竞技场视图与全链路 Mock 数据实现

## 变更背景
为了完善 Volur Lite NEXT 前端工程中各类竞赛模式的用户竞技体验，并与既有理论知识赛（THEORY）形成规范对齐，需要在 `src/views/competition/` 目录下新增 `CtfCompetitionView`（CTF 解题夺旗赛竞技场），支持赛题分类浏览、题目详情查阅、离线附件下载、动态靶机容器生命周期调度（拉起/延期/销毁）、Flag 校验提交、官方公告流转、实时天梯排行榜以及战队信息看板，并提供高保真本地 Mock 数据集以支持即开即用的前端预览与交互调试。

## 改动范围与核心实现

### 1. 目标文件
- **视图层**：
  - 新建 `../../src/volur-lite-view-NEXT/src/views/competition/CtfCompetitionView.vue`
  - 修改 `src/volur-lite-view-NEXT/src/views/competition/CompetitionDetailView.vue`
- **路由层**：
  - 修改 `src/volur-lite-view-NEXT/src/router/routes/competition.ts`
- **类型定义与 API 封装**：
  - 修改 `src/volur-lite-view-NEXT/src/types/competition/index.ts`
  - 修改 `src/volur-lite-view-NEXT/src/api/competition.ts`

### 2. 核心改动点
1. **领域模型与 API 1:1 对齐**：
   - 在 `types/competition/index.ts` 中增补 `CtfCompetitionChallengeVO`、`CtfCompetitionChallengeDetailVO`、`CtfFlagSubmitRequestDTO`、`CtfChallengeInstanceVO` 与 `CtfScoreboardItemVO`；
   - 在 `api/competition.ts` 中封装 `listCtfChallengesApi`、`getCtfChallengeDetailApi`、`submitCtfFlagApi`、`startCtfInstanceApi`、`stopCtfInstanceApi`、`extendCtfInstanceApi` 与 `getCtfScoreboardApi`，路径与动词严格对齐后端 `CtfCompetitionController`。
2. **路由解耦与按需版本控制**：
   - 在 `router/routes/competition.ts` 中注册独立考场竞技路由 `/competitions/:id/ctf` 与短链路由 `/c/:id/ctf`，并保持 `requiredAuth: true` 显式路由守卫；
   - 在 `CompetitionDetailView.vue` 的 `enterArena` 方法中打通 CTF 赛事的路由跳转调度。
3. **CTF 竞技场交互设计 (Google Material Design & Workspace)**：
   - 基于 `CompetitionLayout` 统一头部骨架（返回导航、倒计时、战队指示胶囊、多语言与暗黑模式切换）；
   - **四级工作台子导航选项卡**：赛题挑战 (`challenges`)、官方公告 (`announcements`)、计分排行榜 (`scoreboard`) 与我的战队 (`team`)；
   - **赛题卡片与分类过滤**：支持全部及 `Web`、`Pwn`、`Reverse`、`Crypto`、`Misc` 五大经典分类切换，展示动态分值、解出统计与已解出状态徽章；
   - **动态容器靶机管理**：支持一键调度靶机容器拉起、独立端口分配、租期实时倒计时、在线延期（+15m）与手动销毁回收；
   - **Flag 提交与实时反馈**：支持输入校验与本地模拟验证，答对自动更新战队解题列表与排行榜积分。
4. **高保真 Mock 数据集集成**：
   - 构造 10 道涵盖全分类的标准赛题及其详情描述、附件链接；
   - 构造置顶及一血官方广播公告；
   - 构造 6 支战队天梯积分榜单与当前用户战队详细档案；
   - 具备后端离线/空数据时的优雅降级保障。

## 安全与质量验证
- 严格践行零注释铁律，代码文件保持极致纯净；
- 通过 `vue-tsc --noEmit && vite build` 生产构建全量类型检测与打包验证，0 错误 0 告警。
