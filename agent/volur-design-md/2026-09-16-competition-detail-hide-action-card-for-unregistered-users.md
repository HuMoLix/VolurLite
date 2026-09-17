# 竞赛详情页未参赛用户主控制卡片条件隐藏施工记录

**施工日期**：2026-09-16  
**施工模块**：`src/volur-lite-view/src/views/competition/CompetitionDetailView.vue`  
**责任主体**：Antigravity AI Agent  

---

## 1. 变更背景与需求分析

在平台竞赛详情页（[`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue)）中，原先无论赛事处于何种阶段，页面顶部均会渲染「Section 1: 报名与入场专属主控制卡片」(`action-status-card`)：
- 若用户未登录，展示“登录账号后即可组建或加入战队参赛”以及【前往登录】按钮；
- 若用户已登录但未报名战队，展示“报名参赛通道”以及【立即报名】（或已截止/已开始 Tag 提示）；
- 若用户已报名，展示战队信息及根据审核通过/开赛状态展示入场卡片。

**存在的问题与体验缺陷**：
1. **死路引导与误导性**：一旦比赛开始，后端团队报名（`signupTeam`）与成员入队（`acceptInvitation`）接口均受 `@RequiredCompetitionStatus(allowed = {CompetitionStatus.REGISTRATION})` 严格拦截。此时对于未报名的用户（无论是游客还是已登录用户），报名通道已事实关闭，引导其登录或展示报名通道属于无效交互；
2. **非参赛用户视觉干扰**：对于在比赛开始后进入赛事详情页的非参赛用户，首屏本应专注于查看比赛的详细赛制、时间轴与防作弊规则，常驻的无用操作卡片严重挤占首屏视窗。

**明确业务需求**：
当比赛开始之后（`hasStarted` 为 true），如果用户还没有报名参加比赛（`!isUserRegistered` 为 true），则不展示“报名与入场专属主控制卡片”。页面仅展示“竞赛简介与赛程”和“队伍与防作弊规则”（以及赛事 Banner，若存在）。

---

## 2. 核心改动点与实施细节

### 2.1 状态判定与响应式推演

在 [`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue) 中建立精准的状态判定体系：

1. **响应式时间戳 `nowTimestamp`**：
   引入 `const nowTimestamp = ref<number>(Date.now())`，并在每秒执行的 `updateStartCountdown()` 中实时同步更新，使依赖当前时间的所有计算属性均具备完整的响应式能力，开赛瞬间界面无需刷新即可平滑流转。

2. **赛事是否已开始 `hasStarted`**：
   - 过滤草稿状态：`competition.value.status === 'DRAFT'` 为 false；
   - 包含进行中及后续状态：`status` 为 `RUNNING`、`FROZEN`、`ENDED`、`ARCHIVED` 均视为已开始；
   - 基于时间判断：非草稿状态下，若 `nowTimestamp.value >= new Date(competition.value.startTime).getTime()` 亦视为已开始。

3. **用户是否已报名参赛 `isUserRegistered`**：
   - 判定标准：`userStore.isLoggedIn && myTeam.value && myTeam.value.status !== 'DISBANDED'`。

4. **控制卡片可见性 `shouldShowActionCard`**：
   - 核心逻辑：
     ```typescript
     const shouldShowActionCard = computed(() => {
       // 当比赛开始之后，未报名参加比赛的用户不展示“报名与入场专属主控制卡片”
       if (hasStarted.value && !isUserRegistered.value) {
         return false
       }
       return true
     })
     ```

### 2.2 视图模板条件渲染

在 Section 1 卡片元素上增加条件判定：
```html
<!-- Section 1: 报名与入场专属主控制卡片 -->
<n-card v-if="shouldShowActionCard" class="action-status-card" :bordered="false">
```
当 `shouldShowActionCard` 为 false 时，该卡片完全从 DOM 中移除，页面主体直接展示 Section 2（即包含“竞赛简介与赛程”和“队伍与防作弊规则”的 `content-detail-card`）。

### 2.3 状态流转修复优化

同步修正并精简了 `isUpcoming`、`isRunning` 与 `isRegistrationOpen` 中对开赛状态的冗余或错误判定，确保赛事状态计算逻辑严格自洽。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量 TypeScript 静态类型检查与 Vite 生产打包：
```bash
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误通过；
- `vite build`：成功构建前端静态资源（构建耗时 4.78s），无任何语法或模块解析错误。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/views/competition/CompetitionDetailView.vue`
- **影响范围**：
  - 公开比赛详情页（`/competitions/:id` 或 `/c/:slug`）；
  - 仅对“比赛开始后未报名的用户”隐藏操作控制卡片；对于已报名的战队成员，仍正常展示战队信息及【进入竞赛】/【查看天梯】等入场通道；对于尚未开赛的赛事，依然完整展示报名入口与登录引导。
