# AppLayout 与 CompetitionLayout 独立视口滚动与滚动槽位稳定化重构

## 1. 变更背景与架构演进

在继后台管理系统 `AdminLayout` 升级为独立双栏滚动模式后，前台主布局 `AppLayout` 与赛事竞技场专用布局 `CompetitionLayout` 也存在类似体验瓶颈：
1. **`AppLayout`**：原先采用 `min-h-screen` 流式布局，当用户在不同页面间导航时，由于全局视口滚动条的动态产生或消失，导致主内容容器（`max-w-[100rem] mx-auto`）因可用视口宽度的突变发生横向抖动（Cumulative Layout Shift）；
2. **`CompetitionLayout`**：原先由 `html / body` 滚动驱动，使得顶部的赛事核心控制栏（倒计时、退出按钮、战队状态）随着页面下滑而移出屏幕，且在题目列表 Tab、排行榜 Tab 等不同长短视图切换时引发视口剧烈弹跳。

为保证全平台交互体验的平滑一致，将 `AppLayout` 与 `CompetitionLayout` 全面升级为**独立双栏/视口滚动模式（App-like Independent Scroll）**。

## 2. 核心设计与改造方案

### 2.1 AppLayout 前台主布局改造
1. **根视口锁定**：
   - 将 `.app-layout` 限制为视口严格尺寸：`h-screen max-h-screen overflow-hidden`；
   - 彻底切断内容向全局外层 `html / body` 溢出的渠道。
2. **主容器独立滚动与槽位保护**：
   - `.app-main` 增加 `h-full overflow-y-auto overflow-x-hidden`、`-webkit-overflow-scrolling: touch` 与 `overscroll-behavior-y: contain`；
   - 注入核心规范属性 `scrollbar-gutter: stable;`，在容器右侧建立常驻预留槽位；
   - 配置系统级 8px 圆角轻量滚动条与悬停高亮；
3. **路由联动与复位支持**：
   - 绑定 `appMainRef`，监听 `route.path` 变更时自动复位滚动位置至顶部；
   - 在 `scrollToTop` 方法中优先对 `appMainRef` 执行平滑滚动，并通过 `defineExpose` 对外暴露。

### 2.2 CompetitionLayout 赛事大厅布局改造
1. **顶栏常驻吸顶与视口隔离**：
   - 根容器 `.competition-layout` 升级为 `h-screen max-h-screen flex flex-col overflow-hidden`；
   - 顶部 `<VCompetitionHeader>` 稳定处于视口顶端，倒计时与状态永久可见，不随主内容滚动移出屏幕。
2. **赛事主工作区独立滚动与槽位稳定化**：
   - 引入专用独立滚动容器 `.competition-main`（包含 `<main>` 题目/排名区与 `<AppFooter>` 底部）；
   - 配置 `h-full overflow-y-auto` 并声明 `scrollbar-gutter: stable;`；
   - 选手在题目筛选、不同 Tab（题目、公告、排行榜、战队）切换或弹窗做题时，主区域物理宽度恒定，彻底杜绝内容挤压与横向抖动。
3. **路由联动与方法暴露**：
   - 绑定 `competitionMainRef`，监听路由切换自动复位；
   - 实现并导出 `scrollToTop` 与 `competitionMainRef`。

## 3. 影响范围与验证结果

- **影响文件**：
  - `src/volur-lite-view-NEXT/src/components/layouts/AppLayout.vue`
  - `src/volur-lite-view-NEXT/src/components/layouts/CompetitionLayout.vue`
- **构建与校验**：
  - `npm run build` 执行成功，通过全部 TypeScript 类型检查与 Vite 打包。
  - 赛事界面顶栏常驻吸顶，题目/排行榜滚动流畅；前台主布局跨路由切换时居中容器零像素抖动。
