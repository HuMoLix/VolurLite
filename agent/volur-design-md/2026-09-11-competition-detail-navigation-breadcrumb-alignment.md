# 竞赛详情视图（CompetitionDetailView）导航栏面包屑层级对齐与交互优化审计说明

## 1. 变更背景与需求说明
根据用户的最新指令：
> 「然后修改 @[src/volur-lite-view/src/views/competition/CompetitionDetailView.vue] 的导航栏应该是 Volur Lite / 竞赛演练 / <比赛名> 然后点击竞赛演练可以返回竞赛列表。」

在既有实现中：
1. 全局主布局 [`AppLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue) 的顶部导航栏（`app-header`）中的面包屑仅配置了静态两级结构：`{{ systemStore.platformName }} / {{ currentRouteTitle }}`；
2. 当用户访问竞赛详情页（`/competitions/:slug` 或 `/c/:slug`）时，导航栏仅呈现 `Volur Lite / 竞赛详情` 或 `Volur Lite / 竞赛短链直达`；
3. 面包屑缺失了中间核心父级节点「竞赛演练」（`Competitions`），无法体现路由上下文层级；末级节点也未能动态反映真实的竞赛名称（`<比赛名>`）；
4. 面包屑节点为纯静态文本，缺少点击交互能力，无法通过点击「竞赛演练」快速返回公开竞赛列表大厅。

本次任务针对全局布局导航条、路由元数据及详情视图状态同步进行了系统性的联动对齐。

---

## 2. 核心架构改动点

### 2.1 路由元数据层级强化 ([`src/router/routes.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/router/routes.ts))
为 `CompetitionDetailBySlug` 与 `CompetitionShortLink` 补充父级导航元数据配置：
```ts
meta: {
  title: '竞赛详情',
  titleKey: 'competition.detail_title',
  parentTitle: '竞赛演练',
  parentTitleKey: 'competition.page_title',
  parentPath: '/competitions',
  requiresAuth: false
}
```
使路由定义明确继承了父级导航路径与多语言国际化键名。

### 2.2 状态同步与响应式通信 ([`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue))
1. **接入 Pinia 竞赛状态仓储**：
   - 引入并调用 `useCompetitionStore`；
   - 在 `loadCompetition()` 成功拉取到竞赛数据（`res.data`）时，自动同步设置 `competitionStore.currentCompetition = res.data`，使布局组件能够实时感知赛事名称；
   - 在组件卸载（`onUnmounted`）时调用 `competitionStore.clearCompetitionState()` 优雅释放状态，避免页面切换残余干扰。
2. **Slug 路由监听 (`watch`)**：
   - 添加对 `() => route.params.slug` 的响应式监听，确保在同构视图内不同赛事切换时能够无刷新重新获取数据并驱动导航栏刷新。

### 2.3 全局布局顶部导航栏交互与渲染升级 ([`AppLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue))
1. **三级动态面包屑渲染结构**：
   - **第一级（平台首页）**：`Volur Lite`（来自 `systemStore.platformName`），赋予手型光标与悬停动画，点击直接返回主页根路径 `/`；
   - **第二级（竞赛演练）**：通过 `isCompetitionDetail` 动态判定分支，展示 `competition.page_title`（“竞赛演练”），赋予手型光标与悬停交互，点击触发 `router.push('/competitions')` 极速返回竞赛大厅；
   - **第三级（当前比赛名）**：通过 `competitionTitle` 计算属性动态优先提取 `competitionStore.currentCompetition?.title`，并在尚未加载完成前安全降级至路由预设标题，添加完整标题 `:title` 悬浮提示；
2. **样式强化与超长文本防溢出**：
   - 新增 `.breadcrumb-link` 样式，提供明确的 `cursor: pointer` 与柔和的颜色过渡反馈；
   - 新增 `.breadcrumb-current` 样式，针对超长比赛名设置了 `max-width: 360px`、`ellipsis` 省略截断与防折行保护，确保顶栏布局在各种屏幕宽度下的工整度。

---

## 3. 构建与验证结论

1. **TypeScript 静态类型检查与构建打包**：
   - 运行 `npm run build`（`vue-tsc --noEmit && vite build`）：**100% 编译成功，0 错误、0 警告**（构建耗时 4.62s）；
2. **行为与交互验收**：
   - 访问 `/competitions` 时：面包屑展示为 `Volur Lite / 竞赛演练`；
   - 访问 `/competitions/:slug` 或 `/c/:slug` 时：面包屑动态渲染为 `Volur Lite / 竞赛演练 / <比赛名>`；
   - 点击中间项「竞赛演练」：即刻平滑返回 `/competitions` 列表大厅；
   - 点击首项「Volur Lite」：返回平台首页；
   - 页面卸载或切换时：状态即时清理，符合单一数据流与组件解耦原则。
